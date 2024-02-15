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

package com.android.settings.spa.about

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settingslib.spa.widget.ui.Category

object BasicInfoCategory {
    @Composable
    fun CategoryItems() {
        Category(title = stringResource(R.string.my_device_info_basic_info_category_title)) {
            DeviceNamePreference.EntryItem()
        }
    }
}