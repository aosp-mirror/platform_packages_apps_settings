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

package com.android.settings.biometrics.fingerprint2.domain.interactor

import android.content.Context
import android.database.ContentObserver
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/** Interface that indicates if press to auth is on or off. */
interface PressToAuthInteractor {
    /** Indicates true if the PressToAuth feature is enabled, false otherwise. */
    val isEnabled: Flow<Boolean>
}

/** Indicates whether or not the press to auth feature is enabled. */
class PressToAuthInteractorImpl(
    private val context: Context,
    private val backgroundDispatcher: CoroutineDispatcher,
) : PressToAuthInteractor {

    /**
     * A flow that contains the status of the press to auth feature.
     */
    override val isEnabled: Flow<Boolean> =

        callbackFlow {
            val callback =
                object : ContentObserver(null) {
                    override fun onChange(selfChange: Boolean) {
                        Log.d(TAG, "SFPS_PERFORMANT_AUTH_ENABLED#onchange")
                        trySend(
                            getPressToAuth(),
                        )
                    }
                }

            context.contentResolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.SFPS_PERFORMANT_AUTH_ENABLED),
                false,
                callback,
                context.userId
            )
            trySend(getPressToAuth())
            awaitClose {
                context.contentResolver.unregisterContentObserver(callback)
            }
        }.flowOn(backgroundDispatcher)


    /**
     * Returns true if press to auth is enabled
     */
    private fun getPressToAuth(): Boolean {
        var toReturn: Int =
            Settings.Secure.getIntForUser(
                context.contentResolver,
                Settings.Secure.SFPS_PERFORMANT_AUTH_ENABLED,
                -1,
                context.userId,
            )
        if (toReturn == -1) {
            toReturn =
                if (
                    context.resources.getBoolean(com.android.internal.R.bool.config_performantAuthDefault)
                ) {
                    1
                } else {
                    0
                }
            Settings.Secure.putIntForUser(
                context.contentResolver,
                Settings.Secure.SFPS_PERFORMANT_AUTH_ENABLED,
                toReturn,
                context.userId,
            )
        }
        return toReturn == 1

    }

    companion object {
        const val TAG = "PressToAuthInteractor"
    }
}
