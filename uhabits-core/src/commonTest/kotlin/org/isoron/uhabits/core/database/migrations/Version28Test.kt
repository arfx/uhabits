package org.isoron.uhabits.core.database.migrations

import kotlinx.coroutines.test.runTest
import org.isoron.platform.io.Database
import org.isoron.platform.io.format
import org.isoron.platform.io.migrateTo
import org.isoron.platform.io.query
import org.isoron.uhabits.core.BaseUnitTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Version28Test : BaseUnitTest() {

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

    private fun dbTest(block: suspend () -> Unit) = runTest {
        initDb()
        block()
    }

    @Test
    fun testMigrateTo28CreatesGroupColumns() = dbTest {
        migrateTo(28)
        db.query("select is_group, group_id from Habits") {}
    }

    @Test
    fun testMigrateTo28KeepsExistingHabitsOutsideGroups() = dbTest {
        migrateTo(28)
        db.query("select is_group, group_id from Habits") { stmt ->
            assertEquals(0, stmt.getInt(0))
            assertNull(stmt.getLongOrNull(1))
        }
    }
}
