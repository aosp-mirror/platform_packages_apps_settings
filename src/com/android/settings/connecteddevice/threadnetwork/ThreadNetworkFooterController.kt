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

package com.android.settings.connecteddevice.threadnetwork

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.HelpUtils
import com.android.settingslib.widget.FooterPreference

/**
 * The footer preference controller for Thread settings in
 * "Connected devices > Connection preferences > Thread".
 */
class ThreadNetworkFooterController(
    context: Context,
    preferenceKey: String
) : BasePreferenceController(context, preferenceKey) {
    override fun getAvailabilityStatus(): Int {
        // The thread_network_settings screen won't be displayed and it doesn't matter if this
        // controller always return AVAILABLE
        return AVAILABLE
    }

    override fun displayPreference(screen: PreferenceScreen) {
        val footer: FooterPreference? = screen.findPreference(KEY_PREFERENCE_FOOTER)
        if (footer != null) {
            footer.setLearnMoreAction { _ -> openLocaleLearnMoreLink() }
            footer.setLearnMoreText(mContext.getString(R.string.thread_network_settings_learn_more))
        }
    }

    private fun openLocaleLearnMoreLink() {
        val intent = HelpUtils.getHelpIntent(
            mContext,
            mContext.getString(R.string.thread_network_settings_learn_more_link),
            mContext::class.java.name
        )
        if (intent != null) {
            mContext.startActivity(intent)
        } else {
            Log.w(TAG, "HelpIntent is null")
        }
    }

    companion object {
        private const val TAG = "ThreadNetworkSettings"
        private const val KEY_PREFERENCE_FOOTER = "thread_network_settings_footer"
    }
}
