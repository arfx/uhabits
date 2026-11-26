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
package org.isoron.uhabits.core.io

import kotlinx.coroutines.test.runTest
import org.isoron.platform.io.ZipReader
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.PaletteColor
import kotlin.test.Test
import kotlin.test.assertTrue

class HabitsCSVCustomColorTest : BaseUnitTest() {
    @Test
    fun testCustomColorIsExportedAsSixDigitHexValue() = runTest {
        val habit = modelFactory.buildHabit().apply {
            name = "Custom color"
            uuid = "csv-custom-color"
            color = PaletteColor.custom(0xFF12AB34.toInt())
        }
        habitList.add(habit)

        val csv = habitList.writeCSV(listOf(habit))
        assertTrue(csv.contains("#12AB34"), csv)

        val exporter = HabitsCSVExporter(habitList, listOf(habit))
        val archive = ZipReader(exporter.writeArchive()).entries()
        val habitsCsv = archive.first { it.name == "Habits.csv" }.content
        assertTrue(habitsCsv.contains("#12AB34"), habitsCsv)
    }

    @Test
    fun testBuiltInPaletteColorKeepsHexExportFormat() {
        assertTrue(PaletteColor(6).toCsvColor().matches(Regex("#[0-9A-Fa-f]{6}")))
    }
}
