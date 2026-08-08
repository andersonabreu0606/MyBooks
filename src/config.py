\
from dataclasses import dataclass
import streamlit as st

@dataclass(frozen=True)
class Settings:
    app_url: str
    db_host: str
    db_port: int
    db_name: str
    db_user: str
    db_password: str
    db_ssl_enabled: bool
    db_ssl_ca_pem: str
    smtp_host: str
    smtp_port: int
    smtp_username: str
    smtp_password: str
    smtp_from_email: str
    smtp_from_name: str
    smtp_security: str
    mfa_pepper: str
    token_pepper: str
    bootstrap_admin_email: str
    bootstrap_admin_name: str
    bootstrap_admin_password: str

def _get(name, default=None):
    try:
        return st.secrets[name]
    except KeyError:
        if default is not None:
            return default
        raise RuntimeError(f"Secret obrigatório ausente: {name}")

def get_settings() -> Settings:
    return Settings(
        app_url=str(_get("APP_URL")).rstrip("/"),
        db_host=str(_get("DB_HOST")),
        db_port=int(_get("DB_PORT", 3306)),
        db_name=str(_get("DB_NAME")),
        db_user=str(_get("DB_USER")),
        db_password=str(_get("DB_PASSWORD")),
        db_ssl_enabled=bool(_get("DB_SSL_ENABLED", True)),
        db_ssl_ca_pem=str(_get("DB_SSL_CA_PEM", "")),
        smtp_host=str(_get("SMTP_HOST")),
        smtp_port=int(_get("SMTP_PORT", 587)),
        smtp_username=str(_get("SMTP_USERNAME")),
        smtp_password=str(_get("SMTP_PASSWORD")),
        smtp_from_email=str(_get("SMTP_FROM_EMAIL")),
        smtp_from_name=str(_get("SMTP_FROM_NAME", "BookApp")),
        smtp_security=str(_get("SMTP_SECURITY", "STARTTLS")).upper(),
        mfa_pepper=str(_get("MFA_PEPPER")),
        token_pepper=str(_get("TOKEN_PEPPER")),
        bootstrap_admin_email=str(_get("BOOTSTRAP_ADMIN_EMAIL", "")).strip().lower(),
        bootstrap_admin_name=str(_get("BOOTSTRAP_ADMIN_NAME", "Administrador")).strip(),
        bootstrap_admin_password=str(_get("BOOTSTRAP_ADMIN_PASSWORD", "")),
    )
