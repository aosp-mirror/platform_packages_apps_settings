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

import android.Manifest.permission.WRITE_SYSTEM_PREFERENCES
import android.app.AppOpsManager.OP_WRITE_SYSTEM_PREFERENCES
import com.android.settings.metrics.SettingsRemoteOpMetricsLogger
import com.android.settingslib.ipc.ApiPermissionChecker
import com.android.settingslib.ipc.AppOpApiPermissionChecker
import com.android.settingslib.service.PreferenceService

/** Service to expose settings APIs. */
class SettingsService :
    PreferenceService(
        graphPermissionChecker = ApiPermissionChecker.alwaysAllow(),
        setterPermissionChecker =
            AppOpApiPermissionChecker(OP_WRITE_SYSTEM_PREFERENCES, WRITE_SYSTEM_PREFERENCES),
        getterPermissionChecker = ApiPermissionChecker.alwaysAllow(),
        metricsLogger = SettingsRemoteOpMetricsLogger(),
    )
