package pt.mybooks.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pt.mybooks.app.data.Book
import pt.mybooks.app.data.LibraryRepository
import pt.mybooks.app.data.LibrarySnapshot
import pt.mybooks.app.data.Loan
import pt.mybooks.app.util.todayEpochDay
import java.util.UUID

class MyBooksViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LibraryRepository(application)
    private val _library = MutableStateFlow(repository.load())
    val library: StateFlow<LibrarySnapshot> = _library.asStateFlow()

    fun saveBook(existingId: String?, title: String, author: String, isbn: String, notes: String) {
        mutate { snapshot ->
            val cleanTitle = title.trim()
            val cleanAuthor = author.trim()
            val existing = snapshot.books.firstOrNull { it.id == existingId }
            val book = Book(
                id = existing?.id ?: UUID.randomUUID().toString(),
                title = cleanTitle,
                author = cleanAuthor,
                isbn = isbn.trim(),
                notes = notes.trim(),
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            )
            snapshot.copy(
                books = if (existing == null) {
                    snapshot.books + book
                } else {
                    snapshot.books.map { if (it.id == existing.id) book else it }
                }
            )
        }
    }

    fun deleteBook(bookId: String) {
        if (activeLoanFor(bookId) != null) return
        mutate { snapshot ->
            snapshot.copy(
                books = snapshot.books.filterNot { it.id == bookId },
                loans = snapshot.loans.filterNot { it.bookId == bookId },
            )
        }
    }

    fun lendBook(bookId: String, borrower: String, phone: String, dueDateEpochDay: Long) {
        if (activeLoanFor(bookId) != null) return
        mutate { snapshot ->
            snapshot.copy(
                loans = snapshot.loans + Loan(
                    id = UUID.randomUUID().toString(),
                    bookId = bookId,
                    borrowerName = borrower.trim(),
                    borrowerPhone = phone.trim(),
                    loanDateEpochDay = todayEpochDay(),
                    dueDateEpochDay = dueDateEpochDay,
                )
            )
        }
    }

    fun returnBook(loanId: String) {
        mutate { snapshot ->
            snapshot.copy(
                loans = snapshot.loans.map { loan ->
                    if (loan.id == loanId && loan.returnedDateEpochDay == null) {
                        loan.copy(returnedDateEpochDay = todayEpochDay())
                    } else {
                        loan
                    }
                }
            )
        }
    }

    fun activeLoanFor(bookId: String): Loan? = _library.value.loans.firstOrNull {
        it.bookId == bookId && it.returnedDateEpochDay == null
    }

    private fun mutate(transform: (LibrarySnapshot) -> LibrarySnapshot) {
        _library.update { current ->
            transform(current).also(repository::save)
        }
    }
}

fun LibrarySnapshot.activeLoans(): List<LoanWithBook> = loans
    .asSequence()
    .filter { it.returnedDateEpochDay == null }
    .mapNotNull { loan -> books.firstOrNull { it.id == loan.bookId }?.let { LoanWithBook(loan, it) } }
    .sortedBy { it.loan.dueDateEpochDay }
    .toList()

fun LibrarySnapshot.loanHistory(): List<LoanWithBook> = loans
    .asSequence()
    .filter { it.returnedDateEpochDay != null }
    .mapNotNull { loan -> books.firstOrNull { it.id == loan.bookId }?.let { LoanWithBook(loan, it) } }
    .sortedByDescending { it.loan.returnedDateEpochDay }
    .toList()
