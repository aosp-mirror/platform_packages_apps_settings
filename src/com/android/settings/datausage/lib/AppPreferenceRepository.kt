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

package com.android.settings.datausage.lib

import android.content.Context
import android.content.pm.PackageManager
import android.os.UserHandle
import android.util.IconDrawableFactory
import androidx.preference.Preference

class AppPreferenceRepository(
    private val context: Context,
    private val iconDrawableFactory: IconDrawableFactory = IconDrawableFactory.newInstance(context),
) {
    private val packageManager = context.packageManager

    fun loadAppPreferences(uids: List<Int>): List<Preference> = uids.flatMap { uid ->
        val userId = UserHandle.getUserId(uid)
        getPackagesForUid(uid).mapNotNull { packageName ->
            getPreference(packageName, userId)
        }
    }

    private fun getPackagesForUid(uid: Int): Array<String> =
        packageManager.getPackagesForUid(uid) ?: emptyArray()

    private fun getPreference(packageName: String, userId: Int): Preference? = try {
        val app = packageManager.getApplicationInfoAsUser(packageName, 0, userId)
        Preference(context).apply {
            icon = iconDrawableFactory.getBadgedIcon(app)
            title = app.loadLabel(packageManager)
            isSelectable = false
        }
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}
