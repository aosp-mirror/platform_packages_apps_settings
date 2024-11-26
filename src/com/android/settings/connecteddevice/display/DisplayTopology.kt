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

package com.android.settings.connecteddevice.display

import com.android.settings.R

import android.content.Context

import androidx.preference.Preference

const val PREFERENCE_KEY = "display_topology_preference"

/**
 * DisplayTopologyPreference allows the user to change the display topology
 * when there is one or more extended display attached.
 */
class DisplayTopologyPreference(context : Context) : Preference(context) {
    init {
        layoutResource = R.layout.display_topology_preference

        // Prevent highlight when hovering with mouse.
        isSelectable = false

        key = PREFERENCE_KEY
    }
}
