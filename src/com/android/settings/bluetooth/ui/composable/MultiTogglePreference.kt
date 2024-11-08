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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.settings.bluetooth.ui.composable.Icon as DeviceSettingComposeIcon
import com.android.settings.bluetooth.ui.model.DeviceSettingPreferenceModel

@Composable
fun MultiTogglePreference(pref: DeviceSettingPreferenceModel.MultiTogglePreference) {
    Column(modifier = Modifier.padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Box {
                Row {
                    for ((idx, toggle) in pref.toggles.withIndex()) {
                        val selected = idx == pref.selectedIndex
                        Column(
                            modifier = Modifier.weight(1f)
                                .padding(start = if (idx == 0) 0.dp else 1.dp)
                                .height(56.dp)
                                .background(
                                    Color.Transparent,
                                    shape = RoundedCornerShape(12.dp),
                                ),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val startCornerRadius = if (idx == 0) 12.dp else 0.dp
                            val endCornerRadius = if (idx == pref.toggles.size - 1) 12.dp else 0.dp
                            Button(
                                onClick = { pref.onSelectedChange(idx) },
                                modifier = Modifier.fillMaxSize(),
                                enabled = pref.isAllowedChangingState,
                                colors = getButtonColors(selected),
                                shape = RoundedCornerShape(
                                    startCornerRadius,
                                    endCornerRadius,
                                    endCornerRadius,
                                    startCornerRadius,
                                )
                            ) {
                                DeviceSettingComposeIcon(
                                    toggle.icon,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().defaultMinSize(32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            for (toggle in pref.toggles) {
                Text(
                    text = toggle.label,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun getButtonColors(isActive: Boolean) = if (isActive) {
    ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    )
} else {
    ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )
}
