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
import org.isoron.uhabits.core.models.Frequency
import org.isoron.uhabits.core.models.Habit
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EditHabitCommandTest : BaseUnitTest() {
    private lateinit var command: EditHabitCommand
    private lateinit var habit: Habit
    private lateinit var modified: Habit
    private lateinit var today: LocalDate

    @BeforeTest
    override fun setUp() {
        super.setUp()
        habit = fixtures.createShortHabit()
        habit.name = "original"
        habit.frequency = Frequency.DAILY
        habit.recompute()
        habitList.add(habit)
        modified = fixtures.createEmptyHabit()
        modified.copyFrom(habit)
        modified.name = "modified"
        habitList.add(modified)
        today = getToday()
    }

    @Test
    fun testExecuteWithConvertedEntriesPreservesHistoricalNotes() {
        habit.originalEntries.clear()
        habit.originalEntries.add(org.isoron.uhabits.core.models.Entry(today, org.isoron.uhabits.core.models.Entry.YES_MANUAL, "morning run"))
        habit.originalEntries.add(org.isoron.uhabits.core.models.Entry(today.minus(1), org.isoron.uhabits.core.models.Entry.NO, "rest day"))
        modified.type = org.isoron.uhabits.core.models.HabitType.NUMERICAL

        val converted = org.isoron.uhabits.core.models.HabitEntryConverter.booleanToNumerical(
            habit.originalEntries.getKnown(),
            yesValue = 2500
        )
        EditHabitCommand(
            habitList,
            habit.id!!,
            modified,
            convertedEntries = converted
        ).run()

        assertEquals(2500, habit.originalEntries.get(today).value)
        assertEquals("morning run", habit.originalEntries.get(today).notes)
        assertEquals(0, habit.originalEntries.get(today.minus(1)).value)
        assertEquals("rest day", habit.originalEntries.get(today.minus(1)).notes)
    }


    @Test
    fun testTypeConversionPreservesNoGoalConfiguration() {
        habit.hasGoal = false
        habit.originalEntries.clear()
        habit.originalEntries.add(org.isoron.uhabits.core.models.Entry(today, org.isoron.uhabits.core.models.Entry.YES_MANUAL, "activity"))
        modified.copyFrom(habit)
        modified.type = org.isoron.uhabits.core.models.HabitType.NUMERICAL

        val converted = org.isoron.uhabits.core.models.HabitEntryConverter.booleanToNumerical(
            habit.originalEntries.getKnown(),
            yesValue = 1000
        )
        EditHabitCommand(
            habitList,
            habit.id!!,
            modified,
            convertedEntries = converted
        ).run()

        assertEquals(false, habit.hasGoal)
        assertEquals(1000, habit.originalEntries.get(today).value)
        assertEquals("activity", habit.originalEntries.get(today).notes)
    }

    @Test
    fun testTypeConversionPersistsComputedAutomaticSuccesses() {
        habit.originalEntries.clear()
        habit.frequency = Frequency(1, 3)
        habit.originalEntries.add(
            org.isoron.uhabits.core.models.Entry(today, org.isoron.uhabits.core.models.Entry.YES_MANUAL, "latest")
        )
        habit.originalEntries.add(
            org.isoron.uhabits.core.models.Entry(today.minus(3), org.isoron.uhabits.core.models.Entry.YES_MANUAL, "earlier")
        )
        habit.recompute()

        val automaticDate = today.minus(1)
        assertEquals(
            org.isoron.uhabits.core.models.Entry.YES_AUTO,
            habit.computedEntries.get(automaticDate).value
        )

        modified.copyFrom(habit)
        modified.type = org.isoron.uhabits.core.models.HabitType.NUMERICAL
        val converted = org.isoron.uhabits.core.models.HabitEntryConverter.booleanToNumerical(
            habit.originalEntries.getKnown(),
            habit.computedEntries.getKnown(),
            1750
        )

        EditHabitCommand(
            habitList,
            habit.id!!,
            modified,
            convertedEntries = converted
        ).run()

        assertEquals(1750, habit.originalEntries.get(automaticDate).value)
        assertEquals(1750, habit.originalEntries.get(today).value)
        assertEquals("latest", habit.originalEntries.get(today).notes)
        assertEquals(1750, habit.originalEntries.get(today.minus(3)).value)
        assertEquals("earlier", habit.originalEntries.get(today.minus(3)).notes)
    }

    @Test
    fun testExecute() {
        command = EditHabitCommand(habitList, habit.id!!, modified)
        val originalScore = habit.scores[today].value
        assertEquals("original", habit.name)
        command.run()
        assertEquals("modified", habit.name)
        assertEquals(originalScore, habit.scores[today].value)
    }
}
