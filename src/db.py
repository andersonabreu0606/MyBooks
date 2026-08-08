\
import ssl
from contextlib import contextmanager
import streamlit as st
from sqlalchemy import create_engine
from sqlalchemy.engine import URL
from sqlalchemy.orm import sessionmaker
from .config import get_settings

@st.cache_resource
def get_engine():
    cfg = get_settings()
    url = URL.create(
        "mysql+pymysql",
        username=cfg.db_user,
        password=cfg.db_password,
        host=cfg.db_host,
        port=cfg.db_port,
        database=cfg.db_name,
        query={"charset": "utf8mb4"},
    )

    connect_args = {}
    if cfg.db_ssl_enabled:
        ctx = ssl.create_default_context(
            cadata=cfg.db_ssl_ca_pem or None
        )
        ctx.check_hostname = True
        ctx.verify_mode = ssl.CERT_REQUIRED
        connect_args["ssl"] = ctx

    return create_engine(
        url,
        pool_pre_ping=True,
        pool_recycle=1800,
        pool_size=5,
        max_overflow=5,
        connect_args=connect_args,
    )

@st.cache_resource
def get_session_factory():
    return sessionmaker(
        bind=get_engine(),
        autoflush=False,
        expire_on_commit=False,
    )

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
