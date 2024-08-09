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

package com.android.settings.bluetooth.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.settings.bluetooth.ui.layout.DeviceSettingLayout
import com.android.settings.bluetooth.ui.layout.DeviceSettingLayoutRow
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.bluetooth.devicesettings.DeviceSettingId
import com.android.settingslib.bluetooth.devicesettings.data.repository.DeviceSettingRepository
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingConfigItemModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class BluetoothDeviceDetailsViewModel(
    private val deviceSettingRepository: DeviceSettingRepository,
    private val cachedDevice: CachedBluetoothDevice,
) : ViewModel() {
    private val items =
        viewModelScope.async(Dispatchers.IO, start = CoroutineStart.LAZY) {
            deviceSettingRepository.getDeviceSettingsConfig(cachedDevice)
        }

    suspend fun getItems(): List<DeviceSettingConfigItemModel>? = items.await()?.mainItems

    fun getDeviceSetting(cachedDevice: CachedBluetoothDevice, @DeviceSettingId settingId: Int) =
        deviceSettingRepository.getDeviceSetting(cachedDevice, settingId)

    suspend fun getLayout(): DeviceSettingLayout? {
        val configItems = getItems() ?: return null
        val idToDeviceSetting =
            configItems
                .filterIsInstance<DeviceSettingConfigItemModel.AppProvidedItem>()
                .associateBy({ it.settingId }, { getDeviceSetting(cachedDevice, it.settingId) })

        val configDeviceSetting =
            configItems.map { idToDeviceSetting[it.settingId] ?: flowOf(null) }
        val positionToSettingIds =
            combine(configDeviceSetting) { settings ->
                    val positionMapping = mutableMapOf<Int, List<Int>>()
                    var multiToggleSettingIds: MutableList<Int>? = null
                    for (i in settings.indices) {
                        val configItem = configItems[i]
                        val setting = settings[i]
                        val isXmlPreference = configItem is DeviceSettingConfigItemModel.BuiltinItem
                        if (!isXmlPreference && setting == null) {
                            continue
                        }
                        if (setting !is DeviceSettingModel.MultiTogglePreference) {
                            multiToggleSettingIds = null
                            positionMapping[i] = listOf(configItem.settingId)
                            continue
                        }

                        if (multiToggleSettingIds != null) {
                            multiToggleSettingIds.add(setting.id)
                        } else {
                            multiToggleSettingIds = mutableListOf(setting.id)
                            positionMapping[i] = multiToggleSettingIds
                        }
                    }
                    positionMapping
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), initialValue = mapOf())
        return DeviceSettingLayout(
            configItems.indices.map { idx ->
                DeviceSettingLayoutRow(positionToSettingIds.map { it[idx] ?: emptyList() })
            })
    }

    class Factory(
        private val deviceSettingRepository: DeviceSettingRepository,
        private val cachedDevice: CachedBluetoothDevice,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return BluetoothDeviceDetailsViewModel(deviceSettingRepository, cachedDevice) as T
        }
    }

    companion object {
        private const val TAG = "BluetoothDeviceDetailsViewModel"
    }
}
