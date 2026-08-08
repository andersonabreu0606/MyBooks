# Diagnóstico SMTP

Para o cenário com porta 465:

```toml
SMTP_HOST = "mail.digitalclouds.com.br"
SMTP_PORT = 465
SMTP_SECURITY = "SSL"
SMTP_TIMEOUT = 20
SMTP_DIAGNOSTICS = true
```

`SMTP_DIAGNOSTICS = true` mostra apenas um código seguro, depois de uma
tentativa de login válida. Não mostra password, token MFA ou stack trace.

Códigos possíveis:

- `SMTP_DNS_ERROR`: hostname não resolve.
- `SMTP_CONNECTION_REFUSED`: porta/serviço recusou a ligação.
- `SMTP_TIMEOUT`: firewall ou servidor não respondeu.
- `SMTP_CERTIFICATE_ERROR`: certificado TLS inválido/nome não corresponde.
- `SMTP_TLS_ERROR`: handshake TLS falhou.
- `SMTP_AUTH_ERROR`: utilizador/password recusados.
- `SMTP_NOT_SUPPORTED`: método de segurança/autenticação incompatível.
- `SMTP_SENDER_REFUSED`: remetente recusado.
- `SMTP_RECIPIENT_REFUSED`: destinatário recusado.
- `SMTP_MESSAGE_REJECTED`: mensagem recusada após autenticação.
- `SMTP_DISCONNECTED`: servidor fechou a ligação.
- `SMTP_PROTOCOL_ERROR`: erro genérico SMTP.
- `SMTP_UNKNOWN_ERROR`: falha não categorizada.

Após resolver o problema:

```toml
SMTP_DIAGNOSTICS = false
```
