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

package com.android.settings.activityembedding

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.android.settings.SettingsActivity
import com.android.settings.Utils
import com.android.settings.homepage.DeepLinkHomepageActivityInternal
import com.android.settings.homepage.SettingsHomepageActivity
import com.android.settings.password.PasswordUtils
import com.android.settingslib.spaprivileged.framework.common.userManager

object EmbeddedDeepLinkUtils {
    private const val TAG = "EmbeddedDeepLinkUtils"

    @JvmStatic
    fun Activity.tryStartMultiPaneDeepLink(
        intent: Intent,
        highlightMenuKey: String? = null,
    ): Boolean {
        intent.putExtra(
            SettingsActivity.EXTRA_INITIAL_CALLING_PACKAGE,
            PasswordUtils.getCallingAppPackageName(activityToken),
        )
        val trampolineIntent: Intent
        if (intent.getBooleanExtra(SettingsActivity.EXTRA_IS_FROM_SLICE, false)) {
            // Get menu key for slice deep link case.
            var sliceHighlightMenuKey: String? = intent.getStringExtra(
                Settings.EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_HIGHLIGHT_MENU_KEY
            )
            if (sliceHighlightMenuKey.isNullOrEmpty()) {
                sliceHighlightMenuKey = highlightMenuKey
            }
            trampolineIntent = getTrampolineIntent(intent, sliceHighlightMenuKey)
            trampolineIntent.setClass(this, DeepLinkHomepageActivityInternal::class.java)
        } else {
            trampolineIntent = getTrampolineIntent(intent, highlightMenuKey)
        }
        return startTrampolineIntent(trampolineIntent)
    }

    /**
     * Returns the deep link trampoline intent for large screen devices.
     */
    @JvmStatic
    fun getTrampolineIntent(intent: Intent, highlightMenuKey: String?): Intent {
        val detailIntent = Intent(intent)
        // Guard against the arbitrary Intent injection.
        if (detailIntent.selector != null) {
            detailIntent.setSelector(null)
        }
        // It's a deep link intent, SettingsHomepageActivity will set SplitPairRule and start it.
        return Intent(Settings.ACTION_SETTINGS_EMBED_DEEP_LINK_ACTIVITY).apply {
            setPackage(Utils.SETTINGS_PACKAGE_NAME)
            replaceExtras(detailIntent)

            // Relay detail intent data to prevent failure of Intent#ParseUri.
            // If Intent#getData() is not null, Intent#toUri will return an Uri which has the scheme
            // of Intent#getData() and it may not be the scheme of an Intent.
            putExtra(
                SettingsHomepageActivity.EXTRA_SETTINGS_LARGE_SCREEN_DEEP_LINK_INTENT_DATA,
                detailIntent.data
            )
            detailIntent.setData(null)
            putExtra(
                Settings.EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_INTENT_URI,
                detailIntent.toUri(Intent.URI_INTENT_SCHEME)
            )
            putExtra(
                Settings.EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_HIGHLIGHT_MENU_KEY,
                highlightMenuKey
            )
            addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
        }
    }

    private fun Context.startTrampolineIntent(trampolineIntent: Intent): Boolean = try {
        val userInfo = userManager.getUserInfo(user.identifier)
        if (userInfo.isManagedProfile) {
            trampolineIntent.setClass(this, DeepLinkHomepageActivityInternal::class.java)
                .putExtra(SettingsActivity.EXTRA_USER_HANDLE, user)
            startActivityAsUser(
                trampolineIntent,
                userManager.getProfileParent(userInfo.id).userHandle
            )
        } else {
            startActivity(trampolineIntent)
        }
        true
    } catch (e: ActivityNotFoundException) {
        Log.e(TAG, "Deep link homepage is not available to show 2-pane UI")
        false
    }
}
