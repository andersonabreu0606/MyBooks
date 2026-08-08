# MyBooks Secure v2 — Streamlit + SQLAlchemy + Neon PostgreSQL

Versão migrada de MySQL para **Neon PostgreSQL**.

## Stack

- Streamlit
- Python
- SQLAlchemy 2
- PostgreSQL no Neon
- Psycopg 3
- Argon2id para passwords
- MFA por código enviado por e-mail
- RBAC: ADMIN, LIBRARIAN e READER
- SMTP STARTTLS/SSL

## Princípio de configuração

A aplicação lê configurações **exclusivamente de `st.secrets`**.

Não existe fallback para `.env` nem `os.environ`.

O ficheiro real:

```text
.streamlit/secrets.toml
```

não deve ser versionado. O repositório contém apenas:

```text
.streamlit/secrets.example.toml
```

## 1. Criar o projeto no Neon

No Neon:

1. Crie um projeto PostgreSQL.
2. Abra **Connect**.
3. Ative **Connection pooling**.
4. Copie a connection string.
5. Confirme que a URL inclui `sslmode=require`.
6. Guarde a string completa no Secret `DATABASE_URL`.

Exemplo estrutural:

```toml
DATABASE_URL = "postgresql://USER:PASSWORD@ep-xxxx-pooler.REGION.aws.neon.tech/neondb?sslmode=require&channel_binding=require"
```

Não coloque esta string no GitHub.

## 2. Secrets do Streamlit

Use o conteúdo de:

```text
.streamlit/secrets.example.toml
```

No Streamlit Community Cloud:

```text
App
  > Settings
    > Secrets
```

Cole todas as variáveis.

Variáveis utilizadas:

```text
APP_NAME
APP_URL
APP_DEBUG
AUTO_CREATE_SCHEMA
DATABASE_URL

SMTP_HOST
SMTP_PORT
SMTP_USERNAME
SMTP_PASSWORD
SMTP_FROM_EMAIL
SMTP_FROM_NAME
SMTP_SECURITY

MFA_PEPPER
TOKEN_PEPPER

BOOTSTRAP_ADMIN_EMAIL
BOOTSTRAP_ADMIN_NAME
BOOTSTRAP_ADMIN_PASSWORD
```

## 3. Criar os peppers

Execute duas vezes:

```bash
python -c "import secrets; print(secrets.token_hex(32))"
```

Use valores diferentes em:

```toml
MFA_PEPPER = "..."
TOKEN_PEPPER = "..."
```

## 4. Primeiro deploy

Inicialmente:

```toml
AUTO_CREATE_SCHEMA = true
```

A aplicação cria as tabelas SQLAlchemy se ainda não existirem.

Se a tabela `users` estiver vazia, cria o primeiro ADMIN através de:

```text
BOOTSTRAP_ADMIN_EMAIL
BOOTSTRAP_ADMIN_NAME
BOOTSTRAP_ADMIN_PASSWORD
```

Depois de confirmar o primeiro acesso:

1. remova `BOOTSTRAP_ADMIN_PASSWORD` dos Secrets;
2. altere `AUTO_CREATE_SCHEMA = false`;
3. numa evolução posterior, use Alembic para migrations formais.

## 5. Execução local

Crie:

```text
.streamlit/secrets.toml
```

a partir do exemplo e depois:

```bash
python -m venv .venv
pip install -r requirements.txt
streamlit run app.py
```

## 6. Segurança

- Connection string do Neon apenas nos Streamlit Secrets.
- TLS exigido na connection string (`sslmode=require`).
- Passwords: Argon2id.
- MFA OTP: 8 dígitos.
- OTP expira em 5 minutos.
- OTP de uso único.
- Limite de tentativas.
- OTP armazenado apenas como HMAC digest.
- RBAC no backend.
- Erros técnicos ocultados quando `APP_DEBUG = false`.
- `.streamlit/secrets.toml` ignorado pelo Git.

## 7. Recomendação Neon

Para uma aplicação hospedada no Streamlit Cloud, prefira a connection string
**pooled**, identificável por `-pooler` no hostname.

O engine SQLAlchemy usa `pool_pre_ping=True`, o que ajuda quando o compute do
Neon ficou inativo e uma conexão antiga deixou de ser válida.
