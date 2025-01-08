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

package com.android.settings.service

import android.app.Application
import android.os.Binder
import android.os.OutcomeReceiver
import android.service.settings.preferences.GetValueRequest
import android.service.settings.preferences.GetValueResult
import android.service.settings.preferences.MetadataRequest
import android.service.settings.preferences.MetadataResult
import android.service.settings.preferences.SetValueRequest
import android.service.settings.preferences.SetValueResult
import android.service.settings.preferences.SettingsPreferenceService
import com.android.settingslib.graph.GetPreferenceGraphApiHandler
import com.android.settingslib.graph.GetPreferenceGraphRequest
import com.android.settingslib.graph.PreferenceGetterApiHandler
import com.android.settingslib.graph.PreferenceGetterFlags
import com.android.settingslib.graph.PreferenceSetterApiHandler
import com.android.settingslib.ipc.ApiPermissionChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PreferenceService : SettingsPreferenceService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val getApiHandler = PreferenceGetterApiHandler(1, ApiPermissionChecker.alwaysAllow())
    private val setApiHandler = PreferenceSetterApiHandler(2, ApiPermissionChecker.alwaysAllow())
    private val graphApi = GraphProvider(3)

    override fun onGetAllPreferenceMetadata(
        request: MetadataRequest,
        callback: OutcomeReceiver<MetadataResult, Exception>,
    ) {
        // MUST get pid/uid in binder thread
        val callingPid = Binder.getCallingPid()
        val callingUid = Binder.getCallingUid()
        scope.launch {
            val graphProto =
                graphApi.invoke(
                    application,
                    callingPid,
                    callingUid,
                    GetPreferenceGraphRequest(
                        flags = PreferenceGetterFlags.METADATA,
                    ),
                )
            val result = transformCatalystGetMetadataResponse(this@PreferenceService, graphProto)
            callback.onResult(result)
        }
    }

    override fun onGetPreferenceValue(
        request: GetValueRequest,
        callback: OutcomeReceiver<GetValueResult, Exception>,
    ) {
        // MUST get pid/uid in binder thread
        val callingPid = Binder.getCallingPid()
        val callingUid = Binder.getCallingUid()
        scope.launch {
            val apiRequest = transformFrameworkGetValueRequest(request)
            val response = getApiHandler.invoke(application, callingPid, callingUid, apiRequest)
            val result =
                transformCatalystGetValueResponse(this@PreferenceService, request, response)
            if (result == null) {
                callback.onError(IllegalStateException("No response"))
            } else {
                callback.onResult(result)
            }
        }
    }

    override fun onSetPreferenceValue(
        request: SetValueRequest,
        callback: OutcomeReceiver<SetValueResult, Exception>,
    ) {
        // MUST get pid/uid in binder thread
        val callingPid = Binder.getCallingPid()
        val callingUid = Binder.getCallingUid()
        scope.launch {
            val apiRequest = transformFrameworkSetValueRequest(request)
            if (apiRequest == null) {
                callback.onResult(
                    SetValueResult.Builder(SetValueResult.RESULT_INVALID_REQUEST).build()
                )
            } else {
                val response = setApiHandler.invoke(application, callingPid, callingUid, apiRequest)

                callback.onResult(transformCatalystSetValueResponse(response))
            }
        }
    }

    // Basic implementation - we already have permission to access Graph for Metadata via superclass
    private class GraphProvider(override val id: Int) : GetPreferenceGraphApiHandler(emptySet()) {
        override fun hasPermission(
            application: Application,
            callingPid: Int,
            callingUid: Int,
            request: GetPreferenceGraphRequest,
        ) = true
    }
}
