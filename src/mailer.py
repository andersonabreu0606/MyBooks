import logging
import smtplib
import socket
import ssl
from dataclasses import dataclass
from email.message import EmailMessage

from .config import get_settings

logger = logging.getLogger(__name__)


@dataclass
class MailDeliveryError(Exception):
    code: str
    safe_message: str

    def __str__(self) -> str:
        return self.safe_message


def _translate_smtp_exception(exc: Exception) -> MailDeliveryError:
    if isinstance(exc, socket.gaierror):
        return MailDeliveryError(
            "SMTP_DNS_ERROR",
            "Não foi possível resolver o hostname do servidor SMTP.",
        )
    if isinstance(exc, (ConnectionRefusedError, smtplib.SMTPConnectError)):
        return MailDeliveryError(
            "SMTP_CONNECTION_REFUSED",
            "O servidor SMTP recusou a ligação.",
        )
    if isinstance(exc, (TimeoutError, socket.timeout)):
        return MailDeliveryError(
            "SMTP_TIMEOUT",
            "O servidor SMTP não respondeu dentro do tempo limite.",
        )
    if isinstance(exc, ssl.SSLCertVerificationError):
        return MailDeliveryError(
            "SMTP_CERTIFICATE_ERROR",
            "O certificado TLS do servidor SMTP não pôde ser validado.",
        )
    if isinstance(exc, ssl.SSLError):
        return MailDeliveryError(
            "SMTP_TLS_ERROR",
            "Falha ao estabelecer a ligação TLS com o servidor SMTP.",
        )
    if isinstance(exc, smtplib.SMTPAuthenticationError):
        return MailDeliveryError(
            "SMTP_AUTH_ERROR",
            "O servidor SMTP recusou o utilizador ou a palavra-passe.",
        )
    if isinstance(exc, smtplib.SMTPNotSupportedError):
        return MailDeliveryError(
            "SMTP_NOT_SUPPORTED",
            "O servidor SMTP não suporta o método de segurança/autenticação configurado.",
        )
    if isinstance(exc, smtplib.SMTPSenderRefused):
        return MailDeliveryError(
            "SMTP_SENDER_REFUSED",
            "O servidor SMTP recusou o endereço do remetente.",
        )
    if isinstance(exc, smtplib.SMTPRecipientsRefused):
        return MailDeliveryError(
            "SMTP_RECIPIENT_REFUSED",
            "O servidor SMTP recusou o destinatário.",
        )
    if isinstance(exc, smtplib.SMTPDataError):
        return MailDeliveryError(
            "SMTP_MESSAGE_REJECTED",
            "O servidor SMTP rejeitou a mensagem.",
        )
    if isinstance(exc, smtplib.SMTPServerDisconnected):
        return MailDeliveryError(
            "SMTP_DISCONNECTED",
            "O servidor SMTP encerrou a ligação inesperadamente.",
        )
    if isinstance(exc, smtplib.SMTPException):
        return MailDeliveryError(
            "SMTP_PROTOCOL_ERROR",
            "Ocorreu um erro no protocolo SMTP.",
        )
    return MailDeliveryError(
        "SMTP_UNKNOWN_ERROR",
        "Ocorreu uma falha inesperada no envio do e-mail.",
    )


