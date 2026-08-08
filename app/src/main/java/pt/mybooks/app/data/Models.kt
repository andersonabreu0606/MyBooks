package pt.mybooks.app.data

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val isbn: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

data class Loan(
    val id: String,
    val bookId: String,
    val borrowerName: String,
    val borrowerPhone: String = "",
    val loanDateEpochDay: Long,
    val dueDateEpochDay: Long,
    val returnedDateEpochDay: Long? = null,
)

data class LibrarySnapshot(
    val books: List<Book> = emptyList(),
    val loans: List<Loan> = emptyList(),
)

data class LoanWithBook(
    val loan: Loan,
    val book: Book,
)
