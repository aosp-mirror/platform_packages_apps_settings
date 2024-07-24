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

package com.android.settings.system

import android.content.Context
import android.content.Intent
import android.telephony.CarrierConfigManager
import android.util.Log
import com.android.settings.network.telephony.safeGetConfig

class ClientInitiatedActionRepository(private val context: Context) {
    private val configManager = context.getSystemService(CarrierConfigManager::class.java)!!

    /**
     * Trigger client initiated action (send intent) on system update
     */
    fun onSystemUpdate() {
        val bundle =
            configManager.safeGetConfig(
                keys = listOf(
                    CarrierConfigManager.KEY_CI_ACTION_ON_SYS_UPDATE_BOOL,
                    CarrierConfigManager.KEY_CI_ACTION_ON_SYS_UPDATE_INTENT_STRING,
                    CarrierConfigManager.KEY_CI_ACTION_ON_SYS_UPDATE_EXTRA_STRING,
                    CarrierConfigManager.KEY_CI_ACTION_ON_SYS_UPDATE_EXTRA_VAL_STRING,
                ),
            )

        if (!bundle.getBoolean(CarrierConfigManager.KEY_CI_ACTION_ON_SYS_UPDATE_BOOL)) return

        val action =
            bundle.getString(CarrierConfigManager.KEY_CI_ACTION_ON_SYS_UPDATE_INTENT_STRING)
        if (action.isNullOrEmpty()) return
        val extra = bundle.getString(CarrierConfigManager.KEY_CI_ACTION_ON_SYS_UPDATE_EXTRA_STRING)
        val extraValue =
            bundle.getString(CarrierConfigManager.KEY_CI_ACTION_ON_SYS_UPDATE_EXTRA_VAL_STRING)
        Log.d(TAG, "onSystemUpdate: broadcasting intent $action with extra $extra, $extraValue")
        val intent = Intent(action).apply {
            if (!extra.isNullOrEmpty()) putExtra(extra, extraValue)
            addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND)
        }
        context.applicationContext.sendBroadcast(intent)
    }

    companion object {
        private const val TAG = "ClientInitiatedAction"
    }
}
