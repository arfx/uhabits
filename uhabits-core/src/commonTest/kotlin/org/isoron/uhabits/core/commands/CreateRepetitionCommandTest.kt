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

import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.Frequency
import org.isoron.uhabits.core.models.NumericalHabitType
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateRepetitionCommandTest : BaseUnitTest() {
    private lateinit var command: CreateRepetitionCommand
    private lateinit var habit: Habit
    private lateinit var today: LocalDate

    @BeforeTest
    override fun setUp() {
        super.setUp()
        habit = fixtures.createShortHabit()
        habitList.add(habit)
        today = getToday()
        command = CreateRepetitionCommand(habitList, habit, today, 100, "")
    }

    @Test
    fun testNumericalGoalCompletionUpdatesGroupThroughRepetitionCommand() {
        val group = modelFactory.buildHabit().apply {
            name = "Group"
            isGroup = true
            hasGoal = false
        }
        habitList.add(group)

        val numericalHabit = fixtures.createShortHabit().apply {
            name = "Distance"
            type = HabitType.NUMERICAL
            hasGoal = true
            frequency = Frequency.DAILY
            targetType = NumericalHabitType.AT_LEAST
            targetValue = 100.0
            groupId = group.id
            originalEntries.clear()
            recompute()
        }
        habitList.add(numericalHabit)

        CreateRepetitionCommand(
            habitList,
            numericalHabit,
            today,
            150000,
            ""
        ).run()

        assertTrue(numericalHabit.computedEntries.get(today).completed)
        assertTrue(group.isActiveOn(today))
    }

    @Test
    fun testExecute() {
        val entries = habit.originalEntries
        var entry = entries.get(today)
        assertEquals(Entry.YES_MANUAL, entry.value)
        command.run()
        entry = entries.get(today)
        assertEquals(100, entry.value.toLong())
    }
}
