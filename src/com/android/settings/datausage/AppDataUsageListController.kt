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
import androidx.annotation.OpenForTesting
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import com.android.settings.core.BasePreferenceController
import com.android.settings.datausage.lib.AppPreferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OpenForTesting
open class AppDataUsageListController @JvmOverloads constructor(
    context: Context,
    preferenceKey: String,
    private val repository: AppPreferenceRepository = AppPreferenceRepository(context),
) : BasePreferenceController(context, preferenceKey) {

    private var uids: List<Int> = emptyList()
    private lateinit var preference: PreferenceGroup

    fun init(uids: List<Int>) {
        this.uids = uids
    }

    override fun getAvailabilityStatus() = AVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)!!
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                updateList()
            }
        }
    }

    private suspend fun updateList() {
        if (uids.size <= 1) {
            preference.isVisible = false
            return
        }
        preference.isVisible = true
        val appPreferences = withContext(Dispatchers.Default) {
            repository.loadAppPreferences(uids)
        }
        preference.removeAll()
        for (appPreference in appPreferences) {
            preference.addPreference(appPreference)
        }
    }
}
