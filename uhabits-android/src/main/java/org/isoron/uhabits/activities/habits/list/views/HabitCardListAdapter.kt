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
package org.isoron.uhabits.activities.habits.list.views

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import me.tatarka.inject.annotations.Inject
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.list.MAX_CHECKMARK_COUNT
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.HabitMatcher
import org.isoron.uhabits.core.models.ModelObservable
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.ui.screens.habits.list.GroupOrder
import org.isoron.uhabits.core.ui.screens.habits.list.HabitCardListCache
import org.isoron.uhabits.core.ui.screens.habits.list.HabitListRow
import org.isoron.uhabits.core.ui.screens.habits.list.ListHabitsMenuBehavior
import org.isoron.uhabits.core.ui.screens.habits.list.ListHabitsSelectionMenuBehavior
import org.isoron.uhabits.core.ui.screens.habits.list.UNGROUPED_KEY
import org.isoron.uhabits.core.ui.screens.habits.list.buildHabitListRows
import org.isoron.uhabits.core.utils.MidnightTimer
import org.isoron.uhabits.inject.ActivityScope
import java.util.LinkedList

class GroupHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tagView: TextView = itemView.findViewById(R.id.groupTag)
    val progressView: TextView = itemView.findViewById(R.id.groupProgress)
    val chevron: ImageView = itemView.findViewById(R.id.groupChevron)
}

/**
 * Provides data that backs a [HabitCardListView].
 *
 *
 * The data if fetched and cached by a [HabitCardListCache]. The cache keeps a
 * flat, position-indexed list of habits; this adapter expands it into display
 * rows — one collapsible header per tag, followed by its habits. All positions
 * arriving from the views (clicks, drags) are display positions and are
 * translated back to flat positions before reaching the cache.
 */
