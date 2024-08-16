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

package com.android.settings.bluetooth.ui.view

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import androidx.fragment.app.FragmentActivity
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.test.core.app.ApplicationProvider
import com.android.settings.bluetooth.domain.interactor.SpatialAudioInteractor
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.bluetooth.devicesettings.DeviceSettingId
import com.android.settingslib.bluetooth.devicesettings.data.repository.DeviceSettingRepository
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingConfigItemModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingConfigModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingIcon
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingStateModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.ToggleModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper.shadowMainLooper

@RunWith(RobolectricTestRunner::class)
class DeviceDetailsFragmentFormatterTest {
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var cachedDevice: CachedBluetoothDevice
    @Mock private lateinit var bluetoothAdapter: BluetoothAdapter
    @Mock private lateinit var repository: DeviceSettingRepository
    @Mock private lateinit var spatialAudioInteractor: SpatialAudioInteractor

    private lateinit var fragment: TestFragment
    private lateinit var underTest: DeviceDetailsFragmentFormatter
    private lateinit var featureFactory: FakeFeatureFactory
    private val testScope = TestScope()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        featureFactory = FakeFeatureFactory.setupForTest()
        `when`(
                featureFactory.bluetoothFeatureProvider.getDeviceSettingRepository(
                    eq(context), eq(bluetoothAdapter), any()))
            .thenReturn(repository)
        `when`(
            featureFactory.bluetoothFeatureProvider.getSpatialAudioInteractor(
                eq(context), any(AudioManager::class.java), any()))
            .thenReturn(spatialAudioInteractor)
        val fragmentActivity = Robolectric.setupActivity(FragmentActivity::class.java)
        assertThat(fragmentActivity.applicationContext).isNotNull()
        fragment = TestFragment(context)
        fragmentActivity.supportFragmentManager.beginTransaction().add(fragment, null).commit()
        shadowMainLooper().idle()

        fragment.preferenceScreen.run {
            addPreference(Preference(context).apply { key = "bluetooth_device_header" })
            addPreference(Preference(context).apply { key = "action_buttons" })
            addPreference(Preference(context).apply { key = "keyboard_settings" })
        }

        underTest =
            DeviceDetailsFragmentFormatterImpl(context, fragment, bluetoothAdapter, cachedDevice)
    }

    @Test
    fun getVisiblePreferenceKeysForMainPage_hasConfig_returnList() {
        testScope.runTest {
            `when`(repository.getDeviceSettingsConfig(cachedDevice))
                .thenReturn(
                    DeviceSettingConfigModel(
                        listOf(
                            DeviceSettingConfigItemModel.BuiltinItem(
                                DeviceSettingId.DEVICE_SETTING_ID_HEADER,
                                "bluetooth_device_header"),
                            DeviceSettingConfigItemModel.BuiltinItem(
                                DeviceSettingId.DEVICE_SETTING_ID_ACTION_BUTTONS, "action_buttons"),
                        ),
                        listOf(),
                        "footer"))

            val keys = underTest.getVisiblePreferenceKeysForMainPage()

            assertThat(keys).containsExactly("bluetooth_device_header", "action_buttons")
        }
    }

    @Test
    fun getVisiblePreferenceKeysForMainPage_noConfig_returnNull() {
        testScope.runTest {
            `when`(repository.getDeviceSettingsConfig(cachedDevice)).thenReturn(null)

            val keys = underTest.getVisiblePreferenceKeysForMainPage()

            assertThat(keys).isNull()
        }
    }

    @Test
    fun updateLayout_configIsNull_notChange() {
        testScope.runTest {
            `when`(repository.getDeviceSettingsConfig(cachedDevice)).thenReturn(null)

            underTest.updateLayout()

            assertThat(getDisplayedPreferences().map { it.key })
                .containsExactly("bluetooth_device_header", "action_buttons", "keyboard_settings")
        }
    }

    @Test
    fun updateLayout_itemsNotInConfig_hide() {
        testScope.runTest {
            `when`(repository.getDeviceSettingsConfig(cachedDevice))
                .thenReturn(
                    DeviceSettingConfigModel(
                        listOf(
                            DeviceSettingConfigItemModel.BuiltinItem(
                                DeviceSettingId.DEVICE_SETTING_ID_HEADER,
                                "bluetooth_device_header"),
                            DeviceSettingConfigItemModel.BuiltinItem(
                                DeviceSettingId.DEVICE_SETTING_ID_KEYBOARD_SETTINGS,
                                "keyboard_settings"),
                        ),
                        listOf(),
                        "footer"))

            underTest.updateLayout()

            assertThat(getDisplayedPreferences().map { it.key })
                .containsExactly("bluetooth_device_header", "keyboard_settings")
        }
    }

    @Test
    fun updateLayout_newItems_displayNewItems() {
        testScope.runTest {
            `when`(repository.getDeviceSettingsConfig(cachedDevice))
                .thenReturn(
                    DeviceSettingConfigModel(
                        listOf(
                            DeviceSettingConfigItemModel.BuiltinItem(
                                DeviceSettingId.DEVICE_SETTING_ID_HEADER,
                                "bluetooth_device_header"),
                            DeviceSettingConfigItemModel.AppProvidedItem(
                                DeviceSettingId.DEVICE_SETTING_ID_ANC),
                            DeviceSettingConfigItemModel.BuiltinItem(
                                DeviceSettingId.DEVICE_SETTING_ID_KEYBOARD_SETTINGS,
                                "keyboard_settings"),
                        ),
                        listOf(),
                        "footer"))
            `when`(repository.getDeviceSetting(cachedDevice, DeviceSettingId.DEVICE_SETTING_ID_ANC))
                .thenReturn(
                    flowOf(
                        DeviceSettingModel.MultiTogglePreference(
                            cachedDevice,
                            DeviceSettingId.DEVICE_SETTING_ID_ANC,
                            "title",
                            toggles =
                                listOf(
                                    ToggleModel(
                                        "", DeviceSettingIcon.BitmapIcon(
                                            Bitmap.createBitmap(
                                                1,
                                                1,
                                                Bitmap.Config.ARGB_8888
                                            )
                                        )
                                    )
                                ),
                            isActive = true,
                            state = DeviceSettingStateModel.MultiTogglePreferenceState(0),
                            isAllowedChangingState = true,
                            updateState = {})))

            underTest.updateLayout()

            assertThat(getDisplayedPreferences().map { it.key })
                .containsExactly(
                    "bluetooth_device_header",
                    "DEVICE_SETTING_${DeviceSettingId.DEVICE_SETTING_ID_ANC}",
                    "keyboard_settings")
        }
    }

    private fun getDisplayedPreferences(): List<Preference> {
        val prefs = mutableListOf<Preference>()
        for (i in 0..<fragment.preferenceScreen.preferenceCount) {
            prefs.add(fragment.preferenceScreen.getPreference(i))
        }
        return prefs
    }

    class TestFragment(context: Context) : DashboardFragment() {
        private val mPreferenceManager: PreferenceManager = PreferenceManager(context)

        init {
            mPreferenceManager.setPreferences(mPreferenceManager.createPreferenceScreen(context))
        }

        public override fun getPreferenceScreenResId(): Int = 0

        override fun getLogTag(): String = "TestLogTag"

        override fun getPreferenceScreen(): PreferenceScreen {
            return mPreferenceManager.preferenceScreen
        }

        override fun getMetricsCategory(): Int = 0

        override fun getPreferenceManager(): PreferenceManager {
            return mPreferenceManager
        }
    }

    private companion object {}
}
