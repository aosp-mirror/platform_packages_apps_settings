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
package com.android.settings.users

import android.content.Context
import androidx.preference.PreferenceFragmentCompat
import com.android.settings.Utils
import com.android.settings.dashboard.profileselector.ProfileSelectFragment

class AutoSyncPrivateDataPreferenceController(
        context: Context?, parent: PreferenceFragmentCompat?)
    : AutoSyncDataPreferenceController(context, parent) {
    init {
        mUserHandle = Utils
                .getProfileOfType(mUserManager, ProfileSelectFragment.ProfileType.PRIVATE)
    }

    override fun getPreferenceKey(): String {
        return KEY_AUTO_SYNC_PRIVATE_ACCOUNT
    }

    override fun isAvailable(): Boolean {
        return (mUserHandle != null
                && mUserManager.getUserInfo(mUserHandle.identifier).isPrivateProfile)
    }

    companion object {
        private const val KEY_AUTO_SYNC_PRIVATE_ACCOUNT = "auto_sync_private_account_data"
    }
}