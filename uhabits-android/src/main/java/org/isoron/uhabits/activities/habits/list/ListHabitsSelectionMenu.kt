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

package org.isoron.uhabits.activities.habits.list

import android.content.Context
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import org.isoron.uhabits.activities.common.dialogs.FrequencyPickerDialog
import org.isoron.uhabits.activities.habits.edit.formatFrequency
import org.isoron.uhabits.core.commands.CreateGroupHabitCommand
import org.isoron.uhabits.core.commands.SetGroupMembersCommand
import org.isoron.uhabits.core.commands.EditHabitCommand
import org.isoron.uhabits.core.models.Frequency
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.GroupGoalType
import org.isoron.uhabits.core.models.ModelFactory
import org.isoron.uhabits.intents.IntentFactory
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import me.tatarka.inject.annotations.Inject
import org.isoron.platform.time.getToday
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.list.views.HabitCardListAdapter
import org.isoron.uhabits.activities.habits.list.views.HabitCardListController
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.ui.NotificationTray
import org.isoron.uhabits.core.ui.screens.habits.list.ListHabitsSelectionMenuBehavior
import org.isoron.uhabits.inject.ActivityContext
import org.isoron.uhabits.inject.ActivityScope
import org.isoron.uhabits.utils.showMessage

@Inject
@ActivityScope
class ListHabitsSelectionMenu(
    @ActivityContext context: Context,
    private val listAdapter: HabitCardListAdapter,
    var commandRunner: CommandRunner,
    private val prefs: Preferences,
    private val behavior: ListHabitsSelectionMenuBehavior,
    private val listController: Lazy<HabitCardListController>,
    private val notificationTray: NotificationTray,
    private val habitList: HabitList,
    private val modelFactory: ModelFactory,
    private val intentFactory: IntentFactory
) : ActionMode.Callback {

    val activity = (context as AppCompatActivity)

    var activeActionMode: ActionMode? = null

    fun onSelectionStart() {
        activity.startSupportActionMode(this)
    }

    fun onSelectionChange() {
        activeActionMode?.invalidate()
    }

    fun onSelectionFinish() {
        activeActionMode?.finish()
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        activeActionMode = mode
        activity.menuInflater.inflate(R.menu.list_habits_selection, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val itemEdit = menu.findItem(R.id.action_edit_habit)
        val itemColor = menu.findItem(R.id.action_color)
        val itemArchive = menu.findItem(R.id.action_archive_habit)
        val itemUnarchive = menu.findItem(R.id.action_unarchive_habit)
        val itemNotify = menu.findItem(R.id.action_notify)
        val itemGroup = menu.findItem(R.id.action_create_group)
        val itemGroupInsights = menu.findItem(R.id.action_group_insights)
        val itemAddToGroup = menu.findItem(R.id.action_add_to_group)
        val itemRemoveFromGroup = menu.findItem(R.id.action_remove_from_group)
        val itemMoveToGroup = menu.findItem(R.id.action_move_to_group)
        val selectedHabits = listAdapter.getSelected()
        val selectedGroups = selectedHabits.filter { it.isGroup }
        val selectedChildren = selectedHabits.filterNot { it.isGroup }
        val selectedGroup = selectedGroups.singleOrNull()

        itemColor.isVisible = selectedGroups.isEmpty()
        itemEdit.isVisible = behavior.canEdit() && selectedHabits.size == 1
        itemArchive.isVisible = behavior.canArchive() && selectedGroups.isEmpty()
        itemUnarchive.isVisible = behavior.canUnarchive() && selectedGroups.isEmpty()
        itemNotify.isVisible = prefs.isDeveloper && selectedGroups.isEmpty()
        itemGroup.isVisible = selectedHabits.size >= 2 && selectedGroups.isEmpty()
        itemGroupInsights.isVisible = selectedGroup != null && selectedHabits.size == 1
        itemAddToGroup.isVisible = selectedGroup != null && selectedChildren.isNotEmpty()
        itemRemoveFromGroup.isVisible = selectedChildren.any { it.groupId != null }
        itemMoveToGroup.isVisible = selectedChildren.isNotEmpty() && habitList.any { it.isGroup }
        activeActionMode?.title = listAdapter.selected.size.toString()
        return true
    }


    private fun updateSelectedGroupMembership(targetGroupId: Long?) {
        val children = listAdapter.getSelected().filterNot { it.isGroup }
        if (children.isEmpty()) return
        commandRunner.run(
            SetGroupMembersCommand(
                habitList = habitList,
                childIds = children.mapNotNull { it.id },
                targetGroupId = targetGroupId
            )
        )
        activeActionMode?.finish()
    }

    private fun showMoveToGroupDialog() {
        val children = listAdapter.getSelected().filterNot { it.isGroup }
        if (children.isEmpty()) return
        val currentGroupIds = children.mapNotNull { it.groupId }.toSet()
        val groups = habitList.filter { it.isGroup && it.id !in currentGroupIds }
        if (groups.isEmpty()) {
            activity.showMessage(activity.getString(R.string.group_habit_no_other_group))
            return
        }
        AlertDialog.Builder(activity)
            .setTitle(R.string.group_habit_move_members)
            .setItems(groups.map { it.name }.toTypedArray()) { _, which ->
                groups[which].id?.let(::updateSelectedGroupMembership)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showCreateGroupDialog() {
        val children = listAdapter.getSelected().filterNot { it.isGroup }
        if (children.size < 2) return

        val nameInput = EditText(activity).apply {
            hint = activity.getString(R.string.group_habit_name_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setSingleLine(true)
        }
        val goalSwitch = SwitchCompat(activity).apply {
            text = activity.getString(R.string.group_habit_goal_enabled)
            isChecked = false
        }
        var groupGoalType = GroupGoalType.OR
        val groupRuleButton = android.widget.Button(activity).apply {
            text = activity.getString(R.string.group_goal_rule_or)
            setOnClickListener {
                val labels = arrayOf(
                    activity.getString(R.string.group_goal_rule_or),
                    activity.getString(R.string.group_goal_rule_and),
                    activity.getString(R.string.group_goal_rule_at_least)
                )
                AlertDialog.Builder(activity)
                    .setTitle(R.string.group_goal_rule)
                    .setItems(labels) { dialog, which ->
                        groupGoalType = when (which) {
                            1 -> GroupGoalType.AND
                            2 -> GroupGoalType.AT_LEAST
                            else -> GroupGoalType.OR
                        }
                        text = labels[which]
                        thresholdInput.visibility = if (groupGoalType == GroupGoalType.AT_LEAST) android.view.View.VISIBLE else android.view.View.GONE
                        dialog.dismiss()
                    }
                    .show()
            }
        }
        val thresholdInput = EditText(activity).apply {
            hint = activity.getString(R.string.group_goal_threshold_hint)
            inputType = InputType.TYPE_CLASS_NUMBER
            setText("1")
            visibility = android.view.View.GONE
        }
        val frequencyLabel = android.widget.TextView(activity).apply {
            text = formatFrequency(3, 7, activity.resources)
            setPadding(0, 12, 0, 12)
            isEnabled = false
        }
        var numerator = 3
        var denominator = 7
        val frequencyButton = android.widget.Button(activity).apply {
            text = activity.getString(R.string.group_habit_choose_frequency)
            isEnabled = false
            setOnClickListener {
                val picker = FrequencyPickerDialog(numerator, denominator)
                picker.onFrequencyPicked = { num, den ->
                    numerator = num
                    denominator = den
                    frequencyLabel.text = formatFrequency(numerator, denominator, activity.resources)
                }
                picker.show(activity.supportFragmentManager, "groupFrequency")
            }
        }
        goalSwitch.setOnCheckedChangeListener { _, checked ->
            frequencyLabel.isEnabled = checked
            frequencyButton.isEnabled = checked
        }

        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (20 * activity.resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, 0)
            addView(nameInput, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
            addView(goalSwitch)
            addView(groupRuleButton)
            addView(thresholdInput)
            addView(frequencyLabel)
            addView(frequencyButton)
        }

        AlertDialog.Builder(activity)
            .setTitle(R.string.group_habit_create_title)
            .setView(content)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isEmpty()) {
                    activity.showMessage(activity.getString(R.string.group_habit_name_required))
                    return@setPositiveButton
                }
                val group = modelFactory.buildHabit().apply {
                    this.name = name
                    this.color = children.first().color
                    this.isGroup = true
                    this.type = HabitType.YES_NO
                    this.hasGoal = false
                    this.groupGoalEnabled = goalSwitch.isChecked
                    this.groupGoalType = groupGoalType
                    this.groupGoalThreshold = thresholdInput.text.toString().toIntOrNull()?.coerceIn(1, children.size) ?: 1
                    this.frequency = Frequency(numerator, denominator)
                }
                commandRunner.run(
                    CreateGroupHabitCommand(
                        modelFactory,
                        habitList,
                        group,
                        children.mapNotNull { it.id }
                    )
                )
                activeActionMode?.finish()
            }
            .show()
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        listController.value.onSelectionFinished()
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_edit_habit -> {
                behavior.onEditHabits()
                return true
            }

            R.id.action_archive_habit -> {
                behavior.onArchiveHabits()
                return true
            }

            R.id.action_unarchive_habit -> {
                behavior.onUnarchiveHabits()
                return true
            }

            R.id.action_categorize -> {
                behavior.onChangeCategory()
                return true
            }

            R.id.action_delete -> {
                behavior.onDeleteHabits()
                return true
            }

            R.id.action_color -> {
                behavior.onChangeColor()
                return true
            }

            R.id.action_notify -> {
                for (h in listAdapter.getSelected())
                    notificationTray.show(h, getToday(), 0)
                return true
            }

            R.id.action_add_to_group -> {
                listAdapter.getSelected().firstOrNull { it.isGroup }?.id
                    ?.let(::updateSelectedGroupMembership)
                return true
            }

            R.id.action_remove_from_group -> {
                updateSelectedGroupMembership(null)
                return true
            }

            R.id.action_move_to_group -> {
                showMoveToGroupDialog()
                return true
            }

            R.id.action_create_group -> {
                showCreateGroupDialog()
                return true
            }


            R.id.action_group_insights -> {
                listAdapter.getSelected().singleOrNull()?.takeIf { it.isGroup }?.id?.let { id ->
                    activity.startActivity(intentFactory.startShowHabitActivity(activity, id))
                }
                return true
            }

            else -> return false
        }
    }
}
