\
import streamlit as st

from src.auth import (
    ensure_schema_and_bootstrap,
    start_login,
    verify_mfa,
    create_invited_user,
    activate_account,
    change_password,
    list_users,
)
from src.books import list_books, create_book

st.set_page_config(
    page_title="BookApp",
    page_icon="📚",
    layout="wide",
    initial_sidebar_state="collapsed",
)

# CSS simples mobile-first.
st.markdown("""
<style>
.block-container { max-width: 1180px; padding-top: 1.4rem; padding-bottom: 2rem; }
div[data-testid="stForm"] { border: 1px solid rgba(128,128,128,.25); padding: 1rem; border-radius: 14px; }
@media (max-width: 640px) {
  .block-container { padding-left: .8rem; padding-right: .8rem; padding-top: .8rem; }
  h1 { font-size: 1.8rem !important; }
}
</style>
""", unsafe_allow_html=True)

try:
    ensure_schema_and_bootstrap()
except Exception as exc:
    st.error("Falha na inicialização segura da aplicação.")
    st.exception(exc)
    st.stop()

def init_state():
    defaults = {
        "user": None,
        "mfa_challenge_token": None,
        "mfa_masked_email": None,
    }
    for key, value in defaults.items():
        if key not in st.session_state:
            st.session_state[key] = value

def logout():
    for key in ("user", "mfa_challenge_token", "mfa_masked_email"):
        st.session_state[key] = None
    st.rerun()

init_state()

# Fluxo de ativação de conta via convite.
activate_token = st.query_params.get("activate")
if activate_token and not st.session_state.user:
    st.title("📚 Ativar conta")
    st.caption("Defina a sua palavra-passe. Depois, o acesso exigirá também MFA por e-mail.")
    with st.form("activate_account"):
        p1 = st.text_input("Nova palavra-passe", type="password")
        p2 = st.text_input("Confirmar palavra-passe", type="password")
        submitted = st.form_submit_button("Ativar conta", use_container_width=True)
    if submitted:
        if p1 != p2:
            st.error("As palavras-passe não coincidem.")
        else:
            try:
                activate_account(activate_token, p1)
                st.query_params.clear()
                st.success("Conta ativada. Já pode iniciar sessão.")
                st.rerun()
            except Exception as exc:
                st.error(str(exc))
    st.stop()

# Login: fase 1 - e-mail + password.
if not st.session_state.user and not st.session_state.mfa_challenge_token:
    st.title("📚 BookApp")
    st.subheader("Iniciar sessão")
    st.caption("Acesso protegido por palavra-passe + código MFA enviado por e-mail.")
    with st.form("login"):
        email = st.text_input("E-mail", autocomplete="username")
        password = st.text_input("Palavra-passe", type="password", autocomplete="current-password")
        submitted = st.form_submit_button("Continuar", use_container_width=True)
    if submitted:
        with st.spinner("A validar credenciais..."):
            result = start_login(email, password)
        if result["ok"]:
            st.session_state.mfa_challenge_token = result["challenge_token"]
            st.session_state.mfa_masked_email = result["masked_email"]
            st.rerun()
        else:
            st.error(result["message"])
    st.stop()

# Login: fase 2 - MFA.
if not st.session_state.user and st.session_state.mfa_challenge_token:
    st.title("Verificação MFA")
    st.info(f"Enviámos um código para {st.session_state.mfa_masked_email}.")
    with st.form("mfa"):
        code = st.text_input("Código de 8 dígitos", max_chars=8, autocomplete="one-time-code")
        submitted = st.form_submit_button("Verificar", use_container_width=True)
    if submitted:
        result = verify_mfa(st.session_state.mfa_challenge_token, code)
        if result["ok"]:
            st.session_state.user = result["user"]
            st.session_state.mfa_challenge_token = None
            st.session_state.mfa_masked_email = None
            st.rerun()
        else:
            st.error(result["message"])
    if st.button("Cancelar"):
        st.session_state.mfa_challenge_token = None
        st.session_state.mfa_masked_email = None
        st.rerun()
    st.stop()

user = st.session_state.user
roles = set(user.get("roles", []))

with st.sidebar:
    st.markdown(f"**{user['full_name']}**")
    st.caption(user["email"])
    st.caption(" · ".join(sorted(roles)))
    options = ["Dashboard", "Livros", "Minha conta"]
    if "ADMIN" in roles:
        options.append("Administração")
    page = st.radio("Menu", options)
    if st.button("Terminar sessão", use_container_width=True):
        logout()

if page == "Dashboard":
    st.title("📚 Dashboard")
    books = list_books()
    c1, c2, c3 = st.columns(3)
    c1.metric("Livros no catálogo", len(books))
    c2.metric("Perfil", ", ".join(sorted(roles)))
    c3.metric("MFA", "Ativo")
    st.subheader("Base segura pronta")
    st.write(
        "Autenticação com Argon2id, MFA por e-mail, RBAC, auditoria de eventos "
        "e MySQL via SQLAlchemy."
    )

elif page == "Livros":
    st.title("📚 Livros")
    search = st.text_input("Pesquisar por título, autor ou ISBN")
    rows = list_books(search)
    st.dataframe(rows, use_container_width=True, hide_index=True)

    if roles.intersection({"ADMIN", "LIBRARIAN"}):
        st.subheader("Adicionar livro")
        with st.form("new_book"):
            title = st.text_input("Título")
            author = st.text_input("Autor")
            isbn13 = st.text_input("ISBN-13")
            year = st.number_input("Ano", min_value=0, max_value=3000, value=2026, step=1)
            submitted = st.form_submit_button("Guardar")
        if submitted:
            try:
                create_book(user, title, author, isbn13, year)
                st.success("Livro criado.")
                st.rerun()
            except Exception as exc:
                st.error(str(exc))

elif page == "Minha conta":
    st.title("👤 Minha conta")
    st.write(f"**Nome:** {user['full_name']}")
    st.write(f"**E-mail:** {user['email']}")
    st.write(f"**Perfis:** {', '.join(sorted(roles))}")

    st.subheader("Alterar palavra-passe")
    with st.form("change_password"):
        current = st.text_input("Palavra-passe atual", type="password")
        new1 = st.text_input("Nova palavra-passe", type="password")
        new2 = st.text_input("Confirmar nova palavra-passe", type="password")
        submitted = st.form_submit_button("Alterar")
    if submitted:
        if new1 != new2:
            st.error("As novas palavras-passe não coincidem.")
        else:
            try:
                change_password(user, current, new1)
                st.success("Palavra-passe alterada.")
            except Exception as exc:
                st.error(str(exc))

elif page == "Administração":
    if "ADMIN" not in roles:
        st.error("Acesso negado.")
        st.stop()

    st.title("🛡️ Administração")
    tab1, tab2 = st.tabs(["Utilizadores", "Convidar utilizador"])

    with tab1:
        st.dataframe(list_users(user), use_container_width=True, hide_index=True)

    with tab2:
        with st.form("invite_user"):
            full_name = st.text_input("Nome")
            email = st.text_input("E-mail")
            role = st.selectbox("Perfil", ["READER", "LIBRARIAN", "ADMIN"])
            submitted = st.form_submit_button("Criar e enviar convite")
        if submitted:
            try:
                create_invited_user(user, email, full_name, role)
                st.success("Convite enviado.")
            except Exception as exc:
                st.error(str(exc))
