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
    private val context: Context,
    private val screenBindingHelper: PreferenceScreenBindingHelper,
) : KeyedObserver<String>, AutoCloseable {
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

    init {
        val restrictionKeys = restrictionKeysToPreferenceKeys.keys
        if (restrictionKeys.isNotEmpty()) {
            val userRestrictions = UserRestrictions.get(context)
            val executor = HandlerExecutor.main
            for (restrictionKey in restrictionKeys) {
                userRestrictions.addObserver(restrictionKey, this, executor)
            }
        }
    }

    override fun onKeyChanged(restrictionKey: String, reason: Int) {
        val keys = restrictionKeysToPreferenceKeys[restrictionKey] ?: return
        for (key in keys) screenBindingHelper.notifyChange(key, CHANGE_REASON_STATE)
    }

    override fun close() {
        val restrictionKeys = restrictionKeysToPreferenceKeys.keys
        if (restrictionKeys.isNotEmpty()) {
            val userRestrictions = UserRestrictions.get(context)
            for (restrictionKey in restrictionKeys) {
                userRestrictions.removeObserver(restrictionKey, this)
            }
        }
    }
}
