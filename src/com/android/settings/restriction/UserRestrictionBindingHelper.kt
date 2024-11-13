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

package com.android.settings.restriction

import android.content.Context
import com.android.settings.PreferenceRestrictionMixin
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.preference.PreferenceScreenBindingHelper
import com.android.settingslib.preference.PreferenceScreenBindingHelper.Companion.CHANGE_REASON_STATE

/** Helper to rebind preference immediately when user restriction is changed. */
class UserRestrictionBindingHelper(
    context: Context,
    private val screenBindingHelper: PreferenceScreenBindingHelper,
) : AutoCloseable {
    private val restrictionKeysToPreferenceKeys: Map<String, MutableSet<String>> =
        mutableMapOf<String, MutableSet<String>>()
            .apply {
                screenBindingHelper.forEachRecursively {
                    val metadata = it.metadata
                    if (metadata is PreferenceRestrictionMixin) {
                        for (restrictionKey in metadata.restrictionKeys) {
                            getOrPut(restrictionKey) { mutableSetOf() }.add(metadata.key)
                        }
                    }
                }
            }
            .toMap()

    private val userRestrictionObserver: KeyedObserver<String?>?

    init {
        if (restrictionKeysToPreferenceKeys.isEmpty()) {
            userRestrictionObserver = null
        } else {
            val observer =
                KeyedObserver<String?> { restrictionKey, _ ->
                    restrictionKey?.let { notifyRestrictionChanged(it) }
                }
            UserRestrictions.addObserver(context, observer, HandlerExecutor.main)
            userRestrictionObserver = observer
        }
    }

    private fun notifyRestrictionChanged(restrictionKey: String) {
        val keys = restrictionKeysToPreferenceKeys[restrictionKey] ?: return
        for (key in keys) screenBindingHelper.notifyChange(key, CHANGE_REASON_STATE)
    }

    override fun close() {
        userRestrictionObserver?.let { UserRestrictions.removeObserver(it) }
    }
}
