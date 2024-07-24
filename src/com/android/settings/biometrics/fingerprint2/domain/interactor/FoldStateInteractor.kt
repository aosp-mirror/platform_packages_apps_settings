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
import android.content.res.Configuration
import com.android.systemui.unfold.compat.ScreenSizeFoldProvider
import com.android.systemui.unfold.updates.FoldProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface FoldStateInteractor {
    /** A flow that contains the fold state info */
    val isFolded: Flow<Boolean>

    /**
     * Indicates a configuration change has occurred, and the repo
     * should update the [isFolded] flow.
     */
    fun onConfigurationChange(newConfig: Configuration)
}

/**
 * Interactor which handles fold state
 */
class FoldStateInteractorImpl(context: Context) : FoldStateInteractor {
    private val screenSizeFoldProvider = ScreenSizeFoldProvider(context)
    override val isFolded: Flow<Boolean> = callbackFlow {
        val foldStateListener = FoldProvider.FoldCallback { isFolded -> trySend(isFolded) }
        screenSizeFoldProvider.registerCallback(foldStateListener, context.mainExecutor)
        awaitClose { screenSizeFoldProvider.unregisterCallback(foldStateListener) }
    }

    /**
     * This function is called by the root activity, indicating an orientation event has occurred.
     * When this happens, the [ScreenSizeFoldProvider] is notified and it will re-compute if the
     * device is folded or not, and notify the [FoldProvider.FoldCallback]
     */
    override fun onConfigurationChange(newConfig: Configuration) {
        screenSizeFoldProvider.onConfigurationChange(newConfig)
    }

}
