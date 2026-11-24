/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isoron.uhabits.core.models

import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getFirstWeekday
import org.isoron.platform.time.getToday

/**
 * Builds the daily history of a Group Habit from its child habits.
 *
 * A group day succeeds when at least one child habit succeeds on that date.
 * Multiple successful children on the same date still produce exactly one
 * group entry, so group frequency goals count unique calendar days.
 */
object GroupHabitCalculator {

    fun recomputeAffectedGroups(habitList: HabitList, groupIds: Set<Long?>) {
        groupIds.filterNotNull().forEach { groupId ->
            val group = habitList.getById(groupId) ?: return@forEach
            if (!group.isGroup) return@forEach
            val children = habitList.filter { it.groupId == groupId && !it.isGroup }
            recompute(group, children)
            habitList.update(group)
            group.observable.notifyListeners()
        }
    }

    fun goalProgress(group: Habit, date: LocalDate = getToday()): Pair<Int, Int> {
        require(group.isGroup)
        if (!group.groupGoalEnabled) return 0 to 0
        val periodStart = when {
            group.frequency.denominator <= 1 -> date
            group.frequency.denominator <= 7 -> date.startOfWeek(getFirstWeekday())
            else -> date.startOfMonth()
        }
        val completedDays = group.originalEntries.getKnown().count {
            it.value == Entry.YES_MANUAL && it.date >= periodStart && it.date <= date
        }
        return completedDays to group.frequency.numerator
    }

    fun contributingColors(group: Habit, children: List<Habit>, date: LocalDate): List<PaletteColor> {
        require(group.isGroup)
        return children
            .filter { child -> child.groupId == group.id && isSuccessfulOn(child, date) }
            .map { it.color }
            .distinctBy { it.paletteIndex }
            .sortedBy { it.paletteIndex }
    }

    private fun isSuccessfulOn(child: Habit, date: LocalDate): Boolean {
        val original = child.originalEntries.get(date)
        if (original.value == Entry.UNKNOWN || original.value == Entry.SKIP) return false
        return when {
            child.hasGoal && child.isNumerical -> child.computedEntries.get(date).completed
            child.hasGoal -> original.value == Entry.YES_MANUAL &&
                child.computedEntries.get(date).completed
            child.isNumerical -> true
            else -> original.value == Entry.YES_MANUAL
        }
    }

    fun recompute(group: Habit, children: List<Habit>) {
        require(group.isGroup) { "Habit must be a Group Habit" }
        require(children.none { it.isGroup }) { "Group Habits cannot contain other groups" }
        require(children.all { it.groupId == group.id }) {
            "All children must belong to the supplied group"
        }

        val successfulDates = children
            .flatMap { child ->
                child.originalEntries.getKnown()
                    .filter { original -> isSuccessfulOn(child, original.date) }
                    .map { it.date }
            }
            .toSet()

        group.originalEntries.replaceAll(
            successfulDates.map { date -> Entry(date, Entry.YES_MANUAL) }
        )
        group.type = HabitType.YES_NO
        group.hasGoal = false
        group.unit = ""
        group.targetValue = 0.0
        group.recompute()
    }
}
