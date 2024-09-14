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
import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.bluetooth.ui.composable.Icon
import com.android.settings.bluetooth.ui.composable.MultiTogglePreferenceGroup
import com.android.settings.bluetooth.ui.layout.DeviceSettingLayout
import com.android.settings.bluetooth.ui.model.DeviceSettingPreferenceModel
import com.android.settings.bluetooth.ui.model.FragmentTypeModel
import com.android.settings.bluetooth.ui.view.DeviceDetailsMoreSettingsFragment.Companion.KEY_DEVICE_ADDRESS
import com.android.settings.bluetooth.ui.viewmodel.BluetoothDeviceDetailsViewModel
import com.android.settings.core.SubSettingLauncher
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.spa.preference.ComposePreference
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingConfigItemModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingIcon
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.widget.preference.Preference as SpaPreference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spa.widget.preference.TwoTargetSwitchPreference
import com.android.settingslib.spa.widget.ui.Footer
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/** Handles device details fragment layout according to config. */
interface DeviceDetailsFragmentFormatter {
    /** Gets keys of visible preferences in built-in preference in xml. */
    fun getVisiblePreferenceKeys(fragmentType: FragmentTypeModel): List<String>?

    /** Updates device details fragment layout. */
    fun getInvisibleBluetoothProfiles(fragmentType: FragmentTypeModel): List<String>?

    /** Updates device details fragment layout. */
    fun updateLayout(fragmentType: FragmentTypeModel)

