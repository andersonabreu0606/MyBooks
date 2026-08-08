package pt.mybooks.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pt.mybooks.app.data.Book
import pt.mybooks.app.data.Loan
import pt.mybooks.app.util.formatEpochDay
import pt.mybooks.app.util.formatEpochDayForInput
import pt.mybooks.app.util.parseDateInput
import pt.mybooks.app.util.todayEpochDay

@Composable
internal fun BookEditorDialog(
    book: Book?,
    onDismiss: () -> Unit,
    onSave: (title: String, author: String, isbn: String, notes: String) -> Unit,
) {
    var title by remember(book?.id) { mutableStateOf(book?.title.orEmpty()) }
    var author by remember(book?.id) { mutableStateOf(book?.author.orEmpty()) }
    var isbn by remember(book?.id) { mutableStateOf(book?.isbn.orEmpty()) }
    var notes by remember(book?.id) { mutableStateOf(book?.notes.orEmpty()) }
    var submitted by remember(book?.id) { mutableStateOf(false) }
    val titleError = submitted && title.isBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (book == null) "Adicionar livro" else "Editar livro") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Título *") },
                    singleLine = true,
                    isError = titleError,
                    supportingText = if (titleError) ({ Text("Indica o título do livro.") }) else null,
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Autor") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = isbn,
                    onValueChange = { isbn = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ISBN") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notas") },
                    minLines = 2,
                    maxLines = 4,
                )
                Text(
                    "* Campo obrigatório",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    submitted = true
                    if (title.isNotBlank()) onSave(title, author, isbn, notes)
                }
            ) {
                Text(if (book == null) "Adicionar" else "Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
internal fun LoanEditorDialog(
    book: Book,
    onDismiss: () -> Unit,
    onSave: (borrower: String, phone: String, dueDateEpochDay: Long) -> Unit,
) {
    val today = remember { todayEpochDay() }
    var borrower by remember(book.id) { mutableStateOf("") }
    var phone by remember(book.id) { mutableStateOf("") }
    var dueDate by remember(book.id) { mutableStateOf(formatEpochDayForInput(today + 14)) }
    var submitted by remember(book.id) { mutableStateOf(false) }
    val parsedDate = parseDateInput(dueDate)
    val borrowerError = submitted && borrower.isBlank()
    val dateError = submitted && (parsedDate == null || parsedDate < today)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Emprestar livro") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(book.title, style = MaterialTheme.typography.titleMedium)
                if (book.author.isNotBlank()) {
                    Text(book.author, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(2.dp))
                OutlinedTextField(
                    value = borrower,
                    onValueChange = { borrower = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Emprestado a *") },
                    singleLine = true,
                    isError = borrowerError,
                    supportingText = if (borrowerError) ({ Text("Indica o nome da pessoa.") }) else null,
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Telefone") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Data prevista *") },
                    placeholder = { Text("dd/mm/aaaa") },
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    singleLine = true,
                    isError = dateError,
                    supportingText = if (dateError) {
                        ({ Text("Usa dd/mm/aaaa e uma data igual ou posterior a hoje.") })
                    } else null,
                )
                Text(
                    "Prazo rápido",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7L, 14L, 30L).forEach { days ->
                        AssistChip(
                            onClick = { dueDate = formatEpochDayForInput(today + days) },
                            label = { Text("$days dias") },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    submitted = true
                    if (borrower.isNotBlank() && parsedDate != null && parsedDate >= today) {
                        onSave(borrower, phone, parsedDate)
                    }
                }
            ) { Text("Registar empréstimo") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
internal fun ConfirmReturnDialog(
    loan: Loan,
    book: Book?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar devolução") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = book?.title ?: "Livro",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text("Emprestado a ${loan.borrowerName}.")
                Text(
                    "A devolução ficará registada com a data de hoje, ${formatEpochDay(todayEpochDay())}.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Confirmar devolução") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
internal fun ConfirmDeleteDialog(
    book: Book,
    hasActiveLoan: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.WarningAmber,
                contentDescription = null,
                tint = if (hasActiveLoan) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
            )
        },
        title = { Text(if (hasActiveLoan) "Livro emprestado" else "Eliminar livro?") },
        text = {
            Text(
                if (hasActiveLoan) {
                    "“${book.title}” só pode ser eliminado depois de ser marcado como devolvido."
                } else {
                    "“${book.title}” e o respetivo histórico de empréstimos serão eliminados deste dispositivo."
                }
            )
        },
        confirmButton = {
            if (hasActiveLoan) {
                Button(onClick = onDismiss) { Text("Entendido") }
            } else {
                Button(onClick = onConfirm) { Text("Eliminar") }
            }
        },
        dismissButton = if (hasActiveLoan) null else {
            { TextButton(onClick = onDismiss) { Text("Cancelar") } }
        },
    )
}
