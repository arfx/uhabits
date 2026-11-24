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
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.uhabits.activities.habits.list.views

import android.content.Context
import android.graphics.PointF
import android.graphics.text.LineBreaker.BREAK_STRATEGY_BALANCED
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import me.tatarka.inject.annotations.Inject
import org.isoron.platform.gui.toInt
import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getToday
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.views.RingView
import org.isoron.uhabits.core.ui.screens.habits.list.HabitCardState
import org.isoron.uhabits.inject.ActivityContext
import org.isoron.uhabits.utils.currentTheme
import org.isoron.uhabits.utils.dp
import org.isoron.uhabits.utils.sres

data class HabitCardViewState(
    val record: HabitCardState,
    val selected: Boolean,
    val buttonCount: Int
)

class HabitCardView(
    @ActivityContext context: Context,
    checkmarkPanelFactory: CheckmarkPanelViewFactory,
    numberPanelFactory: NumberPanelViewFactory,
    private val controller: HabitCardListView.Controller
) : FrameLayout(context) {

    var checkmarkPanel: CheckmarkPanelView
    private var numberPanel: NumberPanelView
    private var innerFrame: LinearLayout
    private var label: TextView
    private var scoreRing: RingView
    private var noGoalMarker: TextView
    private var scoreRingContainer: FrameLayout

    private var dataOffset = 0
    private var state: HabitCardViewState? = null

    fun setState(newState: HabitCardViewState) {
        state = newState
        val r = newState.record
        val c = getActiveColor(r)
        label.apply {
            text = when {
                r.isGroup && r.groupGoalEnabled && r.isExpanded -> "▾  ${r.name}  (${r.groupGoalProgress}/${r.groupGoalTarget})"
                r.isGroup && r.groupGoalEnabled -> "▸  ${r.name}  (${r.groupGoalProgress}/${r.groupGoalTarget})"
                r.isGroup && r.isExpanded -> "▾  ${r.name}"
                r.isGroup -> "▸  ${r.name}"
                r.isGroupChild -> "    ${r.name}"
                else -> r.name
            }
            setTextColor(c)
        }
        scoreRing.visibility = if (r.hasGoal) View.VISIBLE else View.GONE
        noGoalMarker.visibility = if (r.hasGoal) View.GONE else View.VISIBLE
        noGoalMarker.setTextColor(c)
        if (r.hasGoal) {
            scoreRing.apply {
                setPercentage(r.score.toFloat())
                setPrecision(1.0f / 16)
                setColor(c)
            }
        }
        checkmarkPanel.visibility = when (r.isNumerical) {
            true -> View.GONE
            false -> View.VISIBLE
        }
        numberPanel.visibility = when (r.isNumerical) {
            true -> View.VISIBLE
            false -> View.GONE
        }
        checkmarkPanel.setState(CheckmarkPanelState(r.checkmarks, c, r.notes, newState.buttonCount))
        numberPanel.setState(
            NumberPanelState(
                r.checkmarks.map { it / 1000.0 },
                r.completed,
                c,
                r.unit,
                r.notes,
                newState.buttonCount
            )
        )
        setSelected(newState.selected)
    }

    fun setDataOffset(offset: Int) {
        dataOffset = offset
        checkmarkPanel.setDataOffset(offset)
        numberPanel.setDataOffset(offset)
    }

    private fun getActiveColor(record: HabitCardState): Int {
        return when (record.archived) {
            true -> sres.getColor(R.attr.contrast60)
            false -> currentTheme().color(record.color).toInt()
        }
    }

    init {
        val ringSize = dp(15f).toInt()
        val sideMargin = dp(8f).toInt()
        scoreRing = RingView(context).apply {
            setThickness(dp(3f))
        }
        noGoalMarker = TextView(context).apply {
            text = "●"
            textSize = 11f
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        scoreRingContainer = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(ringSize, ringSize).apply {
                setMargins(sideMargin, 0, sideMargin, 0)
                gravity = Gravity.CENTER
            }
            addView(scoreRing, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(noGoalMarker, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }

        label = TextView(context).apply {
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            if (SDK_INT >= Build.VERSION_CODES.Q) {
                breakStrategy = BREAK_STRATEGY_BALANCED
            }
        }

        checkmarkPanel = checkmarkPanelFactory.create().apply {
            onToggle = { date, value, notes ->
                triggerRipple(date)
                val location = getAbsoluteButtonLocation(date)
                state?.record?.id?.let { habitId ->
                    controller.onToggle(
                        habitId,
                        date,
                        value,
                        notes,
                        location.x,
                        location.y
                    )
                }
            }
            onEdit = { date ->
                triggerRipple(date)
                val location = getAbsoluteButtonLocation(date)
                state?.record?.id?.let { habitId ->
                    controller.onEdit(habitId, date, location.x, location.y)
                }
            }
        }

        numberPanel = numberPanelFactory.create().apply {
            visibility = GONE
            onEdit = { date ->
                triggerRipple(date)
                val location = getAbsoluteButtonLocation(date)
                state?.record?.id?.let { habitId ->
                    controller.onEdit(habitId, date, location.x, location.y)
                }
            }
        }

        innerFrame = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            elevation = dp(1f)

            addView(scoreRingContainer)
            addView(label)
            addView(checkmarkPanel)
            addView(numberPanel)

            setOnTouchListener { v, event ->
                v.background.setHotspot(event.x, event.y)
                false
            }
        }

        clipToPadding = false
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        val margin = dp(3f).toInt()
        setPadding(margin, 0, margin, margin)
        addView(innerFrame)
    }

    override fun setSelected(isSelected: Boolean) {
        super.setSelected(isSelected)
        updateBackground(isSelected)
    }

    fun triggerRipple(date: LocalDate) {
        val location = getRelativeButtonLocation(date)
        triggerRipple(location.x, location.y)
    }

    private fun getRelativeButtonLocation(date: LocalDate): PointF {
        val today = getToday()
        val offset = date.daysUntil(today) - dataOffset
        val panel = when (state?.record?.isNumerical == true) {
            true -> numberPanel
            false -> checkmarkPanel
        }
        val button = panel.buttons[offset]
        val y = button.height / 2.0f
        val x = panel.x + button.x + (button.width / 2).toFloat()
        return PointF(x, y)
    }

    private fun getAbsoluteButtonLocation(date: LocalDate): PointF {
        val containerLocation = IntArray(2)
        this.getLocationInWindow(containerLocation)
        val relButtonLocation = getRelativeButtonLocation(date)
        val windowInsets = rootWindowInsets
        val xInset = windowInsets?.displayCutout?.safeInsetLeft ?: 0
        val yInset = if (SDK_INT <= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            windowInsets?.systemWindowInsetTop ?: 0
        } else {
            0
        }
        return PointF(
            containerLocation[0].toFloat() + relButtonLocation.x - xInset,
            containerLocation[1].toFloat() + relButtonLocation.y - yInset
        )
    }

    private fun triggerRipple(x: Float, y: Float) {
        val background = innerFrame.background
        background.setHotspot(x, y)
        background.state = intArrayOf(
            android.R.attr.state_pressed,
            android.R.attr.state_enabled
        )
        Handler().postDelayed({ background.state = intArrayOf() }, 25)
    }

    private fun updateBackground(isSelected: Boolean) {
        val background = when (isSelected) {
            true -> R.drawable.selected_box
            false -> R.drawable.ripple
        }
        innerFrame.setBackgroundResource(background)
    }

    companion object {
        fun (() -> Unit).delay(delayInMillis: Long) {
            Handler(Looper.getMainLooper()).postDelayed(this, delayInMillis)
        }
    }
}

class HabitCardViewHolder(itemView: HabitCardView) : RecyclerView.ViewHolder(itemView)

@Inject
class HabitCardViewFactory(
    @ActivityContext val context: Context,
    private val checkmarkPanelFactory: CheckmarkPanelViewFactory,
    private val numberPanelFactory: NumberPanelViewFactory,
    private val controller: Lazy<HabitCardListController>
) {
    fun create() = HabitCardView(context, checkmarkPanelFactory, numberPanelFactory, controller.value)
}
