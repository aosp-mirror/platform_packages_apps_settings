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

package com.android.settings.spa

import com.android.settings.spa.app.InstallUnknownAppsListProvider
import com.android.settings.spa.home.HomePageProvider
import com.android.settingslib.spa.framework.common.SettingsPageProviderRepository
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListTemplate

private val togglePermissionAppListTemplate = TogglePermissionAppListTemplate(
    allProviders = listOf(InstallUnknownAppsListProvider),
)

val settingsPageProviders = SettingsPageProviderRepository(
    allPagesList = listOf(
        HomePageProvider,
    ) + togglePermissionAppListTemplate.createPageProviders(),
    rootPages = listOf(HomePageProvider.name),
)
