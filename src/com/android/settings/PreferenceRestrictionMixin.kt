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

package com.android.settings

import android.content.Context
import android.os.UserHandle
import android.os.UserManager
import androidx.annotation.CallSuper
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin
import com.android.settingslib.RestrictedLockUtilsInternal
import com.android.settingslib.metadata.PreferenceRestrictionProvider

/** Mixin to support restriction. */
interface PreferenceRestrictionMixin : PreferenceRestrictionProvider {

    /**
     * Keys for restriction.
     *
     * Preference is restricted when **ANY** key in the list is restricted.
     */
    val restrictionKeys: Array<String>

    val useAdminDisabledSummary: Boolean
        get() = false

    @CallSuper fun isEnabled(context: Context) = !context.hasBaseUserRestriction(restrictionKeys)

    override fun isRestricted(context: Context) =
        context.getRestrictionEnforcedAdmin(restrictionKeys) != null
}

/** Returns the admin that has enforced restriction on given keys. */
fun Context.getRestrictionEnforcedAdmin(restrictionKeys: Array<String>): EnforcedAdmin? {
    val userId = UserHandle.myUserId()
    return restrictionKeys.firstNotNullOfOrNull {
        RestrictedLockUtilsInternal.checkIfRestrictionEnforced(this, it, userId)
    }
}

/** Returns if there is **any** base user restriction on given keys. */
fun Context.hasBaseUserRestriction(restrictionKeys: Array<String>): Boolean {
    val userManager = getSystemService(UserManager::class.java) ?: return false
    val userHandle = UserHandle.of(UserHandle.myUserId())
    return restrictionKeys.any { userManager.hasBaseUserRestriction(it, userHandle) }
}
