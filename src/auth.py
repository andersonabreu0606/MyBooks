\
from datetime import datetime, timedelta, timezone
from sqlalchemy import select, func
from sqlalchemy.orm import selectinload
from email_validator import validate_email, EmailNotValidError

from .config import get_settings
from .db import db_session, get_engine
from .models import Base, User, Role, MfaChallenge, AccountToken, AuthEvent, utcnow
from .security import (
    hash_password, verify_password, password_needs_rehash,
    generate_otp, generate_token, otp_digest, token_digest, secure_equals
)
from .mailer import send_mfa_code, send_invitation

ROLE_DEFS = {
    "ADMIN": "Administração total",
    "LIBRARIAN": "Gestão do catálogo",
    "READER": "Consulta e biblioteca pessoal",
}

def normalize_email(email: str) -> str:
    try:
        result = validate_email(email.strip(), check_deliverability=False)
        return result.normalized.lower()
    except EmailNotValidError:
        raise ValueError("Endereço de e-mail inválido.")

def ensure_schema_and_bootstrap():
    engine = get_engine()
    Base.metadata.create_all(engine)

    cfg = get_settings()
    with db_session() as db:
        for name, description in ROLE_DEFS.items():
            role = db.scalar(select(Role).where(Role.name == name))
            if not role:
                db.add(Role(name=name, description=description))
        db.flush()

        total = db.scalar(select(func.count(User.id))) or 0
        if total == 0:
            if not (cfg.bootstrap_admin_email and cfg.bootstrap_admin_password):
                raise RuntimeError(
                    "Base sem utilizadores. Configure BOOTSTRAP_ADMIN_EMAIL e "
                    "BOOTSTRAP_ADMIN_PASSWORD nos secrets."
                )
            email = normalize_email(cfg.bootstrap_admin_email)
            admin_role = db.scalar(select(Role).where(Role.name == "ADMIN"))
            user = User(
                email=email,
                full_name=cfg.bootstrap_admin_name,
                password_hash=hash_password(cfg.bootstrap_admin_password),
                is_active=True,
                activated_at=utcnow(),
                roles=[admin_role],
            )
            db.add(user)
            db.flush()
            db.add(AuthEvent(
                user_id=user.id,
                email_attempted=email,
                event_type="BOOTSTRAP_ADMIN_CREATED",
                success=True,
            ))

def _load_user_by_email(db, email: str):
    return db.scalar(
        select(User)
        .options(selectinload(User.roles))
        .where(User.email == email)
    )

def start_login(email: str, password: str):
    """
    Retorna:
      {"ok": True, "challenge_token": ..., "masked_email": ...}
      {"ok": False, "message": ...}
    """
    generic_error = "Credenciais inválidas ou conta indisponível."
    try:
        normalized = normalize_email(email)
    except ValueError:
        normalized = email.strip().lower()

    now = utcnow()
    with db_session() as db:
        user = _load_user_by_email(db, normalized)

        if not user or not user.is_active or not user.password_hash:
            db.add(AuthEvent(
                user_id=user.id if user else None,
                email_attempted=normalized[:254] if normalized else None,
                event_type="PASSWORD_LOGIN",
                success=False,
                details="invalid_or_inactive",
            ))
            return {"ok": False, "message": generic_error}

        if user.locked_until and user.locked_until > now:
            db.add(AuthEvent(
                user_id=user.id, email_attempted=normalized,
                event_type="PASSWORD_LOGIN", success=False,
                details="account_locked",
            ))
            return {"ok": False, "message": generic_error}

        if not verify_password(user.password_hash, password):
            user.failed_login_count += 1
            if user.failed_login_count >= 5:
                user.locked_until = now + timedelta(minutes=15)
                user.failed_login_count = 0
            db.add(AuthEvent(
                user_id=user.id, email_attempted=normalized,
                event_type="PASSWORD_LOGIN", success=False,
                details="bad_password",
            ))
            return {"ok": False, "message": generic_error}

        # Rehash transparente quando os parâmetros Argon2 mudarem.
        if password_needs_rehash(user.password_hash):
            user.password_hash = hash_password(password)

        user.failed_login_count = 0
        user.locked_until = None

        # Invalida desafios anteriores ainda não consumidos.
        active = db.scalars(
            select(MfaChallenge).where(
                MfaChallenge.user_id == user.id,
                MfaChallenge.consumed_at.is_(None),
            )
        ).all()
        for challenge in active:
            challenge.consumed_at = now

        code = generate_otp()
        challenge_token = generate_token()
        challenge = MfaChallenge(
            challenge_token=challenge_token,
            user_id=user.id,
            code_digest=otp_digest(challenge_token, code),
            expires_at=now + timedelta(minutes=5),
            max_attempts=5,
        )
        db.add(challenge)
        db.flush()

        # O OTP nunca é gravado em plaintext nem em logs.
        try:
            send_mfa_code(user.email, code)
        except Exception as exc:
            challenge.consumed_at = now
            db.add(AuthEvent(
                user_id=user.id, email_attempted=user.email,
                event_type="MFA_SEND", success=False,
                details=type(exc).__name__,
            ))
            return {
                "ok": False,
                "message": "Não foi possível enviar o código MFA. Tente novamente mais tarde.",
            }

        db.add(AuthEvent(
            user_id=user.id, email_attempted=user.email,
            event_type="MFA_SEND", success=True,
        ))

        local, domain = user.email.split("@", 1)
        masked = (local[:2] + "***@" + domain) if len(local) > 2 else ("***@" + domain)
        return {
            "ok": True,
            "challenge_token": challenge_token,
            "masked_email": masked,
        }

