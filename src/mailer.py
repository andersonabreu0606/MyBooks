\
import smtplib
import ssl
from email.message import EmailMessage
from .config import get_settings

def _send(to_email: str, subject: str, text_body: str) -> None:
    cfg = get_settings()
    msg = EmailMessage()
    msg["From"] = f"{cfg.smtp_from_name} <{cfg.smtp_from_email}>"
    msg["To"] = to_email
    msg["Subject"] = subject
    msg.set_content(text_body)

    context = ssl.create_default_context()
    if cfg.smtp_security == "SSL":
        with smtplib.SMTP_SSL(cfg.smtp_host, cfg.smtp_port, context=context, timeout=20) as smtp:
            smtp.login(cfg.smtp_username, cfg.smtp_password)
            smtp.send_message(msg)
    elif cfg.smtp_security == "STARTTLS":
        with smtplib.SMTP(cfg.smtp_host, cfg.smtp_port, timeout=20) as smtp:
            smtp.ehlo()
            smtp.starttls(context=context)
            smtp.ehlo()
            smtp.login(cfg.smtp_username, cfg.smtp_password)
            smtp.send_message(msg)
    else:
        raise RuntimeError("SMTP_SECURITY deve ser STARTTLS ou SSL.")

def send_mfa_code(to_email: str, code: str) -> None:
    _send(
        to_email,
        "Código de verificação - BookApp",
        (
            "Foi solicitado um acesso à sua conta BookApp.\n\n"
            f"Código de verificação: {code}\n\n"
            "O código expira em 5 minutos e só pode ser utilizado uma vez.\n"
            "Se não foi você, ignore esta mensagem e altere a sua palavra-passe."
        ),
    )

def send_invitation(to_email: str, full_name: str, activation_url: str) -> None:
    _send(
        to_email,
        "Convite para o BookApp",
        (
            f"Olá, {full_name}.\n\n"
            "Foi criada uma conta para si no BookApp.\n"
            "Defina a sua palavra-passe através do link abaixo:\n\n"
            f"{activation_url}\n\n"
            "O link expira em 24 horas e só pode ser utilizado uma vez."
        ),
    )
