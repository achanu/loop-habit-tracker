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
package org.isoron.uhabits.core.ui.screens.habits.list

import org.isoron.uhabits.core.models.Habit

/**
 * One row of the habit list: either a group header ([habit] is null) or a
 * habit card ([tag] is null for ungrouped habits).
 */
class HabitListRow(val habit: Habit?, val tag: String?) {
    val isHeader: Boolean
        get() = habit == null
}

/**
 * Expands an ordered list of habits into display rows: ungrouped habits
 * first, then one collapsible section per tag, in order of first appearance.
 * A habit with several tags appears once per tag.
 * // ponytail: multi-tag habits duplicate their stable id in the adapter; if
 * // that misbehaves (animations, drag-and-drop), restrict to first tag.
 */
fun buildHabitListRows(
    habits: List<Habit>,
    collapsed: Set<String>
): List<HabitListRow> {
    val rows = mutableListOf<HabitListRow>()
    for (habit in habits) {
        if (habit.tags.isEmpty()) rows.add(HabitListRow(habit, null))
    }
    val tags = mutableListOf<String>()
    for (habit in habits) {
        for (tag in habit.tags) {
            if (tag !in tags) tags.add(tag)
        }
    }
    for (tag in tags) {
        rows.add(HabitListRow(null, tag))
        if (tag !in collapsed) {
            for (habit in habits) {
                if (tag in habit.tags) rows.add(HabitListRow(habit, tag))
            }
        }
    }
    return rows
}
