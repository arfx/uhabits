package org.isoron.uhabits.database

import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.runBlocking
import org.isoron.platform.io.UserFile
import org.isoron.uhabits.AndroidDirFinder
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.utils.DatabaseUtils
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CustomColorBackupTest : BaseAndroidTest() {
    @Test
    fun testCustomColorSurvivesCompatibleBackupAndRestore() = runBlocking {
        val uuid = "custom-color-backup-roundtrip"
        val expectedColor = 0xFF12AB34.toInt()
        val habit = fixtures.createEmptyHabit().apply {
            name = "Custom color backup"
            this.uuid = uuid
            color = PaletteColor.custom(expectedColor)
        }
        habitList.update(listOf(habit))

        val backupDir = AndroidDirFinder(targetContext).getFilesDir("Backups")!!
        val backupPath = DatabaseUtils.saveDatabaseCopy(targetContext, backupDir)

        SQLiteDatabase.openDatabase(backupPath, null, SQLiteDatabase.OPEN_READONLY).use { backup ->
            backup.rawQuery(
                "SELECT color FROM HabitCustomColors WHERE habit_uuid = ?",
                arrayOf(uuid)
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(expectedColor, cursor.getInt(0))
            }
        }

        fixtures.purgeHabits(habitList)
        val backupFile = UserFile(backupPath)
        assertTrue(component.parent.genericImporter.canHandle(backupFile))
        component.parent.genericImporter.importHabitsFromFile(backupFile)

        val restored = habitList.getByUUID(uuid)
        assertNotNull(restored)
        assertEquals(PaletteColor.custom(expectedColor), restored!!.color)
    }
}
