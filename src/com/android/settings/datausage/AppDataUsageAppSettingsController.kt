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

package com.android.settings.datausage

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import androidx.annotation.OpenForTesting
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.core.BasePreferenceController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OpenForTesting
open class AppDataUsageAppSettingsController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey) {

    private var packageNames: Iterable<String> = emptyList()
    private var userId: Int = -1
    private lateinit var preference: Preference
    private var resolvedIntent: Intent? = null

    private val packageManager = mContext.packageManager

    override fun getAvailabilityStatus() = AVAILABLE

    fun init(packageNames: Iterable<String>, userId: Int) {
        this.packageNames = packageNames
        this.userId = userId
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)!!
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                update()
            }
        }
    }

    private suspend fun update() {
        resolvedIntent = withContext(Dispatchers.Default) {
            packageNames.map { packageName ->
                Intent(SettingsIntent).setPackage(packageName)
            }.firstOrNull { intent ->
                packageManager.resolveActivityAsUser(intent, 0, userId) != null
            }
        }
        preference.isVisible = resolvedIntent != null
    }

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == mPreferenceKey) {
            resolvedIntent?.let { mContext.startActivityAsUser(it, UserHandle.of(userId)) }
            return true
        }
        return false
    }

    private companion object {
        val SettingsIntent = Intent(Intent.ACTION_MANAGE_NETWORK_USAGE).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
        }
    }
}
