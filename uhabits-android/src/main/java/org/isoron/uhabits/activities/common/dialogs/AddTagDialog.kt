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
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Filter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.Habit

private fun dp(context: Context, value: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics)
        .toInt()

/** A small tappable tag chip, as used in the detail view and dialogs. */
fun tagChipView(context: Context, text: String, onClick: () -> Unit): TextView {
    val view = TextView(context)
    view.text = text
    view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
    view.setBackgroundResource(R.drawable.bg_tag_chip)
    val pad = dp(context, 8f)
    view.setPadding(pad, pad / 2, pad, pad / 2)
    view.setOnClickListener { onClick() }
    return view
}

/** Wraps its children into rows; used to lay out tag chips. */
private class TagFlowLayout(context: Context) : ViewGroup(context) {
    private val spacing = dp(context, 8f)

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
 * Shows a dialog that lets the user type a tag or tap one from a cloud of
 * [existingTags], and passes the entered tag to [onConfirm].
 * // ponytail: the cloud is not height-capped; wrap it in a ScrollView if tag
 * // counts ever outgrow the dialog.
 */
fun showAddTagDialog(
    context: Context,
    existingTags: List<String>,
    onConfirm: (String) -> Unit
) {
    val input = AutoCompleteTextView(context)
    input.hint = context.getString(R.string.tags_example)
    input.setAdapter(TagSuggestionAdapter(context, existingTags))

    val pad = dp(context, 20f)
    val container = LinearLayout(context)
    container.orientation = LinearLayout.VERTICAL
    container.setPadding(pad, pad / 2, pad, 0)
    if (existingTags.isNotEmpty()) {
        val cloud = TagFlowLayout(context)
        for (tag in existingTags) {
            cloud.addView(
                tagChipView(context, tag) {
                    input.setText(Habit.appendTagInput(input.text.toString(), tag))
                    input.setSelection(input.text.length)
                }
            )
        }
        container.addView(cloud, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        input.setPadding(0, dp(context, 8f), 0, 0)
    }
    container.addView(
        input,
        LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
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

private const val MATCH_PARENT = LinearLayout.LayoutParams.MATCH_PARENT
private const val WRAP_CONTENT = LinearLayout.LayoutParams.WRAP_CONTENT
