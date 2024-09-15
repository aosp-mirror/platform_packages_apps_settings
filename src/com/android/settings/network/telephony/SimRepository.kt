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

package com.android.settings.network.telephony

import android.content.Context
import android.content.pm.PackageManager
import com.android.settingslib.spaprivileged.framework.common.userManager

class SimRepository(context: Context) {
    private val packageManager = context.packageManager
    private val userManager = context.userManager

    /** Gets whether we show mobile network settings page to the current user. */
    fun showMobileNetworkPage(): Boolean =
        packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) && userManager.isAdminUser
}
