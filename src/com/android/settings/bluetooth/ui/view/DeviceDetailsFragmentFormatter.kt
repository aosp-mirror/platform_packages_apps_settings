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
import android.media.AudioManager
import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.bluetooth.ui.composable.Icon
import com.android.settings.bluetooth.ui.composable.MultiTogglePreferenceGroup
import com.android.settings.bluetooth.ui.layout.DeviceSettingLayout
import com.android.settings.bluetooth.ui.viewmodel.BluetoothDeviceDetailsViewModel
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.spa.preference.ComposePreference
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingConfigItemModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingStateModel
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spa.widget.preference.TwoTargetSwitchPreference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import com.android.settingslib.spa.widget.preference.Preference as SpaPreference


/** Handles device details fragment layout according to config. */
interface DeviceDetailsFragmentFormatter {
    /** Gets keys of visible preferences in built-in preference in xml. */
    fun getVisiblePreferenceKeysForMainPage(): List<String>?

    /** Updates device details fragment layout. */
    fun updateLayout()
}

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceDetailsFragmentFormatterImpl(
    private val context: Context,
    private val fragment: SettingsPreferenceFragment,
    bluetoothAdapter: BluetoothAdapter,
    private val cachedDevice: CachedBluetoothDevice
) : DeviceDetailsFragmentFormatter {
    private val repository =
        featureFactory.bluetoothFeatureProvider.getDeviceSettingRepository(
            context, bluetoothAdapter, fragment.lifecycleScope)
    private val spatialAudioInteractor =
        featureFactory.bluetoothFeatureProvider.getSpatialAudioInteractor(
            context, context.getSystemService(AudioManager::class.java), fragment.lifecycleScope)
    private val viewModel: BluetoothDeviceDetailsViewModel =
        ViewModelProvider(
                fragment,
                BluetoothDeviceDetailsViewModel.Factory(
                    repository,
                    spatialAudioInteractor,
                    cachedDevice,
                ))
            .get(BluetoothDeviceDetailsViewModel::class.java)

    override fun getVisiblePreferenceKeysForMainPage(): List<String>? = runBlocking {
        viewModel
            .getItems()
            ?.filterIsInstance<DeviceSettingConfigItemModel.BuiltinItem>()
            ?.mapNotNull { it.preferenceKey }
    }

    /** Updates bluetooth device details fragment layout. */
    override fun updateLayout() = runBlocking {
        val items = viewModel.getItems() ?: return@runBlocking
        val layout = viewModel.getLayout() ?: return@runBlocking
        val prefKeyToSettingId =
            items
                .filterIsInstance<DeviceSettingConfigItemModel.BuiltinItem>()
                .associateBy({ it.preferenceKey }, { it.settingId })

        val settingIdToXmlPreferences: MutableMap<Int, Preference> = HashMap()
        for (i in 0 until fragment.preferenceScreen.preferenceCount) {
            val pref = fragment.preferenceScreen.getPreference(i)
            prefKeyToSettingId[pref.key]?.let { id -> settingIdToXmlPreferences[id] = pref }
        }
        fragment.preferenceScreen.removeAll()

        for (row in items.indices) {
            val settingId = items[row].settingId
            if (settingIdToXmlPreferences.containsKey(settingId)) {
                fragment.preferenceScreen.addPreference(
                    settingIdToXmlPreferences[settingId]!!.apply { order = row })
            } else {
                val pref =
                    ComposePreference(context)
                        .apply {
                            key = getPreferenceKey(settingId)
                            order = row
                        }
                        .also { pref -> pref.setContent { buildPreference(layout, row) } }
                fragment.preferenceScreen.addPreference(pref)
            }
        }
    }

    @Composable
    private fun buildPreference(layout: DeviceSettingLayout, row: Int) {
        val contents by
            remember(row) {
                    layout.rows[row].settingIds.flatMapLatest { settingIds ->
                        if (settingIds.isEmpty()) {
                            flowOf(emptyList<DeviceSettingModel>())
                        } else {
                            combine(
                                settingIds.map { settingId ->
                                    viewModel.getDeviceSetting(cachedDevice, settingId)
                                }) {
                                    it.toList()
                                }
                        }
                    }
                }
                .collectAsStateWithLifecycle(initialValue = listOf())

        val settings = contents
        when (settings.size) {
            0 -> {}
            1 -> {
                when (val setting = settings[0]) {
                    is DeviceSettingModel.ActionSwitchPreference -> {
                        buildActionSwitchPreference(setting)
                    }
                    is DeviceSettingModel.MultiTogglePreference -> {
                        buildMultiTogglePreference(listOf(setting))
                    }
                    null -> {}
                    else -> {
                        Log.w(TAG, "Unknown preference type ${setting.id}, skip.")
                    }
                }
            }
            else -> {
                if (!settings.all { it is DeviceSettingModel.MultiTogglePreference }) {
                    return
                }
                buildMultiTogglePreference(
                    settings.filterIsInstance<DeviceSettingModel.MultiTogglePreference>())
            }
        }
    }

    @Composable
    private fun buildMultiTogglePreference(prefs: List<DeviceSettingModel.MultiTogglePreference>) {
        MultiTogglePreferenceGroup(prefs)
    }

    @Composable
    private fun buildActionSwitchPreference(model: DeviceSettingModel.ActionSwitchPreference) {
        if (model.switchState != null) {
            val switchPrefModel =
                object : SwitchPreferenceModel {
                    override val title = model.title
                    override val summary = { model.summary ?: "" }
                    override val checked = { model.switchState?.checked }
                    override val onCheckedChange = { newChecked: Boolean ->
                        model.updateState?.invoke(
                            DeviceSettingStateModel.ActionSwitchPreferenceState(newChecked))
                        Unit
                    }
                    override val icon = @Composable { deviceSettingIcon(model) }
                }
            if (model.intent != null) {
                TwoTargetSwitchPreference(switchPrefModel) { context.startActivity(model.intent) }
            } else {
                SwitchPreference(switchPrefModel)
            }
        } else {
            SpaPreference(
                object : PreferenceModel {
                    override val title = model.title
                    override val summary = { model.summary ?: "" }
                    override val onClick = {
                        model.intent?.let { context.startActivity(it) }
                        Unit
                    }
                    override val icon = @Composable { deviceSettingIcon(model) }
                })
        }
    }

    @Composable
    private fun deviceSettingIcon(model: DeviceSettingModel.ActionSwitchPreference) {
        model.icon?.let { icon ->
            Icon(icon, modifier = Modifier.size(SettingsDimension.itemIconSize))
        }
    }

    private fun getPreferenceKey(settingId: Int) = "DEVICE_SETTING_${settingId}"

    companion object {
        const val TAG = "DeviceDetailsFormatter"
    }
}
