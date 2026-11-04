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

package org.isoron.uhabits.activities.habits.edit

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.content.res.Resources
import android.os.Bundle
import android.text.Html
import android.text.InputType
import android.text.Spanned
import android.text.format.DateFormat
import android.view.View
import android.widget.ArrayAdapter
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.android.datetimepicker.time.RadialPickerLayout
import com.android.datetimepicker.time.TimePickerDialog
import org.isoron.platform.gui.toInt
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.AndroidThemeSwitcher
import org.isoron.uhabits.activities.common.dialogs.ColorPickerDialogFactory
import org.isoron.uhabits.activities.common.dialogs.FrequencyPickerDialog
import org.isoron.uhabits.activities.common.dialogs.WeekdayPickerDialog
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.commands.CreateHabitCommand
import org.isoron.uhabits.core.commands.EditHabitCommand
import org.isoron.uhabits.core.models.Frequency
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.HabitEntryConverter
import org.isoron.uhabits.core.models.NumericalHabitType
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.models.Reminder
import org.isoron.uhabits.core.models.WeekdayList
import kotlin.math.roundToInt
import org.isoron.uhabits.databinding.ActivityEditHabitBinding
import org.isoron.uhabits.utils.applyBottomInset
import org.isoron.uhabits.utils.applyRootViewInsets
import org.isoron.uhabits.utils.applyToolbarInsets
import org.isoron.uhabits.utils.dismissCurrentAndShow
import org.isoron.uhabits.utils.formatTime
import org.isoron.uhabits.utils.toFormattedString

fun formatFrequency(freqNum: Int, freqDen: Int, resources: Resources) = when {
    freqNum == 1 && (freqDen == 30 || freqDen == 31) -> resources.getString(R.string.every_month)
    freqDen == 30 || freqDen == 31 -> resources.getString(R.string.x_times_per_month, freqNum)
    freqNum == 1 && freqDen == 1 -> resources.getString(R.string.every_day)
    freqNum == 1 && freqDen == 7 -> resources.getString(R.string.every_week)
    freqNum == 1 && freqDen > 1 -> resources.getString(R.string.every_x_days, freqDen)
    freqDen == 7 -> resources.getString(R.string.x_times_per_week, freqNum)
    else -> resources.getString(R.string.x_times_per_y_days, freqNum, freqDen)
}

class EditHabitActivity : AppCompatActivity() {

    private lateinit var themeSwitcher: AndroidThemeSwitcher
    private lateinit var binding: ActivityEditHabitBinding
    private lateinit var commandRunner: CommandRunner

