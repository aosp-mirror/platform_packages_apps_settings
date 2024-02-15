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

package com.android.settings.network.telephony

import android.content.Context
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.flow.Flow

private const val TAG = "AllowedNetworkTypesFlow"

/** Creates an instance of a cold Flow for Allowed Network Types of given [subId]. */
fun Context.allowedNetworkTypesFlow(subId: Int): Flow<Long> = telephonyCallbackFlow(subId) {
    object : TelephonyCallback(), TelephonyCallback.AllowedNetworkTypesListener {
        override fun onAllowedNetworkTypesChanged(reason: Int, allowedNetworkType: Long) {
            if (reason == TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER ||
                reason == TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_CARRIER
            ) {
                trySend(allowedNetworkType)
                Log.d(TAG, "[$subId] reason: $reason, allowedNetworkType: $allowedNetworkType")
            }
        }
    }
}
