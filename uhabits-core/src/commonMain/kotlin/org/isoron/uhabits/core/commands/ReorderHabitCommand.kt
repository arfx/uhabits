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
package org.isoron.uhabits.core.commands

import org.isoron.uhabits.core.models.GroupHabitCalculator
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList

sealed class ReorderTarget {
    data class Before(val ref: Habit) : ReorderTarget()

    data class After(val ref: Habit) : ReorderTarget()
}

data class ReorderHabitCommand(
    val habitList: HabitList,
    val habit: Habit,
    val target: ReorderTarget?,
    val category: String,
    val targetGroupId: Long? = habit.groupId
) : Command {
    override fun run() {
        val previousGroupId = habit.groupId
        val targetGroup = targetGroupId?.let { id ->
            habitList.getById(id)?.also {
                require(it.isGroup) { "Target must be a Group Habit" }
            } ?: throw IllegalArgumentException("Group $id does not exist")
        }

        require(!habit.isGroup || targetGroupId == null) {
            "Group Habits cannot be nested"
        }

        habit.category = category
        habit.groupId = targetGroup?.id
        when (target) {
            is ReorderTarget.Before -> habitList.moveBefore(habit, target.ref)
            is ReorderTarget.After -> habitList.moveAfter(habit, target.ref)
            null -> {}
        }
        habitList.update(listOf(habit))
        if (previousGroupId != habit.groupId) {
            GroupHabitCalculator.recomputeAffectedGroups(
                habitList,
                setOf(previousGroupId, habit.groupId)
            )
        }
        habitList.resort()
    }
}
