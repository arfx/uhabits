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
import org.isoron.uhabits.core.models.HabitList

/**
 * Assigns ordinary habits to a group, moves them between groups, or removes
 * them from their current group when [targetGroupId] is null.
 */
data class SetGroupMembersCommand(
    val habitList: HabitList,
    val childIds: List<Long>,
    val targetGroupId: Long?
) : Command {
    override fun run() {
        require(childIds.isNotEmpty()) { "At least one habit must be selected" }
        val targetGroup = targetGroupId?.let { id ->
            habitList.getById(id)?.also {
                require(it.isGroup) { "Target must be a Group Habit" }
            } ?: throw IllegalArgumentException("Group $id does not exist")
        }
        val children = childIds.distinct().map { id ->
            habitList.getById(id) ?: throw IllegalArgumentException("Habit $id does not exist")
        }
        require(children.none { it.isGroup }) { "Group Habits cannot contain other groups" }

        val affectedGroupIds = children.mapNotNull { it.groupId }.toMutableSet()
        targetGroup?.id?.let { affectedGroupIds.add(it) }
        children.forEach { it.groupId = targetGroup?.id }
        habitList.update(children)
        GroupHabitCalculator.recomputeAffectedGroups(habitList, affectedGroupIds)
    }
}