def verify_mfa(challenge_token: str, code: str):
    now = utcnow()
    code = "".join(ch for ch in code if ch.isdigit())

    with db_session() as db:
        challenge = db.scalar(
            select(MfaChallenge)
            .options(selectinload(MfaChallenge.user).selectinload(User.roles))
            .where(MfaChallenge.challenge_token == challenge_token)
        )

        if not challenge or challenge.consumed_at is not None:
            return {"ok": False, "message": "Código inválido ou expirado."}

        if challenge.expires_at <= now:
            challenge.consumed_at = now
            return {"ok": False, "message": "Código inválido ou expirado."}

        if challenge.attempts >= challenge.max_attempts:
            challenge.consumed_at = now
            return {"ok": False, "message": "Código inválido ou expirado."}

        challenge.attempts += 1
        expected = otp_digest(challenge.challenge_token, code)
        if not secure_equals(challenge.code_digest, expected):
            if challenge.attempts >= challenge.max_attempts:
                challenge.consumed_at = now
            db.add(AuthEvent(
                user_id=challenge.user_id,
                email_attempted=challenge.user.email,
                event_type="MFA_VERIFY",
                success=False,
                details="bad_code",
            ))
            return {"ok": False, "message": "Código inválido ou expirado."}

        challenge.consumed_at = now
        user = challenge.user
        user.last_login_at = now
        db.add(AuthEvent(
            user_id=user.id,
            email_attempted=user.email,
            event_type="LOGIN_SUCCESS",
            success=True,
        ))
        return {
            "ok": True,
            "user": {
                "id": user.id,
                "public_id": user.public_id,
                "email": user.email,
                "full_name": user.full_name,
                "roles": sorted(r.name for r in user.roles),
            },
        }

def create_invited_user(actor: dict, email: str, full_name: str, role_name: str):
    if "ADMIN" not in actor.get("roles", []):
        raise PermissionError("Operação permitida apenas para ADMIN.")

    email = normalize_email(email)
    full_name = full_name.strip()
    if not full_name:
        raise ValueError("Nome é obrigatório.")
    if role_name not in ROLE_DEFS:
        raise ValueError("Perfil inválido.")

    raw_token = generate_token()
    cfg = get_settings()
    expires = utcnow() + timedelta(hours=24)

    with db_session() as db:
        existing = _load_user_by_email(db, email)
        if existing:
            raise ValueError("Já existe um utilizador com este e-mail.")

        role = db.scalar(select(Role).where(Role.name == role_name))
        user = User(
            email=email,
            full_name=full_name,
            password_hash=None,
            is_active=True,
            roles=[role],
        )
        db.add(user)
        db.flush()

        token = AccountToken(
            user_id=user.id,
            token_type="ACTIVATE",
            token_digest=token_digest(raw_token),
            expires_at=expires,
        )
        db.add(token)
        db.flush()

        activation_url = f"{cfg.app_url}/?activate={raw_token}"
        try:
            send_invitation(email, full_name, activation_url)
        except Exception:
            # A transação será revertida pelo context manager.
            raise RuntimeError("Falha ao enviar o convite por e-mail.")

        db.add(AuthEvent(
            user_id=user.id,
            email_attempted=email,
            event_type="USER_INVITED",
            success=True,
            details=f"role={role_name}",
        ))

def activate_account(raw_token: str, new_password: str):
    digest = token_digest(raw_token)
    now = utcnow()

    with db_session() as db:
        token = db.scalar(
            select(AccountToken)
            .options(selectinload(AccountToken.user))
            .where(
                AccountToken.token_digest == digest,
                AccountToken.token_type == "ACTIVATE",
            )
        )
        if not token or token.consumed_at is not None or token.expires_at <= now:
            raise ValueError("Link de ativação inválido ou expirado.")

        user = token.user
        user.password_hash = hash_password(new_password)
        user.activated_at = now
        token.consumed_at = now
        db.add(AuthEvent(
            user_id=user.id,
            email_attempted=user.email,
            event_type="ACCOUNT_ACTIVATED",
            success=True,
        ))

def change_password(actor: dict, current_password: str, new_password: str):
    with db_session() as db:
        user = db.get(User, actor["id"])
        if not user or not user.password_hash or not verify_password(user.password_hash, current_password):
            raise ValueError("Palavra-passe atual inválida.")
        user.password_hash = hash_password(new_password)
        db.add(AuthEvent(
            user_id=user.id, email_attempted=user.email,
            event_type="PASSWORD_CHANGED", success=True
        ))

def list_users(actor: dict):
    if "ADMIN" not in actor.get("roles", []):
        raise PermissionError("Operação permitida apenas para ADMIN.")
    with db_session() as db:
        users = db.scalars(
            select(User).options(selectinload(User.roles)).order_by(User.full_name)
        ).all()
        return [
            {
                "id": u.id,
                "nome": u.full_name,
                "email": u.email,
                "ativo": u.is_active,
                "ativado": bool(u.activated_at),
                "perfis": ", ".join(sorted(r.name for r in u.roles)),
            }
            for u in users
        ]
