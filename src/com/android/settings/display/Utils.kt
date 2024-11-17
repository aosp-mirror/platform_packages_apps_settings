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

package com.android.settings.display

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.service.attention.AttentionService

fun Context.isAdaptiveSleepSupported() =
    resources.getBoolean(com.android.internal.R.bool.config_adaptive_sleep_available) &&
        isAttentionServiceAvailable()

private fun Context.isAttentionServiceAvailable(): Boolean {
    val packageManager = getPackageManager()
    val packageName = packageManager.attentionServicePackageName
    if (packageName.isNullOrEmpty()) return false
    val intent = Intent(AttentionService.SERVICE_INTERFACE).setPackage(packageName)
    val resolveInfo = packageManager.resolveService(intent, PackageManager.MATCH_SYSTEM_ONLY)
    return resolveInfo != null && resolveInfo.serviceInfo != null
}
