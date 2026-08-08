package pt.mybooks.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pt.mybooks.app.data.Book
import pt.mybooks.app.data.LibrarySnapshot
import pt.mybooks.app.data.Loan
import pt.mybooks.app.data.LoanWithBook
import pt.mybooks.app.util.daysUntil
import pt.mybooks.app.util.formatEpochDay
import pt.mybooks.app.util.todayEpochDay

@Composable
internal fun HomeScreen(
    library: LibrarySnapshot,
    onAddBook: () -> Unit,
    onOpenBooks: () -> Unit,
    onOpenLoans: () -> Unit,
    onReturn: (Loan) -> Unit,
) {
    val active = remember(library) { library.activeLoans() }
    val overdue = active.filter { it.loan.dueDateEpochDay < todayEpochDay() }
    val available = library.books.size - active.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "A tua biblioteca,\nsempre por perto.",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Organiza os teus livros e não percas de vista nenhum empréstimo.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = library.books.size.toString(),
                    label = "livros",
                    icon = Icons.Default.LibraryBooks,
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = available.coerceAtLeast(0).toString(),
                    label = "disponíveis",
                    icon = Icons.Default.CheckCircle,
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = active.size.toString(),
                    label = "emprestados",
                    icon = Icons.Default.SwapHoriz,
                )
            }
        }

        if (overdue.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    shape = RoundedCornerShape(20.dp),
                    onClick = onOpenLoans,
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "${overdue.size} ${if (overdue.size == 1) "empréstimo atrasado" else "empréstimos atrasados"}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text("Toca para veres os detalhes.")
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }
        }

        item {
            SectionTitle(
                title = "Empréstimos atuais",
                action = if (active.isNotEmpty()) "Ver todos" else null,
                onAction = onOpenLoans,
            )
        }

        if (active.isEmpty()) {
            item {
                CompactEmptyCard(
                    icon = Icons.Default.AutoStories,
                    title = "Nenhum livro fora de casa",
                    body = "Quando emprestares um livro, ele aparece aqui.",
                    action = if (library.books.isEmpty()) "Adicionar primeiro livro" else "Ver livros",
                    onAction = if (library.books.isEmpty()) onAddBook else onOpenBooks,
                )
            }
        } else {
            items(active.take(3), key = { it.loan.id }) { item ->
                LoanCard(item = item, onReturn = { onReturn(item.loan) })
            }
        }
    }
}

private enum class BookFilter(val label: String) {
    ALL("Todos"),
    AVAILABLE("Disponíveis"),
    LENT("Emprestados"),
}

