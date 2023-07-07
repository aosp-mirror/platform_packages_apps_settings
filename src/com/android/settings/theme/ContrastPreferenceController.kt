/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.settings.theme

import android.app.UiModeManager
import android.app.UiModeManager.ContrastUtils.CONTRAST_LEVEL_HIGH
import android.app.UiModeManager.ContrastUtils.CONTRAST_LEVEL_MEDIUM
import android.app.UiModeManager.ContrastUtils.toContrastLevel
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.text.TextUtils
import androidx.preference.Preference
import com.android.internal.annotations.VisibleForTesting
import com.android.settings.R
import com.android.settings.core.BasePreferenceController

/**
 * Controller that opens the contrast dialog and updates the text describing the current contrast
 */
class ContrastPreferenceController(
        private val context: Context,
        private val uiModeManager: UiModeManager) : BasePreferenceController(context, KEY) {

    companion object {
        @VisibleForTesting
        const val KEY = "contrast_preference"
    }

    override fun getAvailabilityStatus(): Int {
        return AVAILABLE
    }

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (TextUtils.equals(preference.key, preferenceKey)) {
            val intent = Intent(Intent.ACTION_SHOW_CONTRAST_DIALOG)
            context.startActivityAsUser(intent, UserHandle(UserHandle.USER_CURRENT))
            return true
        }
        return false
    }

    override fun getSummary(): CharSequence = getSummary(toContrastLevel(uiModeManager.contrast))

    @VisibleForTesting
    fun getSummary(contrast: Int): String {
        return when (contrast) {
            CONTRAST_LEVEL_HIGH -> context.getString(R.string.contrast_high)
            CONTRAST_LEVEL_MEDIUM -> context.getString(R.string.contrast_medium)
            else -> context.getString(R.string.contrast_standard)
        }
    }
}