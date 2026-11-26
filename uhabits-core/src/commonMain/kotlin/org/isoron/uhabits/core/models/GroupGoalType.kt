package org.isoron.uhabits.core.models

enum class GroupGoalType(val value: Int) {
    OR(0),
    AND(1),
    AT_LEAST(2);

    companion object {
        fun fromInt(value: Int): GroupGoalType =
            values().firstOrNull { it.value == value } ?: OR
    }
}
