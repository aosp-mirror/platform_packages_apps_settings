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

package com.android.settings.bluetooth.domain.interactor

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.android.settings.R
import com.android.settingslib.bluetooth.BluetoothUtils
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.bluetooth.devicesettings.DeviceSettingId
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingIcon
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingStateModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.ToggleModel
import com.android.settingslib.media.domain.interactor.SpatializerInteractor
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Provides device setting for spatial audio. */
interface SpatialAudioInteractor {
    /** Gets device setting for spatial audio */
    fun getDeviceSetting(
        cachedDevice: CachedBluetoothDevice,
    ): Flow<DeviceSettingModel?>
}

class SpatialAudioInteractorImpl(
    private val context: Context,
    private val audioManager: AudioManager,
    private val spatializerInteractor: SpatializerInteractor,
    private val coroutineScope: CoroutineScope,
    private val backgroundCoroutineContext: CoroutineContext,
) : SpatialAudioInteractor {
    private val spatialAudioOffToggle =
        ToggleModel(
            context.getString(R.string.spatial_audio_multi_toggle_off),
            DeviceSettingIcon.ResourceIcon(R.drawable.ic_spatial_audio_off))
    private val spatialAudioOnToggle =
        ToggleModel(
            context.getString(R.string.spatial_audio_multi_toggle_on),
            DeviceSettingIcon.ResourceIcon(R.drawable.ic_spatial_audio))
    private val headTrackingOnToggle =
        ToggleModel(
            context.getString(R.string.spatial_audio_multi_toggle_head_tracking_on),
            DeviceSettingIcon.ResourceIcon(R.drawable.ic_head_tracking))
    private val changes = MutableSharedFlow<Unit>()

    override fun getDeviceSetting(
        cachedDevice: CachedBluetoothDevice,
    ): Flow<DeviceSettingModel?> =
        changes
            .onStart { emit(Unit) }
            .map { getSpatialAudioDeviceSettingModel(cachedDevice) }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), initialValue = null)

    private suspend fun getSpatialAudioDeviceSettingModel(
        cachedDevice: CachedBluetoothDevice,
    ): DeviceSettingModel? {
        // TODO(b/343317785): use audio repository instead of calling AudioManager directly.
        Log.i(TAG, "CachedDevice: $cachedDevice profiles: ${cachedDevice.profiles}")
        val attributes =
            BluetoothUtils.getAudioDeviceAttributesForSpatialAudio(
                cachedDevice, audioManager.getBluetoothAudioDeviceCategory(cachedDevice.address))
                ?: run {
                    Log.i(TAG, "No audio profiles in cachedDevice: ${cachedDevice.address}.")
                    return null
                }

        Log.i(TAG, "Audio device attributes for ${cachedDevice.address}: $attributes.")
        val spatialAudioAvailable = spatializerInteractor.isSpatialAudioAvailable(attributes)
        if (!spatialAudioAvailable) {
            Log.i(TAG, "Spatial audio is not available for ${cachedDevice.address}")
            return null
        }
        val headTrackingAvailable =
            spatialAudioAvailable && spatializerInteractor.isHeadTrackingAvailable(attributes)
        val toggles =
            if (headTrackingAvailable) {
                listOf(spatialAudioOffToggle, spatialAudioOnToggle, headTrackingOnToggle)
            } else {
                listOf(spatialAudioOffToggle, spatialAudioOnToggle)
            }
        val spatialAudioEnabled = spatializerInteractor.isSpatialAudioEnabled(attributes)
        val headTrackingEnabled =
            spatialAudioEnabled && spatializerInteractor.isHeadTrackingEnabled(attributes)

        val activeIndex =
            when {
                headTrackingEnabled -> INDEX_HEAD_TRACKING_ENABLED
                spatialAudioEnabled -> INDEX_SPATIAL_AUDIO_ON
                else -> INDEX_SPATIAL_AUDIO_OFF
            }
        Log.i(
            TAG,
            "Head tracking available: $headTrackingAvailable, " +
                "spatial audio enabled: $spatialAudioEnabled, " +
                "head tracking enabled: $headTrackingEnabled")
        return DeviceSettingModel.MultiTogglePreference(
            cachedDevice = cachedDevice,
            id = DeviceSettingId.DEVICE_SETTING_ID_SPATIAL_AUDIO_MULTI_TOGGLE,
            title = context.getString(R.string.spatial_audio_multi_toggle_title),
            toggles = toggles,
            isActive = spatialAudioEnabled,
            state = DeviceSettingStateModel.MultiTogglePreferenceState(activeIndex),
            isAllowedChangingState = true,
            updateState = { newState ->
                coroutineScope.launch(backgroundCoroutineContext) {
                    Log.i(TAG, "Update spatial audio state: $newState")
                    when (newState.selectedIndex) {
                        INDEX_SPATIAL_AUDIO_OFF -> {
                            spatializerInteractor.setSpatialAudioEnabled(attributes, false)
                        }
                        INDEX_SPATIAL_AUDIO_ON -> {
                            spatializerInteractor.setSpatialAudioEnabled(attributes, true)
                            spatializerInteractor.setHeadTrackingEnabled(attributes, false)
                        }
                        INDEX_HEAD_TRACKING_ENABLED -> {
                            spatializerInteractor.setSpatialAudioEnabled(attributes, true)
                            spatializerInteractor.setHeadTrackingEnabled(attributes, true)
                        }
                    }
                    changes.emit(Unit)
                }
            })
    }

    companion object {
        private const val TAG = "SpatialAudioInteractorImpl"
        private const val INDEX_SPATIAL_AUDIO_OFF = 0
        private const val INDEX_SPATIAL_AUDIO_ON = 1
        private const val INDEX_HEAD_TRACKING_ENABLED = 2
    }
}
