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

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioDeviceAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.bluetooth.LeAudioProfile
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingStateModel
import com.android.settingslib.media.data.repository.SpatializerRepository
import com.android.settingslib.media.domain.interactor.SpatializerInteractor
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SpatialAudioInteractorTest {
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var audioManager: AudioManager
    @Mock private lateinit var cachedDevice: CachedBluetoothDevice
    @Mock private lateinit var bluetoothDevice: BluetoothDevice
    @Mock private lateinit var spatializerRepository: SpatializerRepository
    @Mock private lateinit var leAudioProfile: LeAudioProfile

    private lateinit var underTest: SpatialAudioInteractor
    private val testScope = TestScope()

    @Before
    fun setUp() {
        val context = spy(ApplicationProvider.getApplicationContext<Context>())
        `when`(cachedDevice.device).thenReturn(bluetoothDevice)
        `when`(cachedDevice.address).thenReturn(BLUETOOTH_ADDRESS)
        `when`(leAudioProfile.profileId).thenReturn(BluetoothProfile.LE_AUDIO)
        underTest =
            SpatialAudioInteractorImpl(
                context,
                audioManager,
                SpatializerInteractor(spatializerRepository),
                testScope.backgroundScope,
                testScope.testScheduler)
    }

    @Test
    fun getDeviceSetting_noAudioProfile_returnNull() {
        testScope.runTest {
            val setting = getLatestValue(underTest.getDeviceSetting(cachedDevice))

            assertThat(setting).isNull()
            verifyNoInteractions(spatializerRepository)
        }
    }

    @Test
    fun getDeviceSetting_audioProfileNotEnabled_returnNull() {
        testScope.runTest {
            `when`(cachedDevice.profiles).thenReturn(listOf(leAudioProfile))
            `when`(leAudioProfile.isEnabled(bluetoothDevice)).thenReturn(false)

            val setting = getLatestValue(underTest.getDeviceSetting(cachedDevice))

            assertThat(setting).isNull()
            verifyNoInteractions(spatializerRepository)
        }
    }

    @Test
    fun getDeviceSetting_spatialAudioNotSupported_returnNull() {
        testScope.runTest {
            `when`(cachedDevice.profiles).thenReturn(listOf(leAudioProfile))
            `when`(leAudioProfile.isEnabled(bluetoothDevice)).thenReturn(true)
            `when`(
                    spatializerRepository.isSpatialAudioAvailableForDevice(
                        BLE_AUDIO_DEVICE_ATTRIBUTES))
                .thenReturn(false)

            val setting = getLatestValue(underTest.getDeviceSetting(cachedDevice))

            assertThat(setting).isNull()
        }
    }

    @Test
    fun getDeviceSetting_spatialAudioSupported_returnTwoToggles() {
        testScope.runTest {
            `when`(cachedDevice.profiles).thenReturn(listOf(leAudioProfile))
            `when`(leAudioProfile.isEnabled(bluetoothDevice)).thenReturn(true)
            `when`(
                    spatializerRepository.isSpatialAudioAvailableForDevice(
                        BLE_AUDIO_DEVICE_ATTRIBUTES))
                .thenReturn(true)
            `when`(
                    spatializerRepository.isHeadTrackingAvailableForDevice(
                        BLE_AUDIO_DEVICE_ATTRIBUTES))
                .thenReturn(false)
            `when`(spatializerRepository.getSpatialAudioCompatibleDevices())
                .thenReturn(listOf(BLE_AUDIO_DEVICE_ATTRIBUTES))
            `when`(spatializerRepository.isHeadTrackingEnabled(BLE_AUDIO_DEVICE_ATTRIBUTES))
                .thenReturn(false)

            val setting =
                getLatestValue(underTest.getDeviceSetting(cachedDevice))
                    as DeviceSettingModel.MultiTogglePreference

            assertThat(setting).isNotNull()
            assertThat(setting.toggles.size).isEqualTo(2)
            assertThat(setting.state.selectedIndex).isEqualTo(1)
        }
    }

    @Test
    fun getDeviceSetting_headTrackingSupported_returnThreeToggles() {
        testScope.runTest {
            `when`(cachedDevice.profiles).thenReturn(listOf(leAudioProfile))
            `when`(leAudioProfile.isEnabled(bluetoothDevice)).thenReturn(true)
            `when`(
                    spatializerRepository.isSpatialAudioAvailableForDevice(
                        BLE_AUDIO_DEVICE_ATTRIBUTES))
                .thenReturn(true)
            `when`(
                    spatializerRepository.isHeadTrackingAvailableForDevice(
                        BLE_AUDIO_DEVICE_ATTRIBUTES))
                .thenReturn(true)
            `when`(spatializerRepository.getSpatialAudioCompatibleDevices())
                .thenReturn(listOf(BLE_AUDIO_DEVICE_ATTRIBUTES))
            `when`(spatializerRepository.isHeadTrackingEnabled(BLE_AUDIO_DEVICE_ATTRIBUTES))
                .thenReturn(true)

            val setting =
                getLatestValue(underTest.getDeviceSetting(cachedDevice))
                    as DeviceSettingModel.MultiTogglePreference

            assertThat(setting).isNotNull()
            assertThat(setting.toggles.size).isEqualTo(3)
            assertThat(setting.state.selectedIndex).isEqualTo(2)
        }
    }

    @Test
    fun getDeviceSetting_updateState_enableSpatialAudio() {
        testScope.runTest {
            `when`(cachedDevice.profiles).thenReturn(listOf(leAudioProfile))
            `when`(leAudioProfile.isEnabled(bluetoothDevice)).thenReturn(true)
            `when`(
                    spatializerRepository.isSpatialAudioAvailableForDevice(
                        BLE_AUDIO_DEVICE_ATTRIBUTES))
                .thenReturn(true)
            `when`(
                    spatializerRepository.isHeadTrackingAvailableForDevice(
                        BLE_AUDIO_DEVICE_ATTRIBUTES))
                .thenReturn(true)
            `when`(spatializerRepository.getSpatialAudioCompatibleDevices()).thenReturn(listOf())
            `when`(spatializerRepository.isHeadTrackingEnabled(BLE_AUDIO_DEVICE_ATTRIBUTES))
                .thenReturn(false)

            val setting =
                getLatestValue(underTest.getDeviceSetting(cachedDevice))
                    as DeviceSettingModel.MultiTogglePreference
            setting.updateState(DeviceSettingStateModel.MultiTogglePreferenceState(2))
            runCurrent()

            assertThat(setting).isNotNull()
            verify(spatializerRepository, times(1))
                .addSpatialAudioCompatibleDevice(BLE_AUDIO_DEVICE_ATTRIBUTES)
        }
    }

    @Test
    fun getDeviceSetting_updateState_enableHeadTracking() {
        testScope.runTest {
            `when`(cachedDevice.profiles).thenReturn(listOf(leAudioProfile))
            `when`(leAudioProfile.isEnabled(bluetoothDevice)).thenReturn(true)
            `when`(
                spatializerRepository.isSpatialAudioAvailableForDevice(
                    BLE_AUDIO_DEVICE_ATTRIBUTES))
                .thenReturn(true)
            `when`(
                spatializerRepository.isHeadTrackingAvailableForDevice(
                    BLE_AUDIO_DEVICE_ATTRIBUTES))
                .thenReturn(true)
            `when`(spatializerRepository.getSpatialAudioCompatibleDevices()).thenReturn(listOf())
            `when`(spatializerRepository.isHeadTrackingEnabled(BLE_AUDIO_DEVICE_ATTRIBUTES))
                .thenReturn(false)

            val setting =
                getLatestValue(underTest.getDeviceSetting(cachedDevice))
                    as DeviceSettingModel.MultiTogglePreference
            setting.updateState(DeviceSettingStateModel.MultiTogglePreferenceState(2))
            runCurrent()

            assertThat(setting).isNotNull()
            verify(spatializerRepository, times(1))
                .addSpatialAudioCompatibleDevice(BLE_AUDIO_DEVICE_ATTRIBUTES)
            verify(spatializerRepository, times(1))
                .setHeadTrackingEnabled(BLE_AUDIO_DEVICE_ATTRIBUTES, true)
        }
    }

    private fun getLatestValue(deviceSettingFlow: Flow<DeviceSettingModel?>): DeviceSettingModel? {
        var latestValue: DeviceSettingModel? = null
        deviceSettingFlow.onEach { latestValue = it }.launchIn(testScope.backgroundScope)
        testScope.runCurrent()
        return latestValue
    }

    private companion object {
        const val BLUETOOTH_ADDRESS = "12:34:56:78:12:34"
        val BLE_AUDIO_DEVICE_ATTRIBUTES =
            AudioDeviceAttributes(
                AudioDeviceAttributes.ROLE_OUTPUT,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                BLUETOOTH_ADDRESS,
            )
    }
}
