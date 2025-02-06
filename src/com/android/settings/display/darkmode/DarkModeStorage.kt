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

package com.android.settings.display.darkmode

import android.app.UiModeManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.PowerManager
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.PreferenceChangeReason

/**
 * Abstract storage for dark mode settings.
 *
 * The underlying storage is manipulated by [UiModeManager] but we do not need to worry about the
 * details.
 */
@Suppress("UNCHECKED_CAST")
internal class DarkModeStorage(private val context: Context) :
    AbstractKeyedDataObservable<String>(), KeyValueStore {
    private lateinit var broadcastReceiver: BroadcastReceiver
    private lateinit var darkModeObserver: DarkModeObserver

    override fun contains(key: String) = true

    override fun <T : Any> getValue(key: String, valueType: Class<T>) = context.isDarkMode() as T

    private fun Context.isDarkMode() =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_YES) != 0

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        context.getSystemService(UiModeManager::class.java)?.setNightModeActivated(value as Boolean)
    }

    override fun onFirstObserverAdded() {
        broadcastReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    notifyChange(PreferenceChangeReason.STATE)
                }
            }
        context.registerReceiver(
            broadcastReceiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
        )

        darkModeObserver = DarkModeObserver(context)
        darkModeObserver.subscribe { notifyChange(PreferenceChangeReason.VALUE) }
    }

    override fun onLastObserverRemoved() {
        context.unregisterReceiver(broadcastReceiver)
        darkModeObserver.unsubscribe()
    }
}
