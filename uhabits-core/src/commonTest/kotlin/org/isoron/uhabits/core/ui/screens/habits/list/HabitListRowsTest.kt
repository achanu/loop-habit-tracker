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

import org.isoron.uhabits.core.BaseUnitTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HabitListRowsTest : BaseUnitTest() {
    private fun habit(tags: List<String>) =
        modelFactory.buildHabit().apply { this.tags = tags }

    @Test
    fun test_emptyList() {
        assertEquals(0, buildHabitListRows(emptyList(), emptySet()).size)
    }

    @Test
    fun test_ungroupedAndGrouped() {
        val a = habit(emptyList())
        val b = habit(listOf("work"))
        val c = habit(listOf("work", "health"))
        val rows = buildHabitListRows(listOf(a, b, c), emptySet())
        val summary = rows.map { it.tag ?: it.habit!!.tags.firstOrNull() ?: "ungrouped" }
        assertEquals(listOf("ungrouped", "work", "work", "work", "health", "health"), summary)
        assertTrue(rows[1].isHeader)
        assertFalse(rows[2].isHeader)
    }

    @Test
    fun test_collapsedAndOrderOfAppearance() {
        val a = habit(listOf("b", "a"))
        val b = habit(listOf("a"))
        val rows = buildHabitListRows(listOf(a, b), setOf("b"))
        val summary = rows.map { it.tag ?: "?" }
        assertEquals(listOf("b", "a", "a", "a"), summary)
    }
}
