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

import org.isoron.platform.io.ZipWriter
import org.isoron.platform.io.csvLine
import org.isoron.platform.io.format
import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.models.EntryList
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList
import kotlin.math.min

/**
 * Class that exports the application data to CSV files inside a ZIP archive.
 */
class HabitsCSVExporter(
    private val allHabits: HabitList,
    private val selectedHabits: List<Habit>
) {
    private val delimiter = ","

    suspend fun writeArchive(): ByteArray {
        val zip = ZipWriter()
        val exportableHabits = allHabits.filter { !it.isGroup }
        val selectedExportableHabits = selectedHabits.filter { !it.isGroup }
        zip.addEntry("Habits.csv", allHabits.writeCSV(exportableHabits))
        zip.addEntry("Groups.json", writeGroupsJson(exportableHabits))
        for (h in selectedExportableHabits) {
            val habitDirName = habitDirName(h, exportableHabits)
            zip.addEntry("${habitDirName}Scores.csv", writeScores(h))
            zip.addEntry("${habitDirName}Checkmarks.csv", writeEntries(h.computedEntries))
        }
        zip.addEntry("Scores.csv", writeMultipleHabitsScores(selectedExportableHabits))
        zip.addEntry("Checkmarks.csv", writeMultipleHabitsCheckmarks(selectedExportableHabits))
        return zip.toBytes()
    }

    private fun habitDirName(h: Habit, exportableHabits: List<Habit>): String {
        val sane = sanitizeFilename(h.name)
        return format("%03d", exportableHabits.indexOf(h) + 1) + " " + sane.trim() + "/"
    }

    private fun writeGroupsJson(exportableHabits: List<Habit>): String {
        val groups = allHabits.filter { it.isGroup }
        return buildString {
            append("{\n  \"version\": 1,\n  \"groups\": [")
            groups.forEachIndexed { groupIndex, group ->
                if (groupIndex > 0) append(",")
                val children = exportableHabits.filter { it.groupId == group.id }
                append("\n    {\n")
                append("      \"uuid\": \"${jsonEscape(group.uuid ?: "")}\",\n")
                append("      \"name\": \"${jsonEscape(group.name)}\",\n")
                append("      \"position\": ${group.position},\n")
                append("      \"color\": ${group.color.paletteIndex},\n")
                append("      \"groupGoalEnabled\": ${group.groupGoalEnabled},\n")
                append("      \"groupGoalType\": ${group.groupGoalType.value},\n")
                append("      \"groupGoalThreshold\": ${group.groupGoalThreshold},\n")
                append("      \"frequencyNumerator\": ${group.frequency.numerator},\n")
                append("      \"frequencyDenominator\": ${group.frequency.denominator},\n")
                append("      \"members\": [")
                children.forEachIndexed { childIndex, child ->
                    if (childIndex > 0) append(", ")
                    append("{\"uuid\": \"${jsonEscape(child.uuid ?: "")}\", ")
                    append("\"exportPosition\": ${exportableHabits.indexOf(child) + 1}}")
                }
                append("]\n    }")
            }
            if (groups.isNotEmpty()) append("\n  ")
            append("]\n}\n")
        }
    }

    private fun jsonEscape(value: String): String = buildString {
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }

    private fun sanitizeFilename(name: String): String {
        val s = name.replace("[^ a-zA-Z0-9._-]+".toRegex(), "")
        return s.substring(0, min(s.length, 100))
    }

    private fun writeScores(habit: Habit): String {
        val sb = StringBuilder()
        val today = getToday()
        var oldest = today
        val known = habit.computedEntries.getKnown()
        if (known.isNotEmpty()) oldest = known[known.size - 1].date
        sb.append(csvLine(arrayOf("Date", "Score")))
        if (!habit.hasGoal) return sb.toString()
        for (s in habit.scores.getByInterval(oldest, today)) {
            sb.append(csvLine(arrayOf(s.date.toCSVString(), format("%.4f", s.value))))
        }
        return sb.toString()
    }

    private fun writeEntries(entries: EntryList): String {
        val sb = StringBuilder()
        sb.append(csvLine(arrayOf("Date", "Value", "Notes")))
        for (entry in entries.getKnown()) {
            sb.append(csvLine(arrayOf(entry.date.toCSVString(), entry.formattedValue, entry.notes)))
        }
        return sb.toString()
    }

    private fun writeMultipleHabitsScores(habits: List<Habit>): String {
        val sb = StringBuilder()
        writeMultipleHabitsHeader(sb, habits)
        val timeframe = getTimeframe(habits)
        val oldest = timeframe[0]
        val newest = getToday()
        val scores = habits.map { habit ->
            if (habit.hasGoal) ArrayList(habit.scores.getByInterval(oldest, newest)) else arrayListOf()
        }
        val days = oldest.daysUntil(newest)
        for (i in 0..days) {
            val date = newest.minus(i).toCSVString()
            sb.append(date).append(delimiter)
            for (j in habits.indices) {
                val score = if (habits[j].hasGoal) {
                    format("%.4f", scores[j][i].value)
                } else {
                    ""
                }
                sb.append(score).append(delimiter)
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    private fun writeMultipleHabitsCheckmarks(habits: List<Habit>): String {
        val sb = StringBuilder()
        writeMultipleHabitsHeader(sb, habits)
        val timeframe = getTimeframe(habits)
        val oldest = timeframe[0]
        val newest = getToday()
        val checkmarks = habits.map { ArrayList(it.computedEntries.getByInterval(oldest, newest)) }
        val days = oldest.daysUntil(newest)
        for (i in 0..days) {
            val date = newest.minus(i).toCSVString()
            sb.append(date).append(delimiter)
            for (j in habits.indices) {
                sb.append(checkmarks[j][i].formattedValue).append(delimiter)
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    private fun writeMultipleHabitsHeader(sb: StringBuilder, habits: List<Habit>) {
        val fields = arrayOf("Date") + habits.map { it.name } + arrayOf("")
        sb.append(csvLine(fields))
    }

    private fun getTimeframe(habits: List<Habit>): Array<LocalDate> {
        var oldest = LocalDate(1000000)
        var newest = LocalDate(0)
        for (habit in habits) {
            val entries = habit.computedEntries.getKnown()
            if (entries.isEmpty()) continue
            val currNew = entries[0].date
            val currOld = entries[entries.size - 1].date
            oldest = if (currOld.isOlderThan(oldest)) currOld else oldest
            newest = if (currNew.isNewerThan(newest)) currNew else newest
        }
        if (oldest == LocalDate(1000000)) return arrayOf(getToday(), getToday())
        return arrayOf(oldest, newest)
    }
}
