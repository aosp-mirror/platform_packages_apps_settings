/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.connecteddevice.display

import android.content.Context
import android.provider.Settings

import androidx.preference.SwitchPreferenceCompat

import com.android.settings.R

const val MIRROR_PREFERENCE_KEY = "mirror_builtin_display"

/**
 * A switch preference which is backed by the MIRROR_BUILT_IN_DISPLAY global setting.
 */
class MirrorPreference(context: Context): SwitchPreferenceCompat(context) {
    init {
        setTitle(R.string.external_display_mirroring_title)

        key = MIRROR_PREFERENCE_KEY
        isPersistent = false
    }

    override fun onAttached() {
        super.onAttached()
        setChecked(0 != Settings.Global.getInt(
                context.contentResolver, Settings.Secure.MIRROR_BUILT_IN_DISPLAY, 0))
    }

    override fun onClick() {
        super.onClick()
        Settings.Global.putInt(
                context.contentResolver, Settings.Secure.MIRROR_BUILT_IN_DISPLAY,
                if (isChecked()) 1 else 0)
    }
}
