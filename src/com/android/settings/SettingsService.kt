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

package com.android.settings

import android.app.Application
import android.content.Intent
import com.android.settings.flags.Flags
import com.android.settingslib.graph.PreferenceGetterRequest
import com.android.settingslib.graph.PreferenceSetterRequest
import com.android.settingslib.ipc.ApiPermissionChecker
import com.android.settingslib.service.PreferenceService

/** Service to expose settings APIs. */
class SettingsService :
    PreferenceService(
        graphPermissionChecker = ApiPermissionChecker.alwaysAllow(),
        setterPermissionChecker = SetterPermissionChecker(),
        getterPermissionChecker = GetterPermissionChecker(),
    ) {

    override fun onBind(intent: Intent) =
        if (Flags.catalystService()) super.onBind(intent) else null
}

/** Permission checker for external setter API. */
private class SetterPermissionChecker : ApiPermissionChecker<PreferenceSetterRequest> {

    override fun hasPermission(
        application: Application,
        callingPid: Int,
        callingUid: Int,
        request: PreferenceSetterRequest,
    ) = true
}

/** Permission checker for external getter API. */
private class GetterPermissionChecker : ApiPermissionChecker<PreferenceGetterRequest> {

    override fun hasPermission(
        application: Application,
        callingPid: Int,
        callingUid: Int,
        request: PreferenceGetterRequest,
    ) = true
}
