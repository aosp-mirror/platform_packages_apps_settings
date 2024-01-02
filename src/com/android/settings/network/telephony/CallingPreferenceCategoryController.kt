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

package com.android.settings.network.telephony

import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.core.BasePreferenceController

/**
 * Preference controller for "Calling" category
 */
class CallingPreferenceCategoryController(context: Context, key: String) :
    BasePreferenceController(context, key) {

    private val visibleChildren = mutableSetOf<String>()
    private var preference: Preference? = null

    override fun getAvailabilityStatus() = AVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        // Not call super here, to avoid preference.isVisible changed unexpectedly
        preference = screen.findPreference(preferenceKey)
    }

    fun updateChildVisible(key: String, isVisible: Boolean) {
        if (isVisible) {
            visibleChildren.add(key)
        } else {
            visibleChildren.remove(key)
        }
        preference?.isVisible = visibleChildren.isNotEmpty()
    }
}
