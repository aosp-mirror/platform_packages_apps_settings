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
package com.android.settings.supervision

import android.content.Context
import android.content.Intent
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceMetadata

/**
 * Setting on PIN Management screen (Settings > Supervision > Manage Pin) that invokes the flow to
 * update the existing device supervision PIN.
 */
class SupervisionChangePinPreference : PreferenceMetadata {
    override val key: String
        get() = "supervision_change_pin"

    override val title: Int
        get() = R.string.supervision_change_pin_preference_title

    override fun intent(context: Context): Intent? {
        // TODO(b/393450922): implement handling of change pin intent.
        return super.intent(context)
    }
}
