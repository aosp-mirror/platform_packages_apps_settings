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

package com.android.settings.bluetooth.ui.composable

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingIcon

@Composable
fun Icon(
    icon: DeviceSettingIcon,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    when (icon) {
        is DeviceSettingIcon.BitmapIcon ->
            androidx.compose.material3.Icon(
                icon.bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = modifier,
                tint = LocalContentColor.current)
        is DeviceSettingIcon.ResourceIcon ->
            androidx.compose.material3.Icon(
                painterResource(icon.resId),
                contentDescription = null,
                modifier = modifier,
                tint = tint)
        else -> {}
    }
}
