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
package com.android.settings.applications.credentials

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import com.android.settings.Utils
import com.android.settings.dashboard.profileselector.ProfileSelectFragment
import com.android.settingslib.applications.DefaultAppInfo

class DefaultPrivateCombinedPreferenceController(context: Context?) : DefaultCombinedPreferenceController(context) {
    private val userHandle: UserHandle? =
            Utils.getProfileOfType(mUserManager, ProfileSelectFragment.ProfileType.PRIVATE)

    override fun isAvailable(): Boolean {
        return if (userHandle == null) {
            false
        } else super.isAvailable()
    }

    override fun getPreferenceKey(): String {
        return "default_credman_autofill_private"
    }

    override fun startActivity(intent: Intent) {
        userHandle?.let { handle ->
            mContext.startActivityAsUser(intent, handle)
        }
    }

    override fun getUser(): Int {
        return userHandle?.identifier ?: UserHandle.myUserId()
    }
}