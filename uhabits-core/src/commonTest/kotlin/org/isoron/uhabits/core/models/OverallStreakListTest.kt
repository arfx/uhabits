package org.isoron.uhabits.core.models

import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.BaseUnitTest
import kotlin.test.Test
import kotlin.test.assertEquals

class OverallStreakListTest : BaseUnitTest() {
    @Test
    fun testAnyCompletedHabitKeepsCategoryStreakAlive() {
        val today = getToday()
        val first = fixtures.createEmptyHabit().apply {
            category = "Health"
            originalEntries.add(Entry(today.minus(1), Entry.YES_MANUAL))
            recompute()
        }
        val second = fixtures.createEmptyHabit().apply {
            category = "Health"
            originalEntries.add(Entry(today, Entry.YES_MANUAL))
            recompute()
        }

        val overall = OverallStreakList()
        overall.recompute("Health", listOf(first, second))

        assertEquals(1, overall.getBest(10).size)
        assertEquals(2, overall.getBest(10).first().length)
        assertEquals(1, first.streaks.getBest(10).first().length)
        assertEquals(1, second.streaks.getBest(10).first().length)
    }

    @Test
    fun testHabitsFromOtherCategoriesAreIgnored() {
        val today = getToday()
        val habit = fixtures.createEmptyHabit().apply {
            category = "Fitness"
            originalEntries.add(Entry(today, Entry.YES_MANUAL))
            recompute()
        }

        val overall = OverallStreakList()
        overall.recompute("Health", listOf(habit))

        assertEquals(0, overall.getBest(10).size)
    }

    @Test
    fun testBlankCategoryHasNoOverallStreak() {
        val today = getToday()
        val habit = fixtures.createEmptyHabit().apply {
            category = ""
            originalEntries.add(Entry(today, Entry.YES_MANUAL))
            recompute()
        }

        val overall = OverallStreakList()
        overall.recompute("", listOf(habit))

        assertEquals(0, overall.getBest(10).size)
    }
}
