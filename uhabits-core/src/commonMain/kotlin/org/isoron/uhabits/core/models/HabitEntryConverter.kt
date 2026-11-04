package org.isoron.uhabits.core.models

/**
 * Converts historical entries when changing a habit between boolean and numerical types.
 * Dates and notes are preserved exactly.
 */
object HabitEntryConverter {
    fun booleanToNumerical(entries: List<Entry>, yesValue: Int): List<Entry> =
        entries.map { entry ->
            val value = when (entry.value) {
                Entry.YES_MANUAL, Entry.YES_AUTO -> yesValue
                Entry.NO -> 0
                else -> entry.value
            }
            entry.copy(value = value, completed = false)
        }

    fun numericalToBoolean(entries: List<Entry>): List<Entry> =
        entries.map { entry ->
            val value = when {
                entry.value == Entry.SKIP -> Entry.SKIP
                entry.value == Entry.UNKNOWN -> Entry.UNKNOWN
                entry.value > 0 -> Entry.YES_MANUAL
                else -> Entry.NO
            }
            entry.copy(value = value, completed = false)
        }
}
