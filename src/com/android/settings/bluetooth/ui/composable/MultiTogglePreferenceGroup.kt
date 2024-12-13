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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.android.settings.R
import com.android.settings.bluetooth.ui.model.DeviceSettingPreferenceModel
import com.android.settings.bluetooth.ui.composable.Icon as DeviceSettingComposeIcon
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.widget.dialog.getDialogWidth

@Composable
fun MultiTogglePreferenceGroup(
    preferenceModels: List<DeviceSettingPreferenceModel.MultiTogglePreference>,
) {
    var settingIdForPopUp by remember { mutableStateOf<Int?>(null) }

    settingIdForPopUp?.let { id ->
        preferenceModels.find { it.id == id && it.isAllowedChangingState }?.let {
            dialog(it) { settingIdForPopUp = null }
        } ?: run {
            settingIdForPopUp = null
        }
    }

    Row(
        modifier = Modifier.padding(SettingsDimension.itemPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        preferenceModels.forEach { preferenceModel ->
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row {
                    Surface(
                        modifier = Modifier.height(64.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surface) {
                            Button(
                                modifier =
                                    Modifier.fillMaxSize().padding(8.dp).semantics {
                                        role = Role.Switch
                                        toggleableState =
                                            if (!preferenceModel.isAllowedChangingState) {
                                                ToggleableState.Indeterminate
                                            } else if (preferenceModel.isActive) {
                                                ToggleableState.On
                                            } else {
                                                ToggleableState.Off
                                            }
                                        contentDescription = preferenceModel.title
                                    },
                                onClick = { settingIdForPopUp = preferenceModel.id },
                                enabled = preferenceModel.isAllowedChangingState,
                                shape = RoundedCornerShape(20.dp),
                                colors = getButtonColors(preferenceModel.isActive),
                                contentPadding = PaddingValues(0.dp)) {
                                    DeviceSettingComposeIcon(
                                        preferenceModel.toggles[preferenceModel.selectedIndex]
                                            .icon,
                                        modifier = Modifier.size(24.dp))
                                }
                        }
                }
                Row { Text(text = preferenceModel.title, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun getButtonColors(isActive: Boolean) =
    if (isActive) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    } else {
        ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun dialog(
    multiTogglePreference: DeviceSettingPreferenceModel.MultiTogglePreference,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = { onDismiss() },
        modifier = Modifier.width(getDialogWidth()),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        content = {
            Card(
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().height(192.dp),
                content = {
                    Box {
                        Button(
                            onClick = { onDismiss() },
                            modifier = Modifier.padding(8.dp).align(Alignment.TopEnd).size(48.dp),
                            contentPadding = PaddingValues(12.dp),
                            colors =
                                ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        ) {
                            Icon(
                                painterResource(id = R.drawable.ic_close),
                                null,
                                tint = MaterialTheme.colorScheme.inverseSurface)
                        }
                        Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 20.dp)) {
                            dialogContent(multiTogglePreference)
                        }
                    }
                },
            )
        })
}

@Composable
private fun dialogContent(multiTogglePreference: DeviceSettingPreferenceModel.MultiTogglePreference) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().height(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text(text = multiTogglePreference.title, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(20.dp))
        var selectedRect by remember { mutableStateOf<Rect?>(null) }
        val offset =
            selectedRect?.let { rect ->
                animateFloatAsState(targetValue = rect.left, finishedListener = {}).value
            }

        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .height(64.dp)
                    .background(
                        MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(28.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Box {
                offset?.let { offset ->
                    with(LocalDensity.current) {
                        Box(
                            modifier =
                                Modifier.offset(offset.toDp(), 0.dp)
                                    .height(selectedRect!!.height.toDp())
                                    .width(selectedRect!!.width.toDp())
                                    .background(
                                        MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = RoundedCornerShape(20.dp)))
                    }
                }
                Row {
                    for ((idx, toggle) in multiTogglePreference.toggles.withIndex()) {
                        val selected = idx == multiTogglePreference.selectedIndex
                        Column(
                            modifier =
                                Modifier.weight(1f)
                                    .padding(horizontal = 8.dp)
                                    .height(48.dp)
                                    .background(
                                        Color.Transparent, shape = RoundedCornerShape(28.dp))
                                    .onGloballyPositioned { layoutCoordinates ->
                                        if (selected) {
                                            selectedRect = layoutCoordinates.boundsInParent()
                                        }
                                    },
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Button(
                                onClick = {
                                    multiTogglePreference.onSelectedChange(idx)
                                },
                                modifier = Modifier.fillMaxSize(),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = LocalContentColor.current),
                            ) {
                                DeviceSettingComposeIcon(
                                    toggle.icon, modifier = Modifier.size(24.dp))
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
            for (toggle in multiTogglePreference.toggles) {
                Text(
                    text = toggle.label,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
            }
        }
    }
}
