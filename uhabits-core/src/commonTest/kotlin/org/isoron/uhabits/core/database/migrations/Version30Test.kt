package org.isoron.uhabits.core.database.migrations

import kotlinx.coroutines.test.runTest
import org.isoron.platform.io.Database
import org.isoron.platform.io.format
import org.isoron.platform.io.migrateTo
import org.isoron.platform.io.query
import org.isoron.uhabits.core.BaseUnitTest
import kotlin.test.Test
import kotlin.test.assertEquals

class Version30Test : BaseUnitTest() {
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
    fun testMigrateTo30AddsGroupGoalRuleWithOrDefaults() = runTest {
        initDb()
        migrateTo(30)
        db.query("select group_goal_type, group_goal_threshold from Habits") { stmt ->
            assertEquals(0, stmt.getInt(0))
            assertEquals(1, stmt.getInt(1))
        }
    }
}
