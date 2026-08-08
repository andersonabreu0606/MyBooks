from contextlib import contextmanager

import streamlit as st
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker

from .config import get_settings


def _sqlalchemy_database_url(raw_url: str) -> str:
    # O Neon normalmente fornece "postgresql://...".
    # Forçamos o driver Psycopg 3 no SQLAlchemy.
    if raw_url.startswith("postgresql+psycopg://"):
        return raw_url
    if raw_url.startswith("postgresql://"):
        return raw_url.replace(
            "postgresql://",
            "postgresql+psycopg://",
            1,
        )
    if raw_url.startswith("postgres://"):
        return raw_url.replace(
            "postgres://",
            "postgresql+psycopg://",
            1,
        )
    raise RuntimeError(
        "DATABASE_URL inválida. Utilize a connection string PostgreSQL fornecida pelo Neon."
    )


@st.cache_resource
def get_engine():
    cfg = get_settings()
    url = _sqlalchemy_database_url(cfg.database_url)

    # Neon pode colocar o compute em idle/scale-to-zero.
    # pool_pre_ping valida a ligação antes de a reutilizar.
    return create_engine(
        url,
        pool_pre_ping=True,
        pool_recycle=300,
        pool_size=5,
        max_overflow=5,
        pool_timeout=30,
        connect_args={
            "connect_timeout": 15,
            "application_name": cfg.app_name,
        },
    )


@st.cache_resource
def get_session_factory():
    return sessionmaker(
        bind=get_engine(),
        autoflush=False,
        expire_on_commit=False,
    )


def test_database_connection() -> None:
    with get_engine().connect() as conn:
        conn.execute(text("SELECT 1"))


@contextmanager
def db_session():
    session = get_session_factory()()
    try:
        yield session
        session.commit()
    except Exception:
        session.rollback()
        raise
    finally:
        session.close()
