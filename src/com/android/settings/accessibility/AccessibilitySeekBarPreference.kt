/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.accessibility

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import com.android.settings.widget.LabeledSeekBarPreference

/**
 * Add a custom AccessibilitySeekBarPreference with tool tip window for font size and display size.
 */
open class AccessibilitySeekBarPreference(context: Context, attrs: AttributeSet?) :
    LabeledSeekBarPreference(context, attrs) {

    var needsQSTooltipReshow = false
    private var tooltipWindow: AccessibilityQuickSettingsTooltipWindow? = null

    override fun onSaveInstanceState(): Parcelable {
        val state = Bundle()
        state.putParcelable(null, super.onSaveInstanceState())
        if (needsQSTooltipReshow || tooltipWindow?.isShowing == true) {
            state.putBoolean(KEY_SAVED_QS_TOOLTIP_RESHOW, /* value= */ true)
        }
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val bundle = state as Bundle
        super.onRestoreInstanceState(bundle.getParcelable(null, Parcelable::class.java))
        if (bundle.containsKey(KEY_SAVED_QS_TOOLTIP_RESHOW)) {
            needsQSTooltipReshow = bundle.getBoolean(KEY_SAVED_QS_TOOLTIP_RESHOW)
        }
    }

    /** To generate a tooltip window and return it. */
    fun createTooltipWindow(): AccessibilityQuickSettingsTooltipWindow =
        AccessibilityQuickSettingsTooltipWindow(context).also { tooltipWindow = it }

    /** To dismiss the tooltip window. */
    fun dismissTooltip() {
        val tooltip = tooltipWindow
        if (tooltip?.isShowing == true) {
            tooltip.dismiss()
            tooltipWindow = null
        }
    }

    companion object {
        private const val KEY_SAVED_QS_TOOLTIP_RESHOW = "qs_tooltip_reshow"
    }
}
