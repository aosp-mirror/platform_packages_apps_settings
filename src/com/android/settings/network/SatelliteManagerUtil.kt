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

package com.android.settings.network

import android.content.Context
import android.os.OutcomeReceiver
import android.telephony.satellite.SatelliteManager
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.Futures.immediateFuture
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

/**
 * Utility class for interacting with the SatelliteManager API.
 */
object SatelliteManagerUtil {

    private const val TAG: String = "SatelliteManagerUtil"

    /**
     * Checks if the satellite modem is enabled.
     *
     * @param context  The application context
     * @param executor The executor to run the asynchronous operation on
     * @return A ListenableFuture that will resolve to `true` if the satellite modem enabled,
     *         `false` otherwise.
     */
    @JvmStatic
    fun requestIsEnabled(context: Context, executor: Executor): ListenableFuture<Boolean> {
        val satelliteManager: SatelliteManager? =
            context.getSystemService(SatelliteManager::class.java)
        if (satelliteManager == null) {
            Log.w(TAG, "SatelliteManager is null")
            return immediateFuture(false)
        }

        return CallbackToFutureAdapter.getFuture { completer ->
            satelliteManager.requestIsEnabled(executor,
                object : OutcomeReceiver<Boolean, SatelliteManager.SatelliteException> {
                    override fun onResult(result: Boolean) {
                        Log.i(TAG, "Satellite modem enabled status: $result")
                        completer.set(result)
                    }

                    override fun onError(error: SatelliteManager.SatelliteException) {
                        super.onError(error)
                        Log.w(TAG, "Can't get satellite modem enabled status", error)
                        completer.set(false)
                    }
                })
            "requestIsEnabled"
        }
    }
}