def _send(to_email: str, subject: str, text_body: str) -> None:
    cfg = get_settings()

    msg = EmailMessage()
    msg["From"] = f"{cfg.smtp_from_name} <{cfg.smtp_from_email}>"
    msg["To"] = to_email
    msg["Subject"] = subject
    msg.set_content(text_body)

    context = ssl.create_default_context()

    try:
        if cfg.smtp_security == "SSL":
            # SSL/TLS desde o início da ligação. Normalmente porta 465.
            with smtplib.SMTP_SSL(
                cfg.smtp_host,
                cfg.smtp_port,
                context=context,
                timeout=cfg.smtp_timeout,
            ) as smtp:
                smtp.ehlo()
                smtp.login(cfg.smtp_username, cfg.smtp_password)
                smtp.send_message(msg)

        elif cfg.smtp_security == "STARTTLS":
            # Ligação SMTP seguida de upgrade STARTTLS. Normalmente porta 587.
            with smtplib.SMTP(
                cfg.smtp_host,
                cfg.smtp_port,
                timeout=cfg.smtp_timeout,
            ) as smtp:
                smtp.ehlo()
                if not smtp.has_extn("starttls"):
                    raise smtplib.SMTPNotSupportedError(
                        "STARTTLS não anunciado pelo servidor"
                    )
                smtp.starttls(context=context)
                smtp.ehlo()
                smtp.login(cfg.smtp_username, cfg.smtp_password)
                smtp.send_message(msg)

        else:
            raise MailDeliveryError(
                "SMTP_CONFIG_ERROR",
                "SMTP_SECURITY deve ser SSL ou STARTTLS.",
            )

    except MailDeliveryError:
        raise
    except Exception as exc:
        # Registra apenas tipo/código técnico; nunca password ou corpo da mensagem.
        translated = _translate_smtp_exception(exc)
        logger.warning(
            "Falha SMTP code=%s exception=%s host=%s port=%s security=%s",
            translated.code,
            type(exc).__name__,
            cfg.smtp_host,
            cfg.smtp_port,
            cfg.smtp_security,
        )
        raise translated from exc


def test_smtp_connection() -> dict:
    """
    Diagnóstico sem enviar e-mail.
    Testa DNS/TCP/TLS/EHLO/autenticação.
    Não retorna credenciais nem detalhes sensíveis.
    """
    cfg = get_settings()
    context = ssl.create_default_context()

    try:
        if cfg.smtp_security == "SSL":
            with smtplib.SMTP_SSL(
                cfg.smtp_host,
                cfg.smtp_port,
                context=context,
                timeout=cfg.smtp_timeout,
            ) as smtp:
                code, _ = smtp.ehlo()
                if not (200 <= code < 400):
                    raise smtplib.SMTPHeloError(code, b"EHLO failed")
                smtp.login(cfg.smtp_username, cfg.smtp_password)

        elif cfg.smtp_security == "STARTTLS":
            with smtplib.SMTP(
                cfg.smtp_host,
                cfg.smtp_port,
                timeout=cfg.smtp_timeout,
            ) as smtp:
                code, _ = smtp.ehlo()
                if not (200 <= code < 400):
                    raise smtplib.SMTPHeloError(code, b"EHLO failed")
                if not smtp.has_extn("starttls"):
                    raise smtplib.SMTPNotSupportedError(
                        "STARTTLS não anunciado pelo servidor"
                    )
                smtp.starttls(context=context)
                smtp.ehlo()
                smtp.login(cfg.smtp_username, cfg.smtp_password)
        else:
            raise MailDeliveryError(
                "SMTP_CONFIG_ERROR",
                "SMTP_SECURITY deve ser SSL ou STARTTLS.",
            )

        return {
            "ok": True,
            "code": "SMTP_OK",
            "message": "Ligação TLS e autenticação SMTP concluídas com sucesso.",
        }

    except MailDeliveryError as exc:
        return {"ok": False, "code": exc.code, "message": exc.safe_message}
    except Exception as exc:
        translated = _translate_smtp_exception(exc)
        logger.warning(
            "Diagnóstico SMTP code=%s exception=%s host=%s port=%s security=%s",
            translated.code,
            type(exc).__name__,
            cfg.smtp_host,
            cfg.smtp_port,
            cfg.smtp_security,
        )
        return {
            "ok": False,
            "code": translated.code,
            "message": translated.safe_message,
        }


def send_mfa_code(to_email: str, code: str) -> None:
    _send(
        to_email,
        "Código de verificação - MyBooks",
        (
            "Foi solicitado um acesso à sua conta MyBooks.\n\n"
            f"Código de verificação: {code}\n\n"
            "O código expira em 5 minutos e só pode ser utilizado uma vez.\n"
            "Se não foi você, ignore esta mensagem e altere a sua palavra-passe."
        ),
    )


def send_invitation(to_email: str, full_name: str, activation_url: str) -> None:
    _send(
        to_email,
        "Convite para o MyBooks",
        (
            f"Olá, {full_name}.\n\n"
            "Foi criada uma conta para si no MyBooks.\n"
            "Defina a sua palavra-passe através do link abaixo:\n\n"
            f"{activation_url}\n\n"
            "O link expira em 24 horas e só pode ser utilizado uma vez."
        ),
    )
