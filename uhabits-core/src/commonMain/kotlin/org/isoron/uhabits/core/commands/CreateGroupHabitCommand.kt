/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
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
import org.isoron.uhabits.core.models.ModelFactory

/**
 * Creates a group and moves the selected ordinary habits into it.
 * The children retain their own goals and historical entries.
 */
data class CreateGroupHabitCommand(
    val modelFactory: ModelFactory,
    val habitList: HabitList,
    val model: Habit,
    val childIds: List<Long>
) : Command {
    override fun run() {
        require(model.isGroup) { "The supplied habit must be a Group Habit" }
        require(childIds.distinct().size >= 2) { "A Group Habit needs at least two children" }

        val children = childIds.distinct().map { id ->
            habitList.getById(id) ?: throw IllegalArgumentException("Habit $id does not exist")
        }
        require(children.none { it.isGroup }) { "Group Habits cannot contain other groups" }

        val affectedGroupIds = children.mapNotNull { it.groupId }.toSet()
        val group = modelFactory.buildHabit()
        group.copyFrom(model)
        group.isGroup = true
        group.groupId = null
        group.type = org.isoron.uhabits.core.models.HabitType.YES_NO
        group.unit = ""
        group.targetValue = 0.0
        habitList.add(group)

        children.forEach { child ->
            child.groupId = group.id
        }
        habitList.update(children)
        GroupHabitCalculator.recompute(group, children)
        habitList.update(group)
        GroupHabitCalculator.recomputeAffectedGroups(habitList, affectedGroupIds)
    }
}
