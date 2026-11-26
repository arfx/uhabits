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
