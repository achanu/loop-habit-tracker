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
 * habit card ([tag] is null for ungrouped habits). A header with both null is
 * the unnamed group of habits without tags.
 */
class HabitListRow(val habit: Habit?, val tag: String?) {
    val isHeader: Boolean
        get() = habit == null
}

/**
 * Key identifying the unnamed group of untagged habits in the collapsed
 * set. Tags cannot be empty, so this cannot collide with a real tag.
 */
const val UNGROUPED_KEY = "\u0000"

/** How the tag groups are ordered on the habit list. */
enum class GroupOrder { APPEARANCE, NAME, COMPLETION }

/**
 * Expands an ordered list of habits into display rows: one collapsible
 * section per tag, then the unnamed group of untagged habits at the end. A
 * habit with several tags appears once per tag.
 * // ponytail: multi-tag habits duplicate their stable id in the adapter; if
 * // that misbehaves (animations, drag-and-drop), restrict to first tag.
 */
fun buildHabitListRows(
    habits: List<Habit>,
    collapsed: Set<String>,
    groupOrder: GroupOrder = GroupOrder.APPEARANCE
): List<HabitListRow> {
    val rows = mutableListOf<HabitListRow>()
    val tags = mutableListOf<String>()
    for (habit in habits) {
        for (tag in habit.tags) {
            if (tag !in tags) tags.add(tag)
        }
    }
    when (groupOrder) {
        GroupOrder.NAME -> tags.sortBy { it.lowercase() }
        GroupOrder.COMPLETION -> tags.sortByDescending { tag ->
            // ponytail: O(tags x habits) recomputed per rebuild; fine for
            // hundreds of habits, index it if the list ever gets huge.
            val members = habits.filter { tag in it.tags }
            if (members.isEmpty()) 0.0 else members.count { it.isCompletedToday() }.toDouble() / members.size
        }
        GroupOrder.APPEARANCE -> {}
    }
    for (tag in tags) {
        rows.add(HabitListRow(null, tag))
        if (tag !in collapsed) {
            for (habit in habits) {
                if (tag in habit.tags) rows.add(HabitListRow(habit, tag))
            }
        }
    }
    val ungrouped = habits.filter { it.tags.isEmpty() }
    if (ungrouped.isNotEmpty()) {
        rows.add(HabitListRow(null, null))
        if (UNGROUPED_KEY !in collapsed) {
            for (habit in ungrouped) rows.add(HabitListRow(habit, null))
        }
    }
    return rows
}
