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

package com.android.settings.deviceinfo.firmwareversion

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.os.UserHandle
import android.os.UserManager
import androidx.preference.Preference
import com.android.internal.app.PlatLogoActivity
import com.android.settings.R
import com.android.settings.Utils
import com.android.settingslib.RestrictedLockUtils
import com.android.settingslib.RestrictedLockUtilsInternal
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding

// LINT.IfChange
class FirmwareVersionDetailPreference :
    PreferenceMetadata,
    PreferenceSummaryProvider,
    PreferenceBinding,
    Preference.OnPreferenceClickListener {

    private val hits = LongArray(ACTIVITY_TRIGGER_COUNT)

    override val key: String
        get() = "os_firmware_version"

    override val title: Int
        get() = R.string.firmware_version

    override fun isIndexable(context: Context) = false

    override fun intent(context: Context): Intent? =
        Intent(Intent.ACTION_MAIN)
            .setClassName("android", PlatLogoActivity::class.java.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    override fun getSummary(context: Context): CharSequence? =
        Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isCopyingEnabled = true
        preference.onPreferenceClickListener = this
    }

    // return true swallows the click event, while return false will start the intent
    override fun onPreferenceClick(preference: Preference): Boolean {
        if (Utils.isMonkeyRunning()) return true

        // remove oldest hit and check whether there are 3 clicks within 500ms
        for (index in 1..<ACTIVITY_TRIGGER_COUNT) hits[index - 1] = hits[index]
        hits[ACTIVITY_TRIGGER_COUNT - 1] = SystemClock.uptimeMillis()
        if (hits[ACTIVITY_TRIGGER_COUNT - 1] - hits[0] > DELAY_TIMER_MILLIS) return true

        val context = preference.context
        val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
        if (userManager?.hasUserRestriction(UserManager.DISALLOW_FUN) != true) return false

        // Sorry, no fun for you!
        val myUserId = UserHandle.myUserId()
        val enforcedAdmin =
            RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                context,
                UserManager.DISALLOW_FUN,
                myUserId,
            ) ?: return true
        val disallowedBySystem =
            RestrictedLockUtilsInternal.hasBaseUserRestriction(
                context,
                UserManager.DISALLOW_FUN,
                myUserId,
            )
        if (!disallowedBySystem) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(context, enforcedAdmin)
        }
        return true
    }

    companion object {
        const val DELAY_TIMER_MILLIS = 500L
        const val ACTIVITY_TRIGGER_COUNT = 3
    }
}
// LINT.ThenChange(FirmwareVersionDetailPreferenceController.java)