    var habitId = -1L
    lateinit var habitType: HabitType
    var unit = ""
    var color = PaletteColor(11)
    var androidColor = 0
    var freqNum = 1
    var freqDen = 1
    var reminderHour = -1
    var reminderMin = -1
    var reminderDays: WeekdayList = WeekdayList.EVERY_DAY
    var targetType = NumericalHabitType.AT_LEAST
    private var frequencyConfigured = true
    private var conversionYesValue: Int? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)

        val component = (application as HabitsApplication).component
        themeSwitcher = AndroidThemeSwitcher(this, component.preferences)
        themeSwitcher.apply()

        binding = ActivityEditHabitBinding.inflate(layoutInflater)
        binding.root.applyRootViewInsets()
        binding.root.applyBottomInset()
        binding.toolbar.applyToolbarInsets()
        setContentView(binding.root)

        if (intent.hasExtra("habitId")) {
            binding.toolbar.title = getString(R.string.edit_habit)
            habitId = intent.getLongExtra("habitId", -1)
            val habit = component.habitList.getById(habitId)!!
            habitType = habit.type
            color = habit.color
            freqNum = habit.frequency.numerator
            freqDen = habit.frequency.denominator
            targetType = habit.targetType
            habit.reminder?.let {
                reminderHour = it.hour
                reminderMin = it.minute
                reminderDays = it.days
            }
            binding.nameInput.setText(habit.name)
            binding.questionInput.setText(habit.question)
            binding.notesInput.setText(habit.description)
            binding.categoryInput.setText(habit.category)
            binding.unitInput.setText(habit.unit)
            binding.targetInput.setText(habit.targetValue.toString())
        } else {
            habitType = HabitType.fromInt(intent.getIntExtra("habitType", HabitType.YES_NO.value))
        }

        if (state != null) {
            habitId = state.getLong("habitId")
            habitType = HabitType.fromInt(state.getInt("habitType"))
            color = PaletteColor(state.getInt("paletteColor"))
            freqNum = state.getInt("freqNum")
            freqDen = state.getInt("freqDen")
            reminderHour = state.getInt("reminderHour")
            reminderMin = state.getInt("reminderMin")
            reminderDays = WeekdayList(state.getInt("reminderDays"))
            frequencyConfigured = state.getBoolean("frequencyConfigured", true)
            conversionYesValue = state.getInt("conversionYesValue", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
        }

        updateColors()

        binding.habitTypeOuterBox.visibility = if (habitId >= 0) View.VISIBLE else View.GONE
        updateHabitTypeControls()
        populateHabitType()

        binding.habitTypePicker.setOnClickListener {
            showHabitTypePicker()
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.elevation = 10.0f

        val colorPickerDialogFactory = ColorPickerDialogFactory(this)
        binding.colorButton.setOnClickListener {
            val picker = colorPickerDialogFactory.create(color, themeSwitcher.currentTheme)
            picker.setListener { paletteColor ->
                this.color = paletteColor
                updateColors()
            }
            picker.dismissCurrentAndShow(supportFragmentManager, "colorPicker")
        }

        populateFrequency()
        binding.booleanFrequencyPicker.setOnClickListener {
            val picker = FrequencyPickerDialog(freqNum, freqDen)
            picker.onFrequencyPicked = { num, den ->
                freqNum = num
                freqDen = den
                frequencyConfigured = true
                binding.booleanFrequencyPicker.error = null
                populateFrequency()
            }
            picker.dismissCurrentAndShow(supportFragmentManager, "frequencyPicker")
        }

        populateTargetType()
        binding.targetTypePicker.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_item)
            arrayAdapter.add(getString(R.string.target_type_at_least))
            arrayAdapter.add(getString(R.string.target_type_at_most))
            builder.setAdapter(arrayAdapter) { dialog, which ->
                targetType = when (which) {
                    0 -> NumericalHabitType.AT_LEAST
                    else -> NumericalHabitType.AT_MOST
                }
                populateTargetType()
                dialog.dismiss()
            }
            val dialog = builder.create()
            dialog.dismissCurrentAndShow()
        }

        binding.numericalFrequencyPicker.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_item)
            arrayAdapter.add(getString(R.string.every_day))
            arrayAdapter.add(getString(R.string.every_week))
            arrayAdapter.add(getString(R.string.every_month))
            builder.setAdapter(arrayAdapter) { dialog, which ->
                freqDen = when (which) {
                    1 -> 7
                    2 -> 30
                    else -> 1
                }
                frequencyConfigured = true
                binding.numericalFrequencyPicker.error = null
                populateFrequency()
                dialog.dismiss()
            }
            builder.show()
        }

        populateReminder()
        binding.reminderTimePicker.setOnClickListener {
            val currentHour = if (reminderHour >= 0) reminderHour else 8
            val currentMin = if (reminderMin >= 0) reminderMin else 0
            val is24HourMode = DateFormat.is24HourFormat(this)
            val dialog = TimePickerDialog.newInstance(
                object : TimePickerDialog.OnTimeSetListener {
                    override fun onTimeSet(view: RadialPickerLayout?, hourOfDay: Int, minute: Int) {
                        reminderHour = hourOfDay
                        reminderMin = minute
                        populateReminder()
                    }

                    override fun onTimeCleared(view: RadialPickerLayout?) {
                        reminderHour = -1
                        reminderMin = -1
                        reminderDays = WeekdayList.EVERY_DAY
                        populateReminder()
                    }
                },
                currentHour,
                currentMin,
                is24HourMode,
                androidColor
            )
            dialog.dismissCurrentAndShow(supportFragmentManager, "timePicker")
        }

        binding.reminderDatePicker.setOnClickListener {
            val dialog = WeekdayPickerDialog()

            dialog.setListener { days: WeekdayList ->
                reminderDays = days
                if (reminderDays.isEmpty) reminderDays = WeekdayList.EVERY_DAY
                populateReminder()
            }
            dialog.setSelectedDays(reminderDays)
            dialog.dismissCurrentAndShow(supportFragmentManager, "dayPicker")
        }

        binding.buttonSave.setOnClickListener {
            if (validate()) save()
        }

        for (fragment in supportFragmentManager.fragments) {
            (fragment as DialogFragment).dismiss()
        }
    }

    private fun save() {
        val component = (application as HabitsApplication).component
        val habit = component.modelFactory.buildHabit()

        var original: Habit? = null
        if (habitId >= 0) {
            original = component.habitList.getById(habitId)!!
            habit.copyFrom(original)
        }

        habit.name = binding.nameInput.text.trim().toString()
        habit.question = binding.questionInput.text.trim().toString()
        habit.description = binding.notesInput.text.trim().toString()
        habit.category = binding.categoryInput.text.trim().toString()
        habit.color = color
        if (reminderHour >= 0) {
            habit.reminder = Reminder(reminderHour, reminderMin, reminderDays)
        } else {
            habit.reminder = null
        }

        habit.frequency = Frequency(freqNum, freqDen)
        if (habitType == HabitType.NUMERICAL) {
            habit.targetValue = binding.targetInput.text.toString().toDouble()
            habit.targetType = targetType
            habit.unit = binding.unitInput.text.trim().toString()
        } else {
            habit.targetValue = 0.0
            habit.unit = ""
        }
        habit.type = habitType

        val convertedEntries = if (original != null && original.type != habitType) {
            when {
                original.type == HabitType.YES_NO && habitType == HabitType.NUMERICAL ->
                    HabitEntryConverter.booleanToNumerical(
                        original.originalEntries.getKnown(),
                        conversionYesValue ?: 1000
                    )
                original.type == HabitType.NUMERICAL && habitType == HabitType.YES_NO ->
                    HabitEntryConverter.numericalToBoolean(original.originalEntries.getKnown())
                else -> null
            }
        } else {
            null
        }

        val command = if (habitId >= 0) {
            EditHabitCommand(
                component.habitList,
                habitId,
                habit,
                convertedEntries
            )
        } else {
            CreateHabitCommand(
                component.modelFactory,
                component.habitList,
                habit
            )
        }
        component.commandRunner.run(command)
        finish()
    }

    private fun validate(): Boolean {
        var isValid = true
        if (binding.nameInput.text.isEmpty()) {
            binding.nameInput.error = getFormattedValidationError(R.string.validation_cannot_be_blank)
            isValid = false
        }
        if (!frequencyConfigured) {
            val picker = if (habitType == HabitType.NUMERICAL) {
                binding.numericalFrequencyPicker
            } else {
                binding.booleanFrequencyPicker
            }
            picker.error = getString(R.string.validation_goal_frequency_required)
            isValid = false
        }
        if (habitType == HabitType.NUMERICAL) {
            if (binding.targetInput.text.isEmpty()) {
                binding.targetInput.error = getString(R.string.validation_cannot_be_blank)
                isValid = false
            }
        }
        return isValid
    }

    private fun populateReminder() {
        if (reminderHour < 0) {
            binding.reminderTimePicker.text = getString(R.string.reminder_off)
            binding.reminderDatePicker.visibility = View.GONE
            binding.reminderDivider.visibility = View.GONE
        } else {
            val time = formatTime(this, reminderHour, reminderMin)
            binding.reminderTimePicker.text = time
            binding.reminderDatePicker.visibility = View.VISIBLE
            binding.reminderDivider.visibility = View.VISIBLE
            binding.reminderDatePicker.text = reminderDays.toFormattedString(this)
        }
    }

    @SuppressLint("StringFormatMatches")
    private fun populateFrequency() {
        binding.booleanFrequencyPicker.text = formatFrequency(freqNum, freqDen, resources)
        binding.numericalFrequencyPicker.text = when (freqDen) {
            1 -> getString(R.string.every_day)
            7 -> getString(R.string.every_week)
            30 -> getString(R.string.every_month)
            else -> "$freqNum/$freqDen"
        }
    }

    private fun populateTargetType() {
        binding.targetTypePicker.text = when (targetType) {
            NumericalHabitType.AT_MOST -> getString(R.string.target_type_at_most)
            else -> getString(R.string.target_type_at_least)
        }
    }

    private fun populateHabitType() {
        binding.habitTypePicker.text = when (habitType) {
            HabitType.YES_NO -> getString(R.string.yes_or_no)
            HabitType.NUMERICAL -> getString(R.string.measurable)
        }
    }

    private fun updateHabitTypeControls() {
        val isNumerical = habitType == HabitType.NUMERICAL
        binding.unitOuterBox.visibility = if (isNumerical) View.VISIBLE else View.GONE
        binding.targetOuterBox.visibility = if (isNumerical) View.VISIBLE else View.GONE
        binding.targetTypeOuterBox.visibility = if (isNumerical) View.VISIBLE else View.GONE
        binding.frequencyOuterBox.visibility = if (isNumerical) View.GONE else View.VISIBLE
        binding.nameInput.hint = if (isNumerical) {
            getString(R.string.measurable_short_example)
        } else {
            getString(R.string.yes_or_no_short_example)
        }
        binding.questionInput.hint = if (isNumerical) {
            getString(R.string.measurable_question_example)
        } else {
            getString(R.string.example_question_boolean)
        }
    }

    private fun showHabitTypePicker() {
        val options = arrayOf(getString(R.string.yes_or_no), getString(R.string.measurable))
        AlertDialog.Builder(this)
            .setTitle(R.string.habit_type)
            .setItems(options) { dialog, which ->
                dialog.dismiss()
                val selectedType = if (which == 0) HabitType.YES_NO else HabitType.NUMERICAL
                if (selectedType == habitType) return@setItems
                if (selectedType == HabitType.NUMERICAL) {
                    showBooleanToNumericalDialog()
                } else {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.convert_to_yes_no)
                        .setMessage(R.string.conversion_numerical_to_boolean_message)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.convert) { _, _ ->
                            changeHabitType(HabitType.YES_NO)
                        }
                        .show()
                }
            }
            .show()
    }

    private fun showBooleanToNumericalDialog() {
        val valueInput = android.widget.EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = getString(R.string.conversion_yes_value_hint)
            setText("1")
            selectAll()
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.convert_to_numerical)
            .setMessage(R.string.conversion_boolean_to_numerical_message)
            .setView(valueInput)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.convert, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = valueInput.text.toString().toDoubleOrNull()
                if (value == null || value < 0.0) {
                    valueInput.error = getString(R.string.validation_conversion_value_required)
                } else {
                    conversionYesValue = (value * 1000.0).roundToInt()
                    changeHabitType(HabitType.NUMERICAL)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun changeHabitType(newType: HabitType) {
        if (newType == habitType) return
        habitType = newType
        freqNum = 1
        freqDen = 1
        frequencyConfigured = false
        binding.booleanFrequencyPicker.error = null
        binding.numericalFrequencyPicker.error = null
        binding.targetInput.error = null
        if (newType == HabitType.NUMERICAL) {
            targetType = NumericalHabitType.AT_LEAST
            binding.targetInput.text.clear()
            binding.unitInput.text.clear()
        } else {
            binding.targetInput.text.clear()
            binding.unitInput.text.clear()
        }
        updateHabitTypeControls()
        populateHabitType()
        populateFrequency()
        populateTargetType()
    }

    private fun updateColors() {
        androidColor = themeSwitcher.currentTheme.color(color).toInt()
        binding.colorButton.backgroundTintList = ColorStateList.valueOf(androidColor)
        if (!themeSwitcher.isNightMode) {
            window.statusBarColor = androidColor
            binding.toolbar.setBackgroundColor(androidColor)
        }
    }

    private fun getFormattedValidationError(@StringRes resId: Int): Spanned {
        val html = "<font color=#FFFFFF>${getString(resId)}</font>"
        return Html.fromHtml(html)
    }

    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)
        with(state) {
            putLong("habitId", habitId)
            putInt("habitType", habitType.value)
            putInt("paletteColor", color.paletteIndex)
            putInt("androidColor", androidColor)
            putInt("freqNum", freqNum)
            putInt("freqDen", freqDen)
            putInt("reminderHour", reminderHour)
            putInt("reminderMin", reminderMin)
            putInt("reminderDays", reminderDays.toInteger())
            putBoolean("frequencyConfigured", frequencyConfigured)
            putInt("conversionYesValue", conversionYesValue ?: Int.MIN_VALUE)
        }
    }
}
