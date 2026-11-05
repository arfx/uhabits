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
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 */
package org.isoron.uhabits.core.models

import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getToday
import kotlin.math.min

/**
 * Streaks shared by the habits in one category.
 *
 * A date is considered successful when at least one habit in the category
 * is completed on that date. This aggregation is deliberately independent
 * from each habit's [StreakList].
 */
class OverallStreakList {
    private val list = ArrayList<Streak>()

    fun recompute(category: String, habits: Iterable<Habit>) {
        list.clear()
        if (category.isBlank()) return

        val today = getToday()
        val completedDates = HashSet<LocalDate>()
        habits.asSequence()
            .filter { it.category == category }
            .flatMap { it.computedEntries.getKnown().asSequence() }
            .filter { it.completed && !it.date.isNewerThan(today) }
            .forEach { completedDates.add(it.date) }

        val dates = completedDates.sortedByDescending { it.daysSince2000 }
        if (dates.isEmpty()) return

        var start = dates.first()
        var end = dates.first()
        for (i in 1 until dates.size) {
            val current = dates[i]
            if (current == start.minus(1)) {
                start = current
            } else {
                list.add(Streak(start, end))
                start = current
                end = current
            }
        }
        list.add(Streak(start, end))
    }

    fun getBest(limit: Int): List<Streak> {
        list.sortWith { first, second -> second.compareLonger(first) }
        return list.take(min(list.size, limit))
            .sortedWith { first, second -> second.compareNewer(first) }
    }
}
