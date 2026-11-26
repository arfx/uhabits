/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isoron.uhabits.database

import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.runBlocking
import org.isoron.platform.io.AndroidFileOpener
import org.isoron.uhabits.AndroidDirFinder
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.utils.DatabaseUtils
import org.junit.Test
import java.io.File

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
                "SELECT color FROM Habits WHERE uuid = ?",
                arrayOf(uuid)
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(19, cursor.getInt(0))
            }
            backup.rawQuery(
                "SELECT color FROM HabitCustomColors WHERE habit_uuid = ?",
                arrayOf(uuid)
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(expectedColor, cursor.getInt(0))
            }
        }

        fixtures.purgeHabits(habitList)
        val backupFile = AndroidFileOpener(targetContext.assets, targetContext.filesDir)
            .openUserFile("Backups/${File(backupPath).name}")
        assertTrue(component.parent.genericImporter.canHandle(backupFile))
        component.parent.genericImporter.importHabitsFromFile(backupFile)

        val restored = habitList.getByUUID(uuid)
        assertNotNull(restored)
        assertEquals(PaletteColor.custom(expectedColor), restored!!.color)
    }
}
