/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.spa.app.specialaccess

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import com.android.settings.R
import com.android.settingslib.spaprivileged.template.app.AppOpPermissionListModel
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListProvider

object UseFullScreenIntentAppListProvider : TogglePermissionAppListProvider {
    override val permissionType = "UseFullScreenIntent"
    override fun createModel(context: Context) = UseFullScreenIntentListModel(context)
}

class UseFullScreenIntentListModel(context: Context) : AppOpPermissionListModel(context) {
    override val pageTitleResId = R.string.full_screen_intent_title
    override val switchTitleResId = R.string.permit_full_screen_intent
    override val footerResId = R.string.footer_description_full_screen_intent
    override val appOp = AppOpsManager.OP_USE_FULL_SCREEN_INTENT
    override val permission = Manifest.permission.USE_FULL_SCREEN_INTENT
    override val setModeByUid = true
}
