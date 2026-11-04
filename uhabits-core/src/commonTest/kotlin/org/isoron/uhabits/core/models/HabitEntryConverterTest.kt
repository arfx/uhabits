package org.isoron.uhabits.core.models

import org.isoron.platform.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class HabitEntryConverterTest {
    @Test
    fun booleanToNumericalMapsValuesAndPreservesNotes() {
        val entries = listOf(
            Entry(LocalDate(2026, 1, 1), Entry.YES_MANUAL, "done"),
            Entry(LocalDate(2026, 1, 2), Entry.YES_AUTO, "scheduled"),
            Entry(LocalDate(2026, 1, 3), Entry.NO, "missed"),
            Entry(LocalDate(2026, 1, 4), Entry.SKIP, "skip"),
            Entry(LocalDate(2026, 1, 5), Entry.UNKNOWN, "unknown")
        )

        val converted = HabitEntryConverter.booleanToNumerical(entries, 2500)

        assertEquals(
            listOf(
                Entry(LocalDate(2026, 1, 1), 2500, "done"),
                Entry(LocalDate(2026, 1, 2), 2500, "scheduled"),
                Entry(LocalDate(2026, 1, 3), 0, "missed"),
                Entry(LocalDate(2026, 1, 4), Entry.SKIP, "skip"),
                Entry(LocalDate(2026, 1, 5), Entry.UNKNOWN, "unknown")
            ),
            converted
        )
    }

    @Test
    fun booleanToNumericalIncludesComputedAutomaticSuccessesOnly() {
        val originalEntries = listOf(
            Entry(LocalDate(2026, 1, 3), Entry.NO, "explicit no"),
            Entry(LocalDate(2026, 1, 1), Entry.YES_MANUAL, "manual yes")
        )
        val computedEntries = listOf(
            Entry(LocalDate(2026, 1, 4), Entry.UNKNOWN),
            Entry(LocalDate(2026, 1, 3), Entry.NO, "explicit no"),
            Entry(LocalDate(2026, 1, 2), Entry.YES_AUTO),
            Entry(LocalDate(2026, 1, 1), Entry.YES_MANUAL, "manual yes")
        )

        val converted = HabitEntryConverter.booleanToNumerical(
            originalEntries,
            computedEntries,
            1500
        )

        assertEquals(
            listOf(
                Entry(LocalDate(2026, 1, 3), 0, "explicit no"),
                Entry(LocalDate(2026, 1, 2), 1500),
                Entry(LocalDate(2026, 1, 1), 1500, "manual yes")
            ),
            converted
        )
    }

    @Test
    fun numericalToBooleanMapsValuesAndPreservesNotes() {
        val entries = listOf(
            Entry(LocalDate(2026, 1, 1), 2500, "positive"),
            Entry(LocalDate(2026, 1, 2), 0, "zero"),
            Entry(LocalDate(2026, 1, 3), -200, "negative"),
            Entry(LocalDate(2026, 1, 4), Entry.SKIP, "skip"),
            Entry(LocalDate(2026, 1, 5), Entry.UNKNOWN, "unknown")
        )

        val converted = HabitEntryConverter.numericalToBoolean(entries)

        assertEquals(
            listOf(
                Entry(LocalDate(2026, 1, 1), Entry.YES_MANUAL, "positive"),
                Entry(LocalDate(2026, 1, 2), Entry.NO, "zero"),
                Entry(LocalDate(2026, 1, 3), Entry.NO, "negative"),
                Entry(LocalDate(2026, 1, 4), Entry.SKIP, "skip"),
                Entry(LocalDate(2026, 1, 5), Entry.UNKNOWN, "unknown")
            ),
            converted
        )
    }
}
