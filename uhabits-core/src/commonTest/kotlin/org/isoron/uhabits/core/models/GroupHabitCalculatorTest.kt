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
package org.isoron.uhabits.core.models

import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.BaseUnitTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupHabitCalculatorTest : BaseUnitTest() {

    @BeforeTest
    override fun setUp() {
        super.setUp()
    }

    @Test
    fun testOrRuleAndUniqueDates() {
        val today = getToday()
        val group = modelFactory.buildHabit().apply {
            id = 100L
            isGroup = true
            hasGoal = false
        }
        val first = modelFactory.buildHabit().apply {
            groupId = group.id
            originalEntries.add(Entry(today, Entry.YES_MANUAL))
            originalEntries.add(Entry(today.minus(1), Entry.YES_MANUAL))
            recompute()
        }
        val second = modelFactory.buildHabit().apply {
            groupId = group.id
            originalEntries.add(Entry(today, Entry.YES_MANUAL))
            recompute()
        }

        GroupHabitCalculator.recompute(group, listOf(first, second))

        assertEquals(2, group.originalEntries.getKnown().size)
        assertEquals(Entry.YES_MANUAL, group.originalEntries.get(today).value)
        assertEquals(Entry.YES_MANUAL, group.originalEntries.get(today.minus(1)).value)
        assertTrue(group.isActiveToday())
    }

    @Test
    fun testWeeklyBooleanGoalOnlyContributesActualCompletionDates() {
        val today = getToday()
        val group = modelFactory.buildHabit().apply {
            id = 105L
            isGroup = true
            hasGoal = false
        }
        val child = modelFactory.buildHabit().apply {
            groupId = group.id
            hasGoal = true
            frequency = Frequency(3, 7)
            originalEntries.add(Entry(today, Entry.YES_MANUAL))
            recompute()
        }

        GroupHabitCalculator.recompute(group, listOf(child))

        assertEquals(listOf(today), group.originalEntries.getKnown().map { it.date })
        assertTrue(group.isActiveToday())
        assertFalse(group.isActiveOn(today.minus(1)))
    }

    @Test
    fun testNoGoalNumericalZeroCountsAsRecordedGroupDay() {
        val today = getToday()
        val group = modelFactory.buildHabit().apply {
            id = 101L
            isGroup = true
            hasGoal = false
        }
        val child = modelFactory.buildHabit().apply {
            groupId = group.id
            type = HabitType.NUMERICAL
            hasGoal = false
            originalEntries.add(Entry(today, 0))
            recompute()
        }

        GroupHabitCalculator.recompute(group, listOf(child))

        assertEquals(Entry.YES_MANUAL, group.originalEntries.get(today).value)
        assertTrue(group.isActiveToday())
    }

    @Test
    fun testNumericalChildWithGoalOnlyCountsCompletedPeriod() {
        val today = getToday()
        val group = modelFactory.buildHabit().apply {
            id = 102L
            isGroup = true
            hasGoal = false
        }
        val child = modelFactory.buildHabit().apply {
            groupId = group.id
            type = HabitType.NUMERICAL
            hasGoal = true
            frequency = Frequency.DAILY
            targetValue = 100.0
            originalEntries.add(Entry(today, 50000))
            recompute()
        }

        GroupHabitCalculator.recompute(group, listOf(child))

        assertFalse(group.isActiveToday())
        child.originalEntries.add(Entry(today, 150000))
        child.recompute()
        GroupHabitCalculator.recompute(group, listOf(child))
        assertTrue(group.isActiveToday())
    }

    @Test
    fun testIndependentWeeklyGoalDoesNotCreateDailyStreakDays() {
        val today = getToday()
        val group = modelFactory.buildHabit().apply {
            id = 104L
            isGroup = true
            hasGoal = false
            groupGoalEnabled = true
            frequency = Frequency(3, 7)
        }
        val child = modelFactory.buildHabit().apply {
            groupId = group.id
            originalEntries.add(Entry(today, Entry.YES_MANUAL))
            originalEntries.add(Entry(today.minus(1), Entry.YES_MANUAL))
            recompute()
        }

        GroupHabitCalculator.recompute(group, listOf(child))

        assertTrue(group.isActiveOn(today))
        assertTrue(group.isActiveOn(today.minus(1)))
        assertFalse(group.isActiveOn(today.minus(2)))
        assertEquals(2 to 3, GroupHabitCalculator.goalProgress(group, today))
    }

    @Test
    fun testRejectsChildrenFromAnotherGroup() {
        val group = modelFactory.buildHabit().apply {
            id = 103L
            isGroup = true
        }
        val child = modelFactory.buildHabit().apply { groupId = 999L }

        var rejected = false
        try {
            GroupHabitCalculator.recompute(group, listOf(child))
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }
}
