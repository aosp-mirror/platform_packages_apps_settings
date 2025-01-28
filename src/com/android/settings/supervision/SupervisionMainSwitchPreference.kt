/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.settings.supervision

import android.app.supervision.SupervisionManager
import android.content.Context
import com.android.settings.R
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.NoOpKeyedObservable
import com.android.settingslib.metadata.MainSwitchPreference
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel

/** Main toggle to enable or disable device supervision. */
class SupervisionMainSwitchPreference :
    MainSwitchPreference(KEY, R.string.device_supervision_switch_title), PreferenceSummaryProvider {

    // TODO(b/383568136): Make presence of summary conditional on whether PIN
    // has been set up before or not.
    override fun getSummary(context: Context): CharSequence? =
        context.getString(R.string.device_supervision_switch_no_pin_summary)

    override fun storage(context: Context): KeyValueStore = SupervisionMainSwitchStorage(context)

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.DISALLOW

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int,
    ) = ReadWritePermit.DISALLOW

    override val sensitivityLevel: Int
        get() = SensitivityLevel.HIGH_SENSITIVITY

    @Suppress("UNCHECKED_CAST")
    private class SupervisionMainSwitchStorage(private val context: Context) :
        NoOpKeyedObservable<String>(), KeyValueStore {
        override fun contains(key: String) = key == KEY

        override fun <T : Any> getValue(key: String, valueType: Class<T>) =
            (context.getSystemService(SupervisionManager::class.java)?.isSupervisionEnabled() ==
                true)
                as T

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
            // TODO(b/392694561): add PIN protection to main toggle.
            if (key == KEY && value is Boolean) {
                val supervisionManager = context.getSystemService(SupervisionManager::class.java)
                supervisionManager.setSupervisionEnabled(value)
            }
        }
    }

    companion object {
        const val KEY = "device_supervision_switch"
    }
}
