from dataclasses import dataclass
import streamlit as st


@dataclass(frozen=True)
class Settings:
    app_name: str
    app_url: str
    app_debug: bool
    auto_create_schema: bool
    database_url: str

    smtp_host: str
    smtp_port: int
    smtp_username: str
    smtp_password: str
    smtp_from_email: str
    smtp_from_name: str
    smtp_security: str
    smtp_timeout: int
    smtp_diagnostics: bool

    mfa_pepper: str
    token_pepper: str

    bootstrap_admin_email: str
    bootstrap_admin_name: str
    bootstrap_admin_password: str


_MISSING = object()


def _get(name, default=_MISSING):
    # Intencionalmente lê APENAS do Streamlit Secrets.
    # Não existe fallback para os.getenv().
    try:
        return st.secrets[name]
    except KeyError:
        if default is not _MISSING:
            return default
        raise RuntimeError(f"Secret obrigatório ausente: {name}")


def get_settings() -> Settings:
    return Settings(
        app_name=str(_get("APP_NAME", "MyBooks")).strip(),
        app_url=str(_get("APP_URL")).rstrip("/"),
        app_debug=bool(_get("APP_DEBUG", False)),
        auto_create_schema=bool(_get("AUTO_CREATE_SCHEMA", True)),
        database_url=str(_get("DATABASE_URL")).strip(),

        smtp_host=str(_get("SMTP_HOST")).strip(),
        smtp_port=int(_get("SMTP_PORT", 587)),
        smtp_username=str(_get("SMTP_USERNAME")).strip(),
        smtp_password=str(_get("SMTP_PASSWORD")),
        smtp_from_email=str(_get("SMTP_FROM_EMAIL")).strip(),
        smtp_from_name=str(_get("SMTP_FROM_NAME", "MyBooks")).strip(),
        smtp_security=str(_get("SMTP_SECURITY", "STARTTLS")).upper().strip(),
        smtp_timeout=int(_get("SMTP_TIMEOUT", 20)),
        smtp_diagnostics=bool(_get("SMTP_DIAGNOSTICS", False)),

        mfa_pepper=str(_get("MFA_PEPPER")),
        token_pepper=str(_get("TOKEN_PEPPER")),

        bootstrap_admin_email=str(
            _get("BOOTSTRAP_ADMIN_EMAIL", "")
        ).strip().lower(),
        bootstrap_admin_name=str(
            _get("BOOTSTRAP_ADMIN_NAME", "Administrador")
        ).strip(),
        bootstrap_admin_password=str(
            _get("BOOTSTRAP_ADMIN_PASSWORD", "")
        ),
    )
