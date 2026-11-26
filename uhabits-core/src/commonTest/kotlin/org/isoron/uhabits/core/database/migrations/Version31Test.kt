package org.isoron.uhabits.core.database.migrations

import kotlinx.coroutines.test.runTest
import org.isoron.platform.io.Database
import org.isoron.platform.io.format
import org.isoron.platform.io.migrateTo
import org.isoron.platform.io.query
import org.isoron.uhabits.core.BaseUnitTest
import kotlin.test.Test
import kotlin.test.assertEquals

class Version31Test : BaseUnitTest() {
    private lateinit var db: Database

    private suspend fun initDb() {
        db = openDatabaseResource("/databases/022.db")
    }

    private suspend fun migrateTo(version: Int) {
        db.migrateTo(version) { v ->
            val path = "migrations/${format("%02d.sql", v)}"
            fileOpener.openResourceFile(path).lines().joinToString("\n")
        }
    }

    @Test
    fun testMigrateTo31CreatesCustomColorTableWithoutChangingHabits() = runTest {
        initDb()
        var habitsBefore = -1
        db.query("SELECT COUNT(*) FROM Habits") { habitsBefore = it.getInt(0) }

        migrateTo(31)

        var tableCount = 0
        db.query(
            "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='HabitCustomColors'"
        ) { tableCount = it.getInt(0) }
        assertEquals(1, tableCount)

        var habitsAfter = -1
        db.query("SELECT COUNT(*) FROM Habits") { habitsAfter = it.getInt(0) }
        assertEquals(habitsBefore, habitsAfter)

        val columns = mutableSetOf<String>()
        db.query("PRAGMA table_info(HabitCustomColors)") { stmt ->
            columns.add(stmt.getText(1))
        }
        assertEquals(setOf("habit_uuid", "color"), columns)
    }
}