    /** Gets the menu items of the fragment. */
    fun getMenuItem(
        fragmentType: FragmentTypeModel
    ): Flow<DeviceSettingPreferenceModel.HelpPreference?>
}

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceDetailsFragmentFormatterImpl(
    private val context: Context,
    private val fragment: SettingsPreferenceFragment,
    bluetoothAdapter: BluetoothAdapter,
    private val cachedDevice: CachedBluetoothDevice,
    private val backgroundCoroutineContext: CoroutineContext,
) : DeviceDetailsFragmentFormatter {
    private val repository =
        featureFactory.bluetoothFeatureProvider.getDeviceSettingRepository(
            context,
            bluetoothAdapter,
            fragment.lifecycleScope,
        )
    private val spatialAudioInteractor =
        featureFactory.bluetoothFeatureProvider.getSpatialAudioInteractor(
            context,
            context.getSystemService(AudioManager::class.java),
            fragment.lifecycleScope,
        )
    private val viewModel: BluetoothDeviceDetailsViewModel =
        ViewModelProvider(
                fragment,
                BluetoothDeviceDetailsViewModel.Factory(
                    fragment.requireActivity().application,
                    repository,
                    spatialAudioInteractor,
                    cachedDevice,
                    backgroundCoroutineContext,
                ),
            )
            .get(BluetoothDeviceDetailsViewModel::class.java)

    override fun getVisiblePreferenceKeys(fragmentType: FragmentTypeModel): List<String>? =
        runBlocking {
            viewModel
                .getItems(fragmentType)
                ?.filterIsInstance<DeviceSettingConfigItemModel.BuiltinItem>()
                ?.map { it.preferenceKey }
        }

    override fun getInvisibleBluetoothProfiles(fragmentType: FragmentTypeModel): List<String>? =
        runBlocking {
            viewModel
                .getItems(fragmentType)
                ?.filterIsInstance<DeviceSettingConfigItemModel.BuiltinItem.BluetoothProfilesItem>()
                ?.first()
                ?.invisibleProfiles
        }

    /** Updates bluetooth device details fragment layout. */
    override fun updateLayout(fragmentType: FragmentTypeModel) = runBlocking {
        val items = viewModel.getItems(fragmentType) ?: return@runBlocking
        val layout = viewModel.getLayout(fragmentType) ?: return@runBlocking

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
                    settingIdToXmlPreferences[settingId]!!.apply { order = row }
                )
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
        // TODO(b/343317785): figure out how to remove the foot preference.
        fragment.preferenceScreen.addPreference(Preference(context).apply { order = 10000 })
    }

    override fun getMenuItem(
        fragmentType: FragmentTypeModel
    ): Flow<DeviceSettingPreferenceModel.HelpPreference?> = flow {
        val t = viewModel.getHelpItem(fragmentType)

        t?.let { item ->
            emitAll(
                viewModel.getDeviceSetting(cachedDevice, item.settingId).map {
                    it as? DeviceSettingPreferenceModel.HelpPreference
                }
            )
        } ?: emit(null)
    }

    @Composable
    private fun buildPreference(layout: DeviceSettingLayout, row: Int) {
        val contents by
            remember(row) {
                    layout.rows[row].columns.flatMapLatest { columns ->
                        if (columns.isEmpty()) {
                            flowOf(emptyList<DeviceSettingPreferenceModel>())
                        } else {
                            combine(
                                columns.map { column ->
                                    viewModel.getDeviceSetting(cachedDevice, column.settingId)
                                }
                            ) {
                                it.toList()
                            }
                        }
                    }
                }
                .collectAsStateWithLifecycle(initialValue = listOf())

        val highlighted by
            remember(row) {
                    layout.rows[row].columns.map { columns -> columns.any { it.highlighted } }
                }
                .collectAsStateWithLifecycle(initialValue = false)

        val settings = contents
        AnimatedVisibility(
            visible = settings.isNotEmpty(),
            enter = expandVertically(expandFrom = Alignment.Top),
            exit = shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            Box {
                Box(
                    modifier =
                    Modifier.matchParentSize()
                        .padding(16.dp, 0.dp, 8.dp, 0.dp)
                        .background(
                            color =
                            if (highlighted) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            },
                            shape = RoundedCornerShape(28.dp),
                        ),
                ) {}
                buildPreferences(settings)
            }
        }
    }

    @Composable
    fun buildPreferences(settings: List<DeviceSettingPreferenceModel?>) {
        when (settings.size) {
            0 -> {}
            1 -> {
                when (val setting = settings[0]) {
                    is DeviceSettingPreferenceModel.PlainPreference -> {
                        buildPlainPreference(setting)
                    }
                    is DeviceSettingPreferenceModel.SwitchPreference -> {
                        buildSwitchPreference(setting)
                    }
                    is DeviceSettingPreferenceModel.MultiTogglePreference -> {
                        buildMultiTogglePreference(listOf(setting))
                    }
                    is DeviceSettingPreferenceModel.FooterPreference -> {
                        buildFooterPreference(setting)
                    }
                    is DeviceSettingPreferenceModel.MoreSettingsPreference -> {
                        buildMoreSettingsPreference()
                    }
                    is DeviceSettingPreferenceModel.HelpPreference -> {}
                    null -> {}
                }
            }
            else -> {
                if (
                    !settings.all {
                        it is DeviceSettingPreferenceModel.MultiTogglePreference
                    }
                ) {
                    return
                }
                buildMultiTogglePreference(
                    settings.filterIsInstance<
                            DeviceSettingPreferenceModel.MultiTogglePreference
                            >()
                )
            }
        }
    }

    @Composable
    private fun buildMultiTogglePreference(
        prefs: List<DeviceSettingPreferenceModel.MultiTogglePreference>
    ) {
        MultiTogglePreferenceGroup(prefs)
    }

    @Composable
    private fun buildSwitchPreference(model: DeviceSettingPreferenceModel.SwitchPreference) {
        val switchPrefModel =
            object : SwitchPreferenceModel {
                override val title = model.title
                override val summary = { model.summary ?: "" }
                override val checked = { model.checked }
                override val onCheckedChange = { newChecked: Boolean ->
                    model.onCheckedChange(newChecked)
                }
                override val icon: (@Composable () -> Unit)?
                    get() {
                        if (model.icon == null) {
                            return null
                        }
                        return { deviceSettingIcon(model.icon) }
                    }
            }
        if (model.onPrimaryClick != null) {
            TwoTargetSwitchPreference(
                switchPrefModel,
                primaryOnClick = model.onPrimaryClick::invoke,
            )
        } else {
            SwitchPreference(switchPrefModel)
        }
    }

    @Composable
    private fun buildPlainPreference(model: DeviceSettingPreferenceModel.PlainPreference) {
        SpaPreference(
            object : PreferenceModel {
                override val title = model.title
                override val summary = { model.summary ?: "" }
                override val onClick = {
                    model.onClick?.invoke()
                    Unit
                }
                override val icon: (@Composable () -> Unit)?
                    get() {
                        if (model.icon == null) {
                            return null
                        }
                        return { deviceSettingIcon(model.icon) }
                    }
            }
        )
    }

    @Composable
    fun buildMoreSettingsPreference() {
        SpaPreference(
            object : PreferenceModel {
                override val title =
                    stringResource(R.string.bluetooth_device_more_settings_preference_title)
                override val summary = {
                    context.getString(R.string.bluetooth_device_more_settings_preference_summary)
                }
                override val onClick = {
                    SubSettingLauncher(context)
                        .setDestination(DeviceDetailsMoreSettingsFragment::class.java.name)
                        .setSourceMetricsCategory(fragment.getMetricsCategory())
                        .setArguments(
                            Bundle().apply { putString(KEY_DEVICE_ADDRESS, cachedDevice.address) }
                        )
                        .launch()
                }
                override val icon = @Composable { deviceSettingIcon(null) }
            }
        )
    }

    @Composable
    fun buildFooterPreference(model: DeviceSettingPreferenceModel.FooterPreference) {
        Footer(footerText = model.footerText)
    }

    @Composable
    private fun deviceSettingIcon(icon: DeviceSettingIcon?) {
        icon?.let { Icon(it, modifier = Modifier.size(SettingsDimension.itemIconSize)) }
    }

    private fun getPreferenceKey(settingId: Int) = "DEVICE_SETTING_${settingId}"

    companion object {
        const val TAG = "DeviceDetailsFormatter"
    }
}
