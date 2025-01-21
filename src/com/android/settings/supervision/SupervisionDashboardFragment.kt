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

import android.app.settings.SettingsEnums
import android.content.Context
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment

/**
 * Fragment to display the Supervision settings landing page (Settings > Supervision).
 *
 * See [SupervisionDashboardScreen] for details on the page contents.
 *
 * This class extends [DashboardFragment] in order to support dynamic settings injection.
 */
class SupervisionDashboardFragment : DashboardFragment() {

    override fun getPreferenceScreenResId() = R.xml.placeholder_preference_screen

    override fun getMetricsCategory() = SettingsEnums.SUPERVISION_DASHBOARD

    override fun getLogTag() = TAG

    override fun getPreferenceScreenBindingKey(context: Context) = SupervisionDashboardScreen.KEY

    // TODO(b/383405598): redirect to Play Store if supervisor client is not
    // fully present.

    companion object {
        private const val TAG = "SupervisionDashboard"
    }
}
