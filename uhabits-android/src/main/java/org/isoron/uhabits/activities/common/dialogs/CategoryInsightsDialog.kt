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
package org.isoron.uhabits.activities.common.dialogs

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.OverallStreakList
import org.isoron.uhabits.core.preferences.Preferences

object CategoryInsightsDialog {
    fun show(
        activity: AppCompatActivity,
        category: String,
        habits: HabitList,
        preferences: Preferences,
        onSaved: () -> Unit = {}
    ) {
        val categoryHabits = habits.filter { it.category == category }
        val overall = OverallStreakList()
        overall.recompute(category, categoryHabits)

        val today = getToday()
        val periodStart = today.minus(29)
        val successfulDays = overall.getSuccessfulDayCount(periodStart, today)
        val successRate = overall.getSuccessRate(periodStart, today)
        val longestStreak = overall.getBest(1).firstOrNull()?.length ?: 0
        val latestStreak = overall.getAll().firstOrNull()
        val currentStreak = latestStreak?.takeIf { it.end >= today.minus(1) }?.length

        val padding = (24 * activity.resources.displayMetrics.density).toInt()
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding / 2, padding, 0)
        }
        content.addView(TextView(activity).apply {
            text = "Ein Tag zählt als erfolgreich, sobald mindestens ein Habit dieser Kategorie abgeschlossen ist."
            textSize = 14f
        })
        content.addView(TextView(activity).apply {
            text = "Aktuelle Serie: " + (currentStreak?.let { "$it Tage" } ?: "Keine aktive Serie")
            textSize = 16f
            setPadding(0, padding / 2, 0, 0)
        })
        content.addView(TextView(activity).apply {
            text = "Längste Serie: $longestStreak Tage"
            textSize = 16f
        })
        content.addView(TextView(activity).apply {
            text = "Erfolgreiche Tage (letzte 30 Tage): $successfulDays / 30"
            textSize = 16f
        })
        content.addView(TextView(activity).apply {
            text = "Erfolgsquote (letzte 30 Tage): ${successRate.toInt()} %"
            textSize = 16f
        })
        content.addView(TextView(activity).apply {
            text = "${categoryHabits.size} Habits in dieser Kategorie"
            textSize = 14f
            setPadding(0, padding / 3, 0, 0)
        })
        val goalToggle = CheckBox(activity).apply {
            text = "Overall Goal für diese Kategorie aktivieren"
            isChecked = preferences.isCategoryOverallGoalEnabled(category)
            setPadding(0, padding / 3, 0, 0)
        }
        content.addView(goalToggle, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        AlertDialog.Builder(activity)
            .setTitle(category)
            .setView(content)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                preferences.setCategoryOverallGoalEnabled(category, goalToggle.isChecked)
                onSaved()
            }
            .show()
    }
}
