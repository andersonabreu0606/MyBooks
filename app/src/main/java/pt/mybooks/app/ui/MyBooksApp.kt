package pt.mybooks.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.mybooks.app.data.Book
import pt.mybooks.app.data.Loan

private enum class MainDestination(val label: String) {
    HOME("Início"),
    BOOKS("Livros"),
    LOANS("Empréstimos"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBooksApp(viewModel: MyBooksViewModel = viewModel()) {
    val library by viewModel.library.collectAsState()
    var destinationName by rememberSaveable { mutableStateOf(MainDestination.HOME.name) }
    val destination = MainDestination.valueOf(destinationName)
    var editingBook by remember { mutableStateOf<Book?>(null) }
    var isAddingBook by rememberSaveable { mutableStateOf(false) }
    var lendingBook by remember { mutableStateOf<Book?>(null) }
    var returningLoan by remember { mutableStateOf<Loan?>(null) }
    var deletingBook by remember { mutableStateOf<Book?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (destination == MainDestination.HOME) "MyBooks" else destination.label,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                MainDestination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destinationName = item.name },
                        icon = {
                            Icon(
                                imageVector = when (item) {
                                    MainDestination.HOME -> Icons.Default.Home
                                    MainDestination.BOOKS -> Icons.Default.LibraryBooks
                                    MainDestination.LOANS -> Icons.Default.SwapHoriz
                                },
                                contentDescription = item.label,
                            )
                        },
                        label = { Text(item.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (destination != MainDestination.LOANS) {
                FloatingActionButton(onClick = { isAddingBook = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar livro")
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (destination) {
                MainDestination.HOME -> HomeScreen(
                    library = library,
                    onAddBook = { isAddingBook = true },
                    onOpenBooks = { destinationName = MainDestination.BOOKS.name },
                    onOpenLoans = { destinationName = MainDestination.LOANS.name },
                    onReturn = { returningLoan = it },
                )

                MainDestination.BOOKS -> BooksScreen(
                    library = library,
                    onAddBook = { isAddingBook = true },
                    onEditBook = { editingBook = it },
                    onDeleteBook = { deletingBook = it },
                    onLendBook = { lendingBook = it },
                )

                MainDestination.LOANS -> LoansScreen(
                    library = library,
                    onReturn = { returningLoan = it },
                    onFindBooks = { destinationName = MainDestination.BOOKS.name },
                )
            }
        }
    }

    if (isAddingBook || editingBook != null) {
        BookEditorDialog(
            book = editingBook,
            onDismiss = {
                isAddingBook = false
                editingBook = null
            },
            onSave = { title, author, isbn, notes ->
                viewModel.saveBook(editingBook?.id, title, author, isbn, notes)
                isAddingBook = false
                editingBook = null
            },
        )
    }

    lendingBook?.let { book ->
        LoanEditorDialog(
            book = book,
            onDismiss = { lendingBook = null },
            onSave = { borrower, phone, dueDate ->
                viewModel.lendBook(book.id, borrower, phone, dueDate)
                lendingBook = null
                destinationName = MainDestination.LOANS.name
            },
        )
    }

    returningLoan?.let { loan ->
        ConfirmReturnDialog(
            loan = loan,
            book = library.books.firstOrNull { it.id == loan.bookId },
            onDismiss = { returningLoan = null },
            onConfirm = {
                viewModel.returnBook(loan.id)
                returningLoan = null
            },
        )
    }

    deletingBook?.let { book ->
        ConfirmDeleteDialog(
            book = book,
            hasActiveLoan = viewModel.activeLoanFor(book.id) != null,
            onDismiss = { deletingBook = null },
            onConfirm = {
                viewModel.deleteBook(book.id)
                deletingBook = null
            },
        )
    }
}
