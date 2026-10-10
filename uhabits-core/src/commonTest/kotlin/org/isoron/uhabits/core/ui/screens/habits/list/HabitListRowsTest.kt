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

import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Entry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HabitListRowsTest : BaseUnitTest() {
    private fun habit(tags: List<String>) =
        modelFactory.buildHabit().apply { this.tags = tags }

    /** One symbol per row: "?", "·" (untagged habit) or the tag. */
    private fun summary(rows: List<HabitListRow>) = rows.map {
        when {
            it.isHeader -> it.tag ?: "?"
            it.habit!!.tags.isEmpty() -> "·"
            else -> it.tag!!
        }
    }

    @Test
    fun test_emptyList() {
        assertEquals(0, buildHabitListRows(emptyList(), emptySet()).size)
    }

    @Test
    fun test_noTags_noUnnamedGroup() {
        val rows = buildHabitListRows(listOf(habit(listOf("work"))), emptySet())
        assertEquals(listOf("work", "work"), summary(rows))
    }

    @Test
    fun test_ungroupedAndGrouped() {
        val a = habit(emptyList())
        val b = habit(listOf("work"))
        val c = habit(listOf("work", "health"))
        val rows = buildHabitListRows(listOf(a, b, c), emptySet())
        assertEquals(listOf("work", "work", "work", "health", "health", "?", "·"), summary(rows))
        assertTrue(rows[0].isHeader)
        assertFalse(rows[1].isHeader)
    }

    @Test
    fun test_collapsed() {
        val a = habit(listOf("b", "a"))
        val b = habit(listOf("a"))
        val rows = buildHabitListRows(listOf(a, b), setOf("b"))
        assertEquals(listOf("b", "a", "a", "a"), summary(rows))
    }

    @Test
    fun test_collapsedUnnamedGroup() {
        val a = habit(emptyList())
        val b = habit(listOf("work"))
        val rows = buildHabitListRows(listOf(a, b), setOf(UNGROUPED_KEY))
        assertEquals(listOf("work", "work", "?"), summary(rows))
    }

    @Test
    fun test_groupOrderByName() {
        val a = habit(listOf("zoo"))
        val b = habit(listOf("Alpha"))
        val rows = buildHabitListRows(listOf(a, b), emptySet(), GroupOrder.NAME)
        assertEquals(listOf("Alpha", "Alpha", "zoo", "zoo"), summary(rows))
    }

    @Test
    fun test_groupOrderAppearance() {
        val a = habit(listOf("zoo"))
        val b = habit(listOf("Alpha"))
        val rows = buildHabitListRows(listOf(a, b), emptySet(), GroupOrder.APPEARANCE)
        assertEquals(listOf("zoo", "zoo", "Alpha", "Alpha"), summary(rows))
    }

    @Test
    fun test_groupOrderByCompletion() {
        val done = habit(listOf("done")).apply {
            originalEntries.add(Entry(getToday(), Entry.YES_MANUAL))
            recompute()
        }
        val pending = habit(listOf("pending"))
        val rows = buildHabitListRows(listOf(pending, done), emptySet(), GroupOrder.COMPLETION)
        assertEquals(listOf("done", "done", "pending", "pending"), summary(rows))
    }

    @Test
    fun test_headerCounts() {
        val done = habit(listOf("work")).apply {
            originalEntries.add(Entry(getToday(), Entry.YES_MANUAL))
            recompute()
        }
        val pending = habit(listOf("work"))
        val untagged = habit(emptyList())
        val rows = buildHabitListRows(listOf(done, pending, untagged), emptySet())
        val header = rows.first { it.isHeader && it.tag == "work" }
        assertEquals(2, header.members)
        assertEquals(1, header.completed)
        val ungroupedHeader = rows.first { it.isHeader && it.tag == null }
        assertEquals(1, ungroupedHeader.members)
        assertEquals(0, ungroupedHeader.completed)
    }
}
