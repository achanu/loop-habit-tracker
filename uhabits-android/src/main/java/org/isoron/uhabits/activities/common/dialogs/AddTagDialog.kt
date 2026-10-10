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
import org.isoron.uhabits.utils.dp
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Filter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.Habit

/** A small tappable tag chip, as used in the detail view and dialogs. */
fun tagChipView(context: Context, text: String, onClick: () -> Unit = {}): TextView {
    val view = TextView(context)
    view.text = text
    view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
    view.setBackgroundResource(R.drawable.bg_tag_chip)
    val pad = context.dp(8f)
    view.setPadding(pad, pad / 2, pad, pad / 2)
    view.setOnClickListener { onClick() }
    return view
}

/** Wraps its children into rows; used to lay out tag chips. */
private class TagFlowLayout(context: Context) : ViewGroup(context) {
    private val spacing = context.dp(8f)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maxWidth = MeasureSpec.getSize(widthMeasureSpec)
        var x = paddingLeft
        var y = paddingTop
        var rowHeight = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
            if (x > paddingLeft && x + child.measuredWidth > maxWidth - paddingRight) {
                x = paddingLeft
                y += rowHeight + spacing
                rowHeight = 0
            }
            x += child.measuredWidth + spacing
            rowHeight = maxOf(rowHeight, child.measuredHeight)
        }
        setMeasuredDimension(maxWidth, y + rowHeight + paddingBottom)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var x = paddingLeft
        var y = paddingTop
        var rowHeight = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (x > paddingLeft && x + child.measuredWidth > measuredWidth - paddingRight) {
                x = paddingLeft
                y += rowHeight + spacing
                rowHeight = 0
            }
            child.layout(x, y, x + child.measuredWidth, y + child.measuredHeight)
            x += child.measuredWidth + spacing
            rowHeight = maxOf(rowHeight, child.measuredHeight)
        }
    }
}

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
 * Shows a dialog that lets the user select existing tags from a cloud or type
 * new ones, and passes all chosen tags to [onConfirm].
 * // ponytail: the cloud is not height-capped; wrap it in a ScrollView if tag
 * // counts ever outgrow the dialog.
 */
fun showAddTagDialog(
    context: Context,
    existingTags: List<String>,
    onConfirm: (List<String>) -> Unit
) {
    val input = AutoCompleteTextView(context)
    input.hint = context.getString(R.string.tags_example)
    input.setAdapter(TagSuggestionAdapter(context, existingTags))

    val pad = context.dp(20f)
    val container = LinearLayout(context)
    container.orientation = LinearLayout.VERTICAL
    container.setPadding(pad, pad / 2, pad, 0)
    val selected = mutableSetOf<String>()
    if (existingTags.isNotEmpty()) {
        val cloud = TagFlowLayout(context)
        for (tag in existingTags) {
            val chip = tagChipView(context, tag)
            chip.setOnClickListener {
                chip.isSelected = !chip.isSelected
                if (chip.isSelected) selected.add(tag) else selected.remove(tag)
            }
            cloud.addView(chip)
        }
        container.addView(cloud, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        input.setPadding(0, context.dp(8f), 0, 0)
    }
    container.addView(
        input,
        LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    )
    AlertDialog.Builder(context)
        .setTitle(R.string.add_tag)
        .setView(container)
        .setPositiveButton(R.string.save) { _, _ ->
            val picked = (selected + Habit.parseTags(input.text.toString())).toList()
            if (picked.isNotEmpty()) onConfirm(picked)
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
}


private const val MATCH_PARENT = LinearLayout.LayoutParams.MATCH_PARENT
private const val WRAP_CONTENT = LinearLayout.LayoutParams.WRAP_CONTENT
