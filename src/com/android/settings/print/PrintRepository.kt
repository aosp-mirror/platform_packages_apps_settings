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

package com.android.settings.print

import android.content.Context
import android.graphics.drawable.Drawable
import android.print.PrintManager
import android.printservice.PrintServiceInfo
import com.android.settings.R
import com.android.settingslib.spa.framework.util.mapItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class PrintRepository(private val context: Context) {

    private val printManager = context.getSystemService(PrintManager::class.java)!!
    private val packageManager = context.packageManager

    data class PrintServiceDisplayInfo(
        val title: String,
        val isEnabled: Boolean,
        val summary: String,
        val icon: Drawable,
        val componentName: String,
    )

    fun printServiceDisplayInfosFlow(): Flow<List<PrintServiceDisplayInfo>> =
        printServicesFlow()
            .mapItem { printService -> printService.toPrintServiceDisplayInfo() }
            .conflate()
            .flowOn(Dispatchers.Default)

    private fun PrintServiceInfo.toPrintServiceDisplayInfo() = PrintServiceDisplayInfo(
        title = resolveInfo.loadLabel(packageManager).toString(),
        isEnabled = isEnabled,
        summary = context.getString(
            if (isEnabled) R.string.print_feature_state_on else R.string.print_feature_state_off
        ),
        icon = resolveInfo.loadIcon(packageManager),
        componentName = componentName.flattenToString(),
    )

    private fun printServicesFlow(): Flow<List<PrintServiceInfo>> =
        printManager.printServicesChangeFlow()
            .map { printManager.getPrintServices(PrintManager.ALL_SERVICES) }
            .conflate()
            .flowOn(Dispatchers.Default)

    private companion object {
        fun PrintManager.printServicesChangeFlow(): Flow<Unit> = callbackFlow {
            val listener = PrintManager.PrintServicesChangeListener { trySend(Unit) }
            addPrintServicesChangeListener(listener, null)
            trySend(Unit)
            awaitClose { removePrintServicesChangeListener(listener) }
        }.conflate().flowOn(Dispatchers.Default)
    }
}
