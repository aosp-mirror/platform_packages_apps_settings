/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.settings.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.Spatializer
import android.net.Uri
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.preference.Preference
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.bluetooth.domain.interactor.SpatialAudioInteractor
import com.android.settings.bluetooth.domain.interactor.SpatialAudioInteractorImpl
import com.android.settings.bluetooth.ui.view.DeviceDetailsFragmentFormatter
import com.android.settings.bluetooth.ui.view.DeviceDetailsFragmentFormatterImpl
import com.android.settingslib.bluetooth.BluetoothUtils
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.bluetooth.devicesettings.data.repository.DeviceSettingRepository
import com.android.settingslib.bluetooth.devicesettings.data.repository.DeviceSettingRepositoryImpl
import com.android.settingslib.media.data.repository.SpatializerRepositoryImpl
import com.android.settingslib.media.domain.interactor.SpatializerInteractor
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import kotlinx.coroutines.Dispatchers

/** Impl of [BluetoothFeatureProvider] */
open class BluetoothFeatureProviderImpl : BluetoothFeatureProvider {
    override fun getBluetoothDeviceSettingsUri(bluetoothDevice: BluetoothDevice): Uri? {
        val uriByte = bluetoothDevice.getMetadata(BluetoothDevice.METADATA_ENHANCED_SETTINGS_UI_URI)
        return uriByte?.let { Uri.parse(String(it)) }
    }

    override fun getBluetoothDeviceControlUri(bluetoothDevice: BluetoothDevice): String? {
        return BluetoothUtils.getControlUriMetaData(bluetoothDevice)
    }

    override fun getRelatedTools(): List<ComponentName>? {
        return null
    }

    override fun getSpatializer(context: Context): Spatializer {
        val audioManager = context.getSystemService(AudioManager::class.java)
        return audioManager.spatializer
    }

    override fun getBluetoothExtraOptions(
        context: Context,
        device: CachedBluetoothDevice
    ): List<Preference>? {
        return ImmutableList.of<Preference>()
    }

    override fun getInvisibleProfilePreferenceKeys(
        context: Context,
        bluetoothDevice: BluetoothDevice
    ): Set<String> {
        return ImmutableSet.of()
    }

    override fun getDeviceSettingRepository(
        context: Context,
        bluetoothAdapter: BluetoothAdapter,
        scope: LifecycleCoroutineScope
    ): DeviceSettingRepository =
        DeviceSettingRepositoryImpl(context, bluetoothAdapter, scope, Dispatchers.IO)

    override fun getSpatialAudioInteractor(
        context: Context,
        audioManager: AudioManager,
        scope: LifecycleCoroutineScope
    ): SpatialAudioInteractor {
        return SpatialAudioInteractorImpl(
            context, audioManager,
            SpatializerInteractor(
                SpatializerRepositoryImpl(
                    getSpatializer(context),
                    Dispatchers.IO
                )
            ), scope, Dispatchers.IO)
    }

    override fun getDeviceDetailsFragmentFormatter(
        context: Context,
        fragment: SettingsPreferenceFragment,
        bluetoothAdapter: BluetoothAdapter,
        cachedDevice: CachedBluetoothDevice
    ): DeviceDetailsFragmentFormatter {
        return DeviceDetailsFragmentFormatterImpl(
            context,
            fragment,
            bluetoothAdapter,
            cachedDevice,
            Dispatchers.IO
        )
    }
}
