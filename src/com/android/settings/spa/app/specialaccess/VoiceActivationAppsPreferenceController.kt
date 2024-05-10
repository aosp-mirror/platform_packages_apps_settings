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

package com.android.settings.spa.app.specialaccess

import android.content.Context
import androidx.preference.Preference
import com.android.settings.core.BasePreferenceController
import com.android.settings.flags.Flags
import com.android.settings.spa.SpaActivity.Companion.startSpaActivity

class VoiceActivationAppsPreferenceController(context: Context, preferenceKey: String) :
        BasePreferenceController(context, preferenceKey) {
    override fun getAvailabilityStatus() =
        if (Flags.enableVoiceActivationAppsInSettings()) AVAILABLE
        else CONDITIONALLY_UNAVAILABLE

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == mPreferenceKey) {
            mContext.startSpaActivity(VoiceActivationAppsListProvider.getAppListRoute())
            return true
        }
        return false
    }
}