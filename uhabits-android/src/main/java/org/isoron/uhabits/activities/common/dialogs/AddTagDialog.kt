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
package org.isoron.uhabits.activities.common.dialogs

import android.content.Context
import android.util.TypedValue
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Filter
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.Habit

/**
 * Suggests existing tags while the user types. Matches only the tag currently
 * being typed — the text after the last comma.
 */
class TagSuggestionAdapter(
    context: Context,
    private val all: List<String>
) : ArrayAdapter<String>(context, android.R.layout.select_dialog_item, ArrayList(all)) {
    var fullText = ""

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            fullText = constraint?.toString() ?: ""
            val query = Habit.currentTagInput(fullText)
            return FilterResults().apply {
                values = all.filter { it.startsWith(query, ignoreCase = true) && it != query }
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            clear()
            @Suppress("UNCHECKED_CAST")
            addAll(results.values as List<String>)
            notifyDataSetChanged()
        }
    }
}

/**
 * Shows a dialog that lets the user type a tag, with suggestions from
 * [existingTags], and passes the entered tag to [onConfirm].
 */
fun showAddTagDialog(
    context: Context,
    existingTags: List<String>,
    onConfirm: (String) -> Unit
) {
    val pad = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 20f, context.resources.displayMetrics
    ).toInt()
    val input = AutoCompleteTextView(context)
    input.hint = context.getString(R.string.tags_example)
    input.setAdapter(TagSuggestionAdapter(context, existingTags))
    val container = LinearLayout(context)
    container.setPadding(pad, pad / 2, pad, 0)
    container.addView(
        input,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    )
    AlertDialog.Builder(context)
        .setTitle(R.string.add_tag)
        .setView(container)
        .setPositiveButton(R.string.save) { _, _ ->
            val tag = input.text.trim().toString()
            if (tag.isNotEmpty()) onConfirm(tag)
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
}