@Inject
@ActivityScope
class HabitCardListAdapter(
    private val cache: HabitCardListCache,
    private val preferences: Preferences,
    private val midnightTimer: MidnightTimer
) : RecyclerView.Adapter<RecyclerView.ViewHolder?>(),
    HabitCardListCache.Listener,
    MidnightTimer.MidnightListener,
    ListHabitsMenuBehavior.Adapter,
    ListHabitsSelectionMenuBehavior.Adapter {
    val observable: ModelObservable = ModelObservable()
    private var listView: HabitCardListView? = null
    val selected: LinkedList<Habit> = LinkedList()
    private var rows: List<HabitListRow> = emptyList()
    private val collapsed: MutableSet<String> =
        preferences.collapsedGroups.split(',').filter { it.isNotEmpty() }.toMutableSet()
    override fun atMidnight() {
        cache.refreshAllHabits()
    }

    fun cancelRefresh() {
        cache.cancelTasks()
    }

    fun hasNoHabit(): Boolean {
        return cache.hasNoHabit()
    }

    /**
     * Sets all items as not selected.
     */
    override fun clearSelection() {
        if (selected.isEmpty()) return

        selected.clear()
        notifyDataSetChanged()
        observable.notifyListeners()
    }

    override fun getSelected(): List<Habit> {
        return ArrayList(selected)
    }

    /**
     * Returns the habit that occupies a certain position on the list, or null
     * if the position holds a group header.
     *
     * @param position position of the item
     * @return the habit at given position or null if position is invalid
     */
    @Deprecated("")
    fun getItem(position: Int): Habit? {
        return rows.getOrNull(position)?.habit
    }

    private fun allHabits(): List<Habit> =
        (0 until cache.habitCount).mapNotNull { cache.getHabitByPosition(it) }

    private fun rebuild() {
        rows = buildHabitListRows(allHabits(), collapsed, groupOrder)
    }

    override var groupOrder: GroupOrder
        get() = runCatching { GroupOrder.valueOf(preferences.groupSort) }
            .getOrDefault(GroupOrder.APPEARANCE)
        set(value) {
            preferences.groupSort = value.name
            rebuild()
            notifyDataSetChanged()
        }

    private fun flatPositionOf(habit: Habit): Int {
        for (i in 0 until cache.habitCount) {
            if (cache.getHabitByPosition(i) == habit) return i
        }
        return -1
    }

    /**
     * Collapses or expands the group with the given tag.
     */
    fun toggleGroup(tag: String) {
        if (!collapsed.remove(tag)) collapsed.add(tag)
        preferences.collapsedGroups = collapsed.joinToString(",")
        rebuild()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return rows.size
    }

    override fun getItemId(position: Int): Long {
        val row = rows[position]
        // ponytail: a habit in multiple groups repeats its id; harmless while
        // structural changes go through notifyDataSetChanged.
        return if (row.isHeader) {
            -((row.tag ?: UNGROUPED_KEY).hashCode().toLong() + 1)
        } else {
            row.habit!!.id!!
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (rows[position].isHeader) TYPE_HEADER else TYPE_HABIT
    }

    /**
     * Returns whether list of selected items is empty.
     *
     * @return true if selection is empty, false otherwise
     */
    val isSelectionEmpty: Boolean
        get() = selected.isEmpty()
    val isSortable: Boolean
        get() = cache.primaryOrder == HabitList.Order.BY_POSITION

    /**
     * Notify the adapter that it has been attached to a ListView.
     */
    fun onAttached() {
        cache.onAttached()
        midnightTimer.addListener(this)
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        if (listView == null) return
        val row = rows[position]
        if (row.isHeader) {
            val header = holder as GroupHeaderViewHolder
            val key = row.tag ?: UNGROUPED_KEY
            header.tagView.text =
                row.tag ?: listView!!.context.getString(R.string.ungrouped)
            header.chevron.rotation = if (key in collapsed) 0f else 90f
            header.progressView.text = "${row.completed}/${row.members}"
            header.itemView.setOnClickListener { toggleGroup(key) }
            return
        }
        val habit = row.habit!!
        val cardHolder = holder as HabitCardViewHolder
        val score = cache.getScore(habit.id!!)
        val checkmarks = cache.getCheckmarks(habit.id!!)
        val notes = cache.getNotes(habit.id!!)
        val selected = selected.contains(habit)
        listView!!.bindCardView(cardHolder, habit, score, checkmarks, notes, selected)
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        if (holder.itemViewType == TYPE_HEADER) return
        listView!!.attachCardView(holder as HabitCardViewHolder)
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        if (holder.itemViewType == TYPE_HEADER) return
        listView!!.detachCardView(holder as HabitCardViewHolder)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            GroupHeaderViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.habit_group_header,
                    parent,
                    false
                )
            )
        } else {
            HabitCardViewHolder(listView!!.createHabitCardView())
        }
    }

    /**
     * Notify the adapter that it has been detached from a ListView.
     */
    fun onDetached() {
        cache.onDetached()
        midnightTimer.removeListener(this)
    }

    private fun onDataSetChanged() {
        rebuild()
        notifyDataSetChanged()
        observable.notifyListeners()
    }

    override fun onItemChanged(position: Int) {
        onDataSetChanged()
    }

    // Insertions and moves are coalesced: they stream in one by one during a
    // refresh, and rebuilding per notification made a full refresh O(n^2).
    // The single rebuild in onRefreshFinished covers them.
    override fun onItemInserted(position: Int) {}

    override fun onItemMoved(oldPosition: Int, newPosition: Int) {}

    override fun onItemRemoved(position: Int) {
        onDataSetChanged()
    }

    override fun onRefreshFinished() {
        onDataSetChanged()
    }

    /**
     * Removes a list of habits from the adapter.
     *
     *
     * Note that this only has effect on the adapter cache. The database is not
     * modified, and the change is lost when the cache is refreshed. This method
     * is useful for making the ListView more responsive: while we wait for the
     * database operation to finish, the cache can be modified to reflect the
     * changes immediately.
     *
     * @param selected list of habits to be removed
     */
    override fun performRemove(selected: List<Habit>) {
        for (habit in selected) cache.remove(habit.id!!)
    }

    /**
     * Changes the order of habits on the adapter.
     *
     *
     * Note that this only has effect on the adapter cache. The database is not
     * modified, and the change is lost when the cache is refreshed. This method
     * is useful for making the ListView more responsive: while we wait for the
     * database operation to finish, the cache can be modified to reflect the
     * changes immediately.
     *
     * @param from the display position of the habit that should be moved
     * @param to   the display position of the habit currently at the target
     */
    fun performReorder(from: Int, to: Int) {
        val fromHabit = rows.getOrNull(from)?.habit ?: return
        val toHabit = rows.getOrNull(to)?.habit ?: return
        val fromFlat = flatPositionOf(fromHabit)
        val toFlat = flatPositionOf(toHabit)
        if (fromFlat < 0 || toFlat < 0) return
        cache.reorder(fromFlat, toFlat)
        rebuild()
        notifyDataSetChanged()
    }

    override fun refresh() {
        cache.refreshAllHabits()
    }

    override fun setFilter(matcher: HabitMatcher) {
        cache.setFilter(matcher)
    }

    /**
     * Sets the HabitCardListView that this adapter will provide data for.
     *
     * This object will be used to generated new HabitCardViews, upon demand.
     *
     * @param listView the HabitCardListView associated with this adapter
     */
    fun setListView(listView: HabitCardListView?) {
        this.listView = listView
    }

    override var primaryOrder: HabitList.Order
        get() = cache.primaryOrder
        set(value) {
            cache.primaryOrder = value
            preferences.defaultPrimaryOrder = value
        }

    override var secondaryOrder: HabitList.Order
        get() = cache.secondaryOrder
        set(value) {
            cache.secondaryOrder = value
            preferences.defaultSecondaryOrder = value
        }

    /**
     * Selects or deselects the item at a given position.
     *
     * @param position the display position of the item to be toggled
     */
    fun toggleSelection(position: Int) {
        val h = getItem(position) ?: return
        val k = selected.indexOf(h)
        if (k < 0) selected.add(h) else selected.remove(h)
        notifyDataSetChanged()
    }

    init {
        cache.setListener(this)
        cache.setCheckmarkCount(
            MAX_CHECKMARK_COUNT
        )
        cache.secondaryOrder = preferences.defaultSecondaryOrder
        cache.primaryOrder = preferences.defaultPrimaryOrder
        setHasStableIds(true)
    }

    companion object {
        const val TYPE_HABIT = 0
        const val TYPE_HEADER = 1
    }
}
