\
from sqlalchemy import select, or_
from .db import db_session
from .models import Book

def list_books(search: str = ""):
    with db_session() as db:
        stmt = select(Book).order_by(Book.title)
        q = search.strip()
        if q:
            like = f"%{q}%"
            stmt = stmt.where(
                or_(
                    Book.title.ilike(like),
                    Book.author_text.ilike(like),
                    Book.isbn13.ilike(like),
                )
            )
        books = db.scalars(stmt.limit(500)).all()
        return [
            {
                "id": b.id,
                "Título": b.title,
                "Autor": b.author_text or "",
                "ISBN-13": b.isbn13 or "",
                "Ano": b.published_year or "",
            }
            for b in books
        ]

def create_book(actor: dict, title: str, author: str = "", isbn13: str = "", published_year=None):
    if not any(role in actor.get("roles", []) for role in ("ADMIN", "LIBRARIAN")):
        raise PermissionError("Sem permissão para gerir livros.")

    title = title.strip()
    if not title:
        raise ValueError("Título é obrigatório.")

    isbn13 = "".join(ch for ch in isbn13 if ch.isdigit()) or None
    if isbn13 and len(isbn13) != 13:
        raise ValueError("ISBN-13 deve ter 13 dígitos.")

    year = int(published_year) if published_year else None
    if year is not None and (year < 0 or year > 3000):
        raise ValueError("Ano inválido.")

    with db_session() as db:
        if isbn13 and db.scalar(select(Book).where(Book.isbn13 == isbn13)):
            raise ValueError("Já existe um livro com este ISBN-13.")
        db.add(Book(
            title=title,
            author_text=author.strip() or None,
            isbn13=isbn13,
            published_year=year,
            created_by=actor["id"],
        ))
