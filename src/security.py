\
import hashlib
import hmac
import secrets
import re
from argon2 import PasswordHasher
from argon2.exceptions import VerifyMismatchError, VerificationError, InvalidHashError
from .config import get_settings

_password_hasher = PasswordHasher()

def hash_password(password: str) -> str:
    validate_password(password)
    return _password_hasher.hash(password)

def verify_password(password_hash: str, password: str) -> bool:
    try:
        return bool(_password_hasher.verify(password_hash, password))
    except (VerifyMismatchError, VerificationError, InvalidHashError):
        return False

def password_needs_rehash(password_hash: str) -> bool:
    return _password_hasher.check_needs_rehash(password_hash)

def validate_password(password: str) -> None:
    if len(password) < 12:
        raise ValueError("A palavra-passe deve ter pelo menos 12 caracteres.")
    if len(password) > 128:
        raise ValueError("A palavra-passe deve ter no máximo 128 caracteres.")
    if not password.strip():
        raise ValueError("A palavra-passe não pode ser vazia.")
    # Evita políticas artificiais de composição; permite passphrases longas.

def generate_otp() -> str:
    # 8 dígitos, CSPRNG.
    return f"{secrets.randbelow(100_000_000):08d}"

def generate_token() -> str:
    return secrets.token_urlsafe(32)

def _hmac_hex(secret_value: str, payload: str) -> str:
    return hmac.new(
        secret_value.encode("utf-8"),
        payload.encode("utf-8"),
        hashlib.sha256,
    ).hexdigest()

def otp_digest(challenge_token: str, code: str) -> str:
    return _hmac_hex(get_settings().mfa_pepper, f"{challenge_token}:{code}")

def token_digest(raw_token: str) -> str:
    return _hmac_hex(get_settings().token_pepper, raw_token)

def secure_equals(a: str, b: str) -> bool:
    return hmac.compare_digest(a, b)
