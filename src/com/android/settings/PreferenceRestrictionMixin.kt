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
import androidx.annotation.CallSuper
import com.android.settingslib.RestrictedLockUtilsInternal
import com.android.settingslib.metadata.PreferenceRestrictionProvider

/** Mixin to support restriction. */
interface PreferenceRestrictionMixin : PreferenceRestrictionProvider {

    val restrictionKey: String

    val useAdminDisabledSummary: Boolean
        get() = false

    @CallSuper fun isEnabled(context: Context) = !context.hasBaseUserRestriction(restrictionKey)

    override fun isRestricted(context: Context) =
        RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
            context,
            restrictionKey,
            UserHandle.myUserId(),
        ) != null
}

fun Context.hasBaseUserRestriction(restrictionKey: String) =
    RestrictedLockUtilsInternal.hasBaseUserRestriction(this, restrictionKey, UserHandle.myUserId())
