package org.isoron.uhabits.core.models

/**
 * Converts historical entries when changing a habit between boolean and numerical types.
 * Dates and existing notes are preserved.
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

    /**
     * Includes automatic successes from the computed history without copying generated UNKNOWN
     * placeholders into the source data. Explicit original entries always take precedence.
     */
    fun booleanToNumerical(
        originalEntries: List<Entry>,
        computedEntries: List<Entry>,
        yesValue: Int
    ): List<Entry> {
        val entriesByDate = originalEntries.associateBy { it.date }.toMutableMap()
        for (entry in computedEntries) {
            if (entry.value == Entry.YES_AUTO && entry.date !in entriesByDate) {
                entriesByDate[entry.date] = entry
            }
        }
        return booleanToNumerical(entriesByDate.values.sortedByDescending { it.date }, yesValue)
    }

    /**
     * Converts positive numerical entries to checks and appends their former value and unit
     * to existing notes, separated by " - ". Zero values remain unchecked without extra text.
     */
    fun numericalToBoolean(entries: List<Entry>, unit: String): List<Entry> =
        entries.map { entry ->
            val value = when {
                entry.value == Entry.SKIP -> Entry.SKIP
                entry.value == Entry.UNKNOWN -> Entry.UNKNOWN
                entry.value > 0 -> Entry.YES_MANUAL
                else -> Entry.NO
            }
            val notes = if (entry.value > 0) {
                val formerValue = formatNumericalValue(entry.value)
                val valueWithUnit = listOf(formerValue, unit.trim())
                    .filter { it.isNotEmpty() }
                    .joinToString(" ")
                if (entry.notes.isEmpty()) valueWithUnit
                else "${entry.notes} - $valueWithUnit"
            } else {
                entry.notes
            }
            entry.copy(value = value, notes = notes, completed = false)
        }

    /** Numerical entry values are stored as thousandths. */
    private fun formatNumericalValue(value: Int): String {
        val whole = value / 1000
        val fraction = (value % 1000).toString().padStart(3, '0').trimEnd('0')
        return if (fraction.isEmpty()) whole.toString() else "$whole,$fraction"
    }
}
