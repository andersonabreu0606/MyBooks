package pt.mybooks.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {
    @Test
    fun `converte data portuguesa para epoch day`() {
        assertEquals(LocalDate.of(2026, 8, 8).toEpochDay(), parseDateInput("08/08/2026"))
    }

    @Test
    fun `rejeita datas impossiveis e formatos invalidos`() {
        assertNull(parseDateInput("31/02/2026"))
        assertNull(parseDateInput("2026-08-08"))
    }

    @Test
    fun `calcula dias restantes e atraso`() {
        assertEquals(4L, daysUntil(epochDay = 104L, today = 100L))
        assertEquals(-3L, daysUntil(epochDay = 97L, today = 100L))
    }
}
