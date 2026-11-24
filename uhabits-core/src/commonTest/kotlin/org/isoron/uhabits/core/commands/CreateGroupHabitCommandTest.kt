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

import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Entry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateGroupHabitCommandTest : BaseUnitTest() {
    @Test
    fun testMovingChildRecomputesPreviousGroup() {
        val today = getToday()
        val remainingChild = fixtures.createShortHabit().apply {
            originalEntries.add(Entry(today.minus(2), Entry.YES_MANUAL))
            recompute()
        }
        val movingChild = fixtures.createShortHabit().apply {
            originalEntries.add(Entry(today, Entry.YES_MANUAL))
            recompute()
        }
        val newSibling = fixtures.createShortHabit()
        val oldGroup = modelFactory.buildHabit().apply {
            name = "Old group"
            isGroup = true
            hasGoal = false
        }
        habitList.add(oldGroup)
        listOf(remainingChild, movingChild).forEach {
            it.groupId = oldGroup.id
            habitList.add(it)
        }
        GroupHabitCalculator.recompute(oldGroup, listOf(remainingChild, movingChild))
        habitList.update(oldGroup)
        habitList.add(newSibling)

        val newGroup = modelFactory.buildHabit().apply {
            name = "New group"
            isGroup = true
            hasGoal = false
        }
        CreateGroupHabitCommand(
            modelFactory,
            habitList,
            newGroup,
            listOf(movingChild.id!!, newSibling.id!!)
        ).run()

        assertEquals(Entry.UNKNOWN, oldGroup.originalEntries.get(today).value)
        assertEquals(Entry.YES_MANUAL, oldGroup.originalEntries.get(today.minus(2)).value)
        assertEquals(Entry.YES_MANUAL, newGroup.originalEntries.get(today).value)
        assertEquals(newGroup.id, movingChild.groupId)
        assertEquals(oldGroup.id, remainingChild.groupId)
    }

    @Test
    fun testCreatesGroupAndPreservesChildHistory() {
        val today = getToday()
        val first = fixtures.createShortHabit().apply {
            name = "Reading"
            originalEntries.add(Entry(today, Entry.YES_MANUAL))
            recompute()
        }
        val second = fixtures.createShortHabit().apply {
            name = "Drawing"
            originalEntries.add(Entry(today.minus(1), Entry.YES_MANUAL))
            recompute()
        }
        habitList.add(first)
        habitList.add(second)

        val model = modelFactory.buildHabit().apply {
            name = "Creative"
            isGroup = true
            hasGoal = false
            groupGoalEnabled = true
        }
        CreateGroupHabitCommand(
            modelFactory,
            habitList,
            model,
            listOf(first.id!!, second.id!!)
        ).run()

        val group = habitList.filter { it.isGroup }.single()
        assertEquals(group.id, first.groupId)
        assertEquals(group.id, second.groupId)
        assertEquals(Entry.YES_MANUAL, group.originalEntries.get(today).value)
        assertEquals(Entry.YES_MANUAL, group.originalEntries.get(today.minus(1)).value)
        assertEquals(Entry.YES_MANUAL, first.originalEntries.get(today).value)
        assertTrue(group.groupGoalEnabled)
        assertEquals(false, group.hasGoal)
    }
}
