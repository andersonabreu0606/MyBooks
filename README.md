# BookApp — Streamlit + Python + SQLAlchemy + MySQL

Starter secure-first para uma aplicação de livros.

## Incluído

- Login por e-mail + palavra-passe.
- Password hashing com Argon2id.
- MFA por e-mail com OTP de 8 dígitos.
- OTP com TTL de 5 minutos, uso único e limite de 5 tentativas.
- OTP nunca armazenado em plaintext; digest HMAC com pepper fora da base.
- Bloqueio temporário após falhas sucessivas de password.
- RBAC: `ADMIN`, `LIBRARIAN`, `READER`.
- Convite de utilizadores por link de ativação de uso único.
- Auditoria básica de autenticação.
- MySQL via SQLAlchemy/PyMySQL.
- TLS para MySQL por padrão.
- SMTP STARTTLS/SSL.
- CRUD inicial de livros.
- Interface responsiva/mobile-first.

## 1. Criar a base

No MySQL:

```sql
CREATE DATABASE bookapp
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE USER 'bookapp_app'@'%' IDENTIFIED BY 'UMA_SENHA_FORTE';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
ON bookapp.* TO 'bookapp_app'@'%';
FLUSH PRIVILEGES;
```

> Em produção, depois da fase inicial de schema/migrations, retire `CREATE` e `ALTER`
> da conta da aplicação e use uma conta separada para migrations.

## 2. Configurar secrets

Localmente:

```bash
mkdir -p .streamlit
cp .streamlit/secrets.example.toml .streamlit/secrets.toml
```

No Streamlit Community Cloud, copie os mesmos pares chave/valor para **Secrets**.

**Não faça commit de `.streamlit/secrets.toml`.**

## 3. Gerar peppers

Gere dois valores diferentes:

```bash
python -c "import secrets; print(secrets.token_hex(32))"
python -c "import secrets; print(secrets.token_hex(32))"
```

Use-os em `MFA_PEPPER` e `TOKEN_PEPPER`.

## 4. Instalar e executar

```bash
python -m venv .venv
# Windows:
.venv\Scripts\activate
# Linux/macOS:
# source .venv/bin/activate

pip install -r requirements.txt
streamlit run app.py
```

## 5. Primeiro administrador

Se a tabela `users` estiver vazia, o sistema cria o primeiro administrador usando:

- `BOOTSTRAP_ADMIN_EMAIL`
- `BOOTSTRAP_ADMIN_NAME`
- `BOOTSTRAP_ADMIN_PASSWORD`

Depois do primeiro deploy e do primeiro acesso, **remova `BOOTSTRAP_ADMIN_PASSWORD` dos secrets**.

## 6. TLS do MySQL

`DB_SSL_ENABLED = true` por padrão.

Se o MySQL usa certificado emitido por uma CA pública, normalmente não precisa fornecer
`DB_SSL_CA_PEM`.

Se usa CA própria/self-signed, copie a CA PEM completa para `DB_SSL_CA_PEM`.

Não exponha a porta 3306 para `0.0.0.0/0`. Restrinja por firewall sempre que possível.

## 7. SMTP

Use:

- `SMTP_SECURITY = "STARTTLS"` com porta 587; ou
- `SMTP_SECURITY = "SSL"` com porta 465.

A conta SMTP deve ser exclusiva da aplicação e as credenciais ficam apenas nos Secrets.

## Próximas evoluções recomendadas

1. Alembic para migrations formais.
2. Recuperação de palavra-passe.
3. Gestão de ativação/desativação e alteração de roles.
4. TOTP/passkeys como MFA mais forte para administradores.
5. Catálogo normalizado: autores, editoras, categorias, edições e ISBN.
6. Biblioteca pessoal, favoritos, estado de leitura e avaliações.
7. Rate limiting persistente por IP/conta.
8. Dashboard e auditoria administrativa.
9. SAST/SCA/secret scanning no GitHub Actions.
10. Pentest antes de produção.
