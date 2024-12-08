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

import android.os.Binder
import android.os.OutcomeReceiver
import android.os.Process
import android.service.settings.preferences.GetValueRequest
import android.service.settings.preferences.GetValueResult
import android.service.settings.preferences.MetadataRequest
import android.service.settings.preferences.MetadataResult
import android.service.settings.preferences.SetValueRequest
import android.service.settings.preferences.SetValueResult
import android.service.settings.preferences.SettingsPreferenceService
import com.android.settingslib.graph.PreferenceGetterApiHandler
import com.android.settingslib.graph.PreferenceSetterApiHandler
import com.android.settingslib.ipc.ApiPermissionChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.Exception

class PreferenceService : SettingsPreferenceService() {

    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    private val getApiHandler = PreferenceGetterApiHandler(1, ApiPermissionChecker.alwaysAllow())
    private val setApiHandler = PreferenceSetterApiHandler(2, ApiPermissionChecker.alwaysAllow())

    override fun onGetAllPreferenceMetadata(
        request: MetadataRequest,
        callback: OutcomeReceiver<MetadataResult, Exception>
    ) {
        // TODO(379750656): Update graph API to be usable outside SettingsLib
        callback.onError(UnsupportedOperationException("Not yet supported"))
    }

    override fun onGetPreferenceValue(
        request: GetValueRequest,
        callback: OutcomeReceiver<GetValueResult, Exception>
    ) {
        scope.launch(Dispatchers.IO) {
            val apiRequest = transformFrameworkGetValueRequest(request)
            val response = getApiHandler.invoke(application, Process.myUid(),
                Binder.getCallingPid(), apiRequest)
            val result = transformCatalystGetValueResponse(
                this@PreferenceService,
                request,
                response
            )
            if (result == null) {
                callback.onError(IllegalStateException("No response"))
            } else {
                callback.onResult(result)
            }
        }
    }

    override fun onSetPreferenceValue(
        request: SetValueRequest,
        callback: OutcomeReceiver<SetValueResult, Exception>
    ) {
        scope.launch(Dispatchers.IO) {
            val apiRequest = transformFrameworkSetValueRequest(request)
            if (apiRequest == null) {
                callback.onResult(
                    SetValueResult.Builder(SetValueResult.RESULT_INVALID_REQUEST).build()
                )
            } else {
                val response = setApiHandler.invoke(application, Process.myUid(),
                    Binder.getCallingPid(), apiRequest)

                callback.onResult(transformCatalystSetValueResponse(response))
            }
        }
    }
}
