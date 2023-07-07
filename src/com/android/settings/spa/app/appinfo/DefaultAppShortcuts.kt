/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.spa.app.appinfo

import android.app.role.RoleManager
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import com.android.settings.R

@Composable
fun DefaultAppShortcuts(app: ApplicationInfo) {
    for (shortCut in SHORT_CUTS) {
        DefaultAppShortcutPreference(shortCut, app)
    }
}

private val SHORT_CUTS = listOf(
    DefaultAppShortcut(RoleManager.ROLE_HOME, R.string.home_app),
    DefaultAppShortcut(RoleManager.ROLE_BROWSER, R.string.default_browser_title),
    DefaultAppShortcut(RoleManager.ROLE_DIALER, R.string.default_phone_title),
    DefaultAppShortcut(RoleManager.ROLE_EMERGENCY, R.string.default_emergency_app),
    DefaultAppShortcut(RoleManager.ROLE_SMS, R.string.sms_application_title),
)
