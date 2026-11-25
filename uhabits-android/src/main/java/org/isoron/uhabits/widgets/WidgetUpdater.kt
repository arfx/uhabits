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

package org.isoron.uhabits.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import me.tatarka.inject.annotations.Inject
import org.isoron.platform.time.DateUtils
import org.isoron.uhabits.core.commands.Command
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.commands.CreateRepetitionCommand
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.preferences.WidgetPreferences
import org.isoron.uhabits.core.tasks.TaskRunner
import org.isoron.uhabits.inject.AppContext
import org.isoron.uhabits.intents.IntentScheduler

/**
 * A WidgetUpdater listens to the commands being executed by the application and
 * updates the home-screen widgets accordingly.
 */
@Inject
class WidgetUpdater(
    @AppContext private val context: Context,
    private val commandRunner: CommandRunner,
    private val taskRunner: TaskRunner,
    private val widgetPrefs: WidgetPreferences,
    private val intentScheduler: IntentScheduler,
    private val preferences: Preferences
) : CommandRunner.Listener {

    override fun onCommandFinished(command: Command) {
        if (command is CreateRepetitionCommand) {
            updateWidgets(command.habit.id, command.habit.groupId)
        } else {
            updateWidgets()
        }
    }

    /**
     * Instructs the updater to start listening to commands. If any relevant
     * commands are executed after this method is called, the corresponding
     * widgets will get updated.
     */
    fun startListening() {
        commandRunner.addListener(this)
    }

    /**
     * Instructs the updater to stop listening to commands. Every command
     * executed after this method is called will be ignored by the updater.
     */
    fun stopListening() {
        commandRunner.removeListener(this)
    }

    fun scheduleStartDayWidgetUpdate() {
        val timestamp = DateUtils.getStartOfTomorrowWithOffset(preferences.midnightDelayHours, 0)
        intentScheduler.scheduleWidgetUpdate(timestamp)
    }

    fun updateWidgets(modifiedHabitId: Long?) {
        updateWidgets(modifiedHabitId, null)
    }

    /**
     * Updates widgets showing the changed habit or its parent group.
     * Group widgets store the group ID, while repetition commands refer to
     * the child habit that was changed.
     */
    fun updateWidgets(modifiedHabitId: Long?, parentGroupId: Long?) {
        taskRunner.execute {
            updateWidgets(modifiedHabitId, parentGroupId, CheckmarkWidgetProvider::class.java)
            updateWidgets(modifiedHabitId, parentGroupId, HistoryWidgetProvider::class.java)
            updateWidgets(modifiedHabitId, parentGroupId, ScoreWidgetProvider::class.java)
            updateWidgets(modifiedHabitId, parentGroupId, StreakWidgetProvider::class.java)
            updateWidgets(modifiedHabitId, parentGroupId, FrequencyWidgetProvider::class.java)
            updateWidgets(modifiedHabitId, parentGroupId, TargetWidgetProvider::class.java)
        }
    }

    private fun updateWidgets(
        modifiedHabitId: Long?,
        parentGroupId: Long?,
        providerClass: Class<*>
    ) {
        val widgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(
            ComponentName(context, providerClass)
        )

        val modifiedWidgetIds = when (modifiedHabitId) {
            null -> widgetIds.toList()
            else -> widgetIds.filter { w ->
                val selectedHabitIds = widgetPrefs.getHabitIdsFromWidgetId(w)
                selectedHabitIds.contains(modifiedHabitId) ||
                    (parentGroupId != null && selectedHabitIds.contains(parentGroupId))
            }
        }

        context.sendBroadcast(
            Intent(context, providerClass).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, modifiedWidgetIds.toIntArray())
            }
        )
    }

    fun updateWidgets() {
        updateWidgets(null)
    }
}
