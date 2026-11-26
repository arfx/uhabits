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
package org.isoron.uhabits.core.database

import kotlinx.coroutines.test.runTest
import org.isoron.platform.io.TestDatabaseHelper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HabitCustomColorRepositoryTest {
    @Test
    fun testCustomColorInsertUpdateClearAndDelete() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()
        val repo = HabitRepository(db)
        val firstColor = 0xFF12AB34.toInt()
        val secondColor = 0xFF987654.toInt()
        val data = HabitData(
            name = "Custom color habit",
            position = 0,
            uuid = "custom-color-habit",
            customColor = firstColor
        )

        data.id = repo.insert(data)
        assertEquals(firstColor, repo.findAll().single().customColor)

        data.customColor = secondColor
        repo.update(data)
        assertEquals(secondColor, repo.findAll().single().customColor)

        data.customColor = null
        repo.update(data)
        assertNull(repo.findAll().single().customColor)

        data.customColor = firstColor
        repo.update(data)
        repo.delete(data.id!!)
        assertEquals(0, repo.findAll().size)

        db.close()
    }

    @Test
    fun testCustomColorsAreAssociatedWithTheirHabitUuid() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()
        val repo = HabitRepository(db)
        val firstColor = 0xFF112233.toInt()
        val secondColor = 0xFF445566.toInt()
        val quotedUuid = "habit'with-quote"
        val quotedColor = 0xFF778899.toInt()

        repo.insert(
            HabitData(name = "First", position = 0, uuid = "first-uuid", customColor = firstColor)
        )
        repo.insert(
            HabitData(name = "Second", position = 1, uuid = "second-uuid", customColor = secondColor)
        )
        repo.insert(
            HabitData(name = "Quoted UUID", position = 2, uuid = quotedUuid, customColor = quotedColor)
        )

        val colors = repo.findAll().associate { it.uuid to it.customColor }
        assertEquals(firstColor, colors["first-uuid"])
        assertEquals(secondColor, colors["second-uuid"])
        assertEquals(quotedColor, colors[quotedUuid])

        db.close()
    }
}
