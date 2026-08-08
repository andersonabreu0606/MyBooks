package pt.mybooks.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class LibraryRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): LibrarySnapshot = runCatching {
        val root = JSONObject(preferences.getString(KEY_LIBRARY, "{}") ?: "{}")
        LibrarySnapshot(
            books = root.optJSONArray("books").toBooks(),
            loans = root.optJSONArray("loans").toLoans(),
        )
    }.getOrDefault(LibrarySnapshot())

    fun save(snapshot: LibrarySnapshot) {
        val root = JSONObject().apply {
            put("books", JSONArray().apply { snapshot.books.forEach { put(it.toJson()) } })
            put("loans", JSONArray().apply { snapshot.loans.forEach { put(it.toJson()) } })
        }
        preferences.edit().putString(KEY_LIBRARY, root.toString()).apply()
    }

    private fun JSONArray?.toBooks(): List<Book> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val id = item.optString("id")
                val title = item.optString("title")
                if (id.isBlank() || title.isBlank()) continue
                add(
                    Book(
                        id = id,
                        title = title,
                        author = item.optString("author"),
                        isbn = item.optString("isbn"),
                        notes = item.optString("notes"),
                        createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                    )
                )
            }
        }
    }

    private fun JSONArray?.toLoans(): List<Loan> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val id = item.optString("id")
                val bookId = item.optString("bookId")
                if (id.isBlank() || bookId.isBlank()) continue
                add(
                    Loan(
                        id = id,
                        bookId = bookId,
                        borrowerName = item.optString("borrowerName"),
                        borrowerPhone = item.optString("borrowerPhone"),
                        loanDateEpochDay = item.optLong("loanDateEpochDay"),
                        dueDateEpochDay = item.optLong("dueDateEpochDay"),
                        returnedDateEpochDay = if (item.isNull("returnedDateEpochDay")) {
                            null
                        } else {
                            item.optLong("returnedDateEpochDay")
                        },
                    )
                )
            }
        }
    }

    private fun Book.toJson() = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("author", author)
        put("isbn", isbn)
        put("notes", notes)
        put("createdAt", createdAt)
    }

    private fun Loan.toJson() = JSONObject().apply {
        put("id", id)
        put("bookId", bookId)
        put("borrowerName", borrowerName)
        put("borrowerPhone", borrowerPhone)
        put("loanDateEpochDay", loanDateEpochDay)
        put("dueDateEpochDay", dueDateEpochDay)
        put("returnedDateEpochDay", returnedDateEpochDay ?: JSONObject.NULL)
    }

    private companion object {
        const val PREFERENCES_NAME = "mybooks_library"
        const val KEY_LIBRARY = "library_json"
    }
}
