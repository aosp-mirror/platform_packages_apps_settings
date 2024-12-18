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

package com.android.settings.network

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.Utils
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle

class InternetPreferenceControllerV2(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey) {

    private val repository by lazy { InternetPreferenceRepository(mContext) }
    private var preference: Preference? = null

    override fun getAvailabilityStatus() =
        if (mContext.resources.getBoolean(R.bool.config_show_internet_settings)) AVAILABLE
        else UNSUPPORTED_ON_DEVICE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        repository.displayInfoFlow().collectLatestWithLifecycle(viewLifecycleOwner) { displayInfo ->
            preference?.apply {
                summary = displayInfo.summary
                icon =
                    mContext.getDrawable(displayInfo.iconResId)?.apply {
                        setTintList(Utils.getColorAttr(mContext, android.R.attr.colorControlNormal))
                    }
            }
        }
    }
}