@Composable
internal fun BooksScreen(
    library: LibrarySnapshot,
    onAddBook: () -> Unit,
    onEditBook: (Book) -> Unit,
    onDeleteBook: (Book) -> Unit,
    onLendBook: (Book) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filterName by rememberSaveable { mutableStateOf(BookFilter.ALL.name) }
    val filter = BookFilter.valueOf(filterName)
    val lentBookIds = remember(library.loans) {
        library.loans.filter { it.returnedDateEpochDay == null }.map { it.bookId }.toSet()
    }
    val visibleBooks = remember(library.books, lentBookIds, query, filter) {
        library.books
            .filter { book ->
                val matchesQuery = query.isBlank() || listOf(book.title, book.author, book.isbn)
                    .any { it.contains(query.trim(), ignoreCase = true) }
                val matchesFilter = when (filter) {
                    BookFilter.ALL -> true
                    BookFilter.AVAILABLE -> book.id !in lentBookIds
                    BookFilter.LENT -> book.id in lentBookIds
                }
                matchesQuery && matchesFilter
            }
            .sortedBy { it.title.lowercase() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Pesquisar") },
            placeholder = { Text("Título, autor ou ISBN") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BookFilter.entries.forEach { item ->
                FilterChip(
                    selected = filter == item,
                    onClick = { filterName = item.name },
                    label = { Text(item.label) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        if (library.books.isEmpty()) {
            EmptyState(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.LibraryBooks,
                title = "A estante está vazia",
                body = "Adiciona os teus livros para começares a organizar empréstimos.",
                action = "Adicionar livro",
                onAction = onAddBook,
            )
        } else if (visibleBooks.isEmpty()) {
            EmptyState(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Search,
                title = "Sem resultados",
                body = "Experimenta alterar a pesquisa ou o filtro.",
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(visibleBooks, key = { it.id }) { book ->
                    val activeLoan = library.loans.firstOrNull {
                        it.bookId == book.id && it.returnedDateEpochDay == null
                    }
                    BookCard(
                        book = book,
                        activeLoan = activeLoan,
                        onEdit = { onEditBook(book) },
                        onDelete = { onDeleteBook(book) },
                        onLend = { onLendBook(book) },
                    )
                }
            }
        }
    }
}

private enum class LoanFilter(val label: String) {
    ACTIVE("Atuais"),
    HISTORY("Histórico"),
}

@Composable
internal fun LoansScreen(
    library: LibrarySnapshot,
    onReturn: (Loan) -> Unit,
    onFindBooks: () -> Unit,
) {
    var filterName by rememberSaveable { mutableStateOf(LoanFilter.ACTIVE.name) }
    val filter = LoanFilter.valueOf(filterName)
    val active = remember(library) { library.activeLoans() }
    val history = remember(library) { library.loanHistory() }
    val visible = if (filter == LoanFilter.ACTIVE) active else history

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LoanFilter.entries.forEach { item ->
                FilterChip(
                    selected = filter == item,
                    onClick = { filterName = item.name },
                    label = {
                        val count = if (item == LoanFilter.ACTIVE) active.size else history.size
                        Text("${item.label}  $count")
                    },
                    leadingIcon = if (item == LoanFilter.HISTORY) {
                        { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null,
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        if (visible.isEmpty()) {
            EmptyState(
                modifier = Modifier.weight(1f),
                icon = if (filter == LoanFilter.ACTIVE) Icons.Default.SwapHoriz else Icons.Default.History,
                title = if (filter == LoanFilter.ACTIVE) "Sem empréstimos atuais" else "Ainda sem histórico",
                body = if (filter == LoanFilter.ACTIVE) {
                    "Escolhe um livro disponível e regista a quem o emprestaste."
                } else {
                    "Os livros devolvidos ficam guardados aqui."
                },
                action = if (filter == LoanFilter.ACTIVE) "Escolher livro" else null,
                onAction = onFindBooks,
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(visible, key = { it.loan.id }) { item ->
                    LoanCard(
                        item = item,
                        onReturn = if (item.loan.returnedDateEpochDay == null) {
                            { onReturn(item.loan) }
                        } else null,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    value: String,
    label: String,
    icon: ImageVector,
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(7.dp).size(18.dp),
                )
            }
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, action: String?, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null) {
            androidx.compose.material3.TextButton(onClick = onAction) {
                Text(action)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun BookCard(
    book: Book,
    activeLoan: Loan?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLend: () -> Unit,
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            BookCover(title = book.title)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = book.author.ifBlank { "Autor desconhecido" },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar ${book.title}", modifier = Modifier.size(19.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar ${book.title}", modifier = Modifier.size(19.dp))
                    }
                }

                Spacer(Modifier.height(10.dp))
                if (activeLoan == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(text = "Disponível", isAlert = false)
                        Spacer(Modifier.weight(1f))
                        Button(onClick = onLend, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                            Text("Emprestar")
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill(text = "Com ${activeLoan.borrowerName}", isAlert = false)
                        Text(
                            text = "Devolução: ${formatEpochDay(activeLoan.dueDateEpochDay)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (activeLoan.dueDateEpochDay < todayEpochDay()) {
                                MaterialTheme.colorScheme.error
                            } else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookCover(title: String) {
    val initials = title.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .take(2)
    Box(
        modifier = Modifier
            .size(width = 58.dp, height = 82.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials.ifBlank { "L" },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun LoanCard(item: LoanWithBook, onReturn: (() -> Unit)?) {
    val loan = item.loan
    val remaining = daysUntil(loan.dueDateEpochDay)
    val isOverdue = loan.returnedDateEpochDay == null && remaining < 0

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                BookCover(item.book.title)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.book.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        item.book.author.ifBlank { "Autor desconhecido" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(8.dp))
                    StatusPill(
                        text = when {
                            loan.returnedDateEpochDay != null -> "Devolvido"
                            isOverdue -> "${-remaining} ${if (remaining == -1L) "dia atrasado" else "dias atrasado"}"
                            remaining == 0L -> "Devolver hoje"
                            else -> "$remaining ${if (remaining == 1L) "dia restante" else "dias restantes"}"
                        },
                        isAlert = isOverdue,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            DetailLine(Icons.Default.Person, loan.borrowerName)
            if (loan.borrowerPhone.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                DetailLine(Icons.Default.Phone, loan.borrowerPhone)
            }
            Spacer(Modifier.height(6.dp))
            DetailLine(
                Icons.Default.CalendarMonth,
                if (loan.returnedDateEpochDay == null) {
                    "Previsto para ${formatEpochDay(loan.dueDateEpochDay)}"
                } else {
                    "Devolvido em ${formatEpochDay(loan.returnedDateEpochDay)}"
                },
                color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onReturn != null) {
                Spacer(Modifier.height(14.dp))
                Button(onClick = onReturn, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Marcar como devolvido")
                }
            }
        }
    }
}

@Composable
private fun DetailLine(icon: ImageVector, text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
private fun StatusPill(text: String, isAlert: Boolean) {
    val background = if (isAlert) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val foreground = if (isAlert) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Surface(color = background, contentColor = foreground, shape = RoundedCornerShape(50)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun CompactEmptyCard(
    icon: ImageVector,
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    body: String,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(20.dp).size(42.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (action != null) {
            Spacer(Modifier.height(18.dp))
            Button(onClick = onAction) { Text(action) }
        }
    }
}
