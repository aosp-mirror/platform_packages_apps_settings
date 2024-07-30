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

package com.android.settings.wifi.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.util.Log
import com.android.settingslib.spaprivileged.framework.common.broadcastReceiverFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class WifiRepository(
    private val context: Context,
    private val wifiStateChangedActionFlow: Flow<Intent> =
        context.broadcastReceiverFlow(IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION)),
) {

    fun wifiStateFlow() = wifiStateChangedActionFlow
        .map { intent ->
            intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
        }
        .onEach { Log.d(TAG, "wifiStateFlow: $it") }

    private companion object {
        private const val TAG = "WifiRepository"
    }
}
