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
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.util.Log
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.wifitrackerlib.WifiEntry
import com.android.wifitrackerlib.WifiPickerTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach

/** Repository that listeners to wifi picker callback and provide wifi picker flow to client. */
class WifiPickerRepository(
    private val context: Context,
    private val createWifiPickerTracker:
        (
            workerThread: HandlerThread, callback: WifiPickerTracker.WifiPickerTrackerCallback
        ) -> WifiPickerTracker =
        { workerThread, callback ->
            featureFactory.wifiTrackerLibProvider.createWifiPickerTracker(
                null,
                context,
                Handler(Looper.getMainLooper()),
                workerThread.getThreadHandler(),
                SystemClock.elapsedRealtimeClock(),
                MAX_SCAN_AGE_MILLIS,
                SCAN_INTERVAL_MILLIS,
                callback,
            )
        }
) {

    fun connectedWifiEntryFlow(): Flow<WifiEntry?> =
        callbackFlow {
                val workerThread =
                    HandlerThread(
                        /* name = */ "$TAG{${Integer.toHexString(System.identityHashCode(this))}}",
                        /* priority = */ Process.THREAD_PRIORITY_BACKGROUND,
                    )
                workerThread.start()
                var tracker: WifiPickerTracker? = null
                val callback =
                    object : WifiPickerTracker.WifiPickerTrackerCallback {
                        override fun onWifiEntriesChanged() {
                            trySend(tracker?.connectedWifiEntry)
                        }

                        override fun onWifiStateChanged() {}

                        override fun onNumSavedNetworksChanged() {}

                        override fun onNumSavedSubscriptionsChanged() {}
                    }

                tracker = createWifiPickerTracker(workerThread, callback)
                tracker.onStart()

                awaitClose {
                    tracker.onStop()
                    tracker.onDestroy()
                    workerThread.quit()
                }
            }
            .conflate()
            .onEach { Log.d(TAG, "connectedWifiEntryFlow: $it") }
            .flowOn(Dispatchers.Default)

    companion object {
        private const val TAG = "WifiPickerRepository"

        /** Max age of tracked WifiEntries */
        private const val MAX_SCAN_AGE_MILLIS: Long = 15000
        /** Interval between initiating WifiPickerTracker scans */
        private const val SCAN_INTERVAL_MILLIS: Long = 10000
    }
}
