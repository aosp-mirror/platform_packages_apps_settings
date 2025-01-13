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

import android.app.ActivityOptions
import android.app.settings.SettingsEnums
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.bluetooth.BlockingPrefWithSliceController
import com.android.settings.bluetooth.BluetoothDetailsProfilesController
import com.android.settings.bluetooth.ui.composable.Icon
import com.android.settings.bluetooth.ui.composable.MultiTogglePreference
import com.android.settings.bluetooth.ui.layout.DeviceSettingLayout
import com.android.settings.bluetooth.ui.model.DeviceSettingPreferenceModel
import com.android.settings.bluetooth.ui.model.FragmentTypeModel
import com.android.settings.bluetooth.ui.view.DeviceDetailsMoreSettingsFragment.Companion.KEY_DEVICE_ADDRESS
import com.android.settings.bluetooth.ui.viewmodel.BluetoothDeviceDetailsViewModel
import com.android.settings.core.SubSettingLauncher
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.overlay.FeatureFactory
import com.android.settings.spa.preference.ComposePreference
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.bluetooth.devicesettings.DeviceSettingId
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingActionModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingConfigItemModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingIcon
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.core.lifecycle.LifecycleObserver
import com.android.settingslib.core.lifecycle.events.OnPause
import com.android.settingslib.core.lifecycle.events.OnStop
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.widget.preference.Preference as SpaPreference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spa.widget.preference.TwoTargetSwitchPreference
import com.android.settingslib.spa.widget.ui.Footer
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** Handles device details fragment layout according to config. */
interface DeviceDetailsFragmentFormatter {
    /** Updates device details fragment layout. */
    fun updateLayout(fragmentType: FragmentTypeModel)

    /** Gets the menu items of the fragment. */
    fun getMenuItem(
        fragmentType: FragmentTypeModel
    ): Flow<DeviceSettingPreferenceModel.HelpPreference?>
}

@FlowPreview
@OptIn(ExperimentalCoroutinesApi::class)
class DeviceDetailsFragmentFormatterImpl(
    private val context: Context,
    private val fragment: DashboardFragment,
    controllers: List<AbstractPreferenceController>,
    private val bluetoothAdapter: BluetoothAdapter,
    private val cachedDevice: CachedBluetoothDevice,
    private val backgroundCoroutineContext: CoroutineContext,
) : DeviceDetailsFragmentFormatter {
    private val metricsFeatureProvider = FeatureFactory.featureFactory.metricsFeatureProvider
    private val prefVisibility = mutableMapOf<String, MutableStateFlow<Boolean>>()
    private val prefVisibilityJobs = mutableListOf<Job>()
    private var isLoading = false
    private var prefKeyToController: Map<String, AbstractPreferenceController> =
        controllers.associateBy { it.preferenceKey }

    private val viewModel: BluetoothDeviceDetailsViewModel =
        ViewModelProvider(
            fragment,
            BluetoothDeviceDetailsViewModel.Factory(
                fragment.requireActivity().application,
                bluetoothAdapter,
                cachedDevice,
                backgroundCoroutineContext,
            ),
        )
            .get(BluetoothDeviceDetailsViewModel::class.java)

    /** Updates bluetooth device details fragment layout. */
    override fun updateLayout(fragmentType: FragmentTypeModel) {
        fragment.setLoading(true, false)
        isLoading = true
        fragment.lifecycleScope.launch { updateLayoutInternal(fragmentType) }
    }

    private suspend fun updateLayoutInternal(fragmentType: FragmentTypeModel) {
        val items = viewModel.getItems(fragmentType) ?: run {
            fragment.setLoading(false, false)
            return
        }
        val layout = viewModel.getLayout(fragmentType) ?: run {
            fragment.setLoading(false, false)
            return
        }

        val prefKeyToSettingId =
            items
                .filterIsInstance<DeviceSettingConfigItemModel.BuiltinItem>()
                .associateBy({ it.preferenceKey }, { it.settingId })

        val settingIdToXmlPreferences: MutableMap<Int, Preference> = HashMap()
        for (i in 0 until fragment.preferenceScreen.preferenceCount) {
            val pref = fragment.preferenceScreen.getPreference(i)
            prefKeyToSettingId[pref.key]?.let { id -> settingIdToXmlPreferences[id] = pref }
            if (pref.key !in prefKeyToSettingId) {
                getController(pref.key)?.let { disableController(it) }
            }
        }
        fragment.preferenceScreen.removeAll()
        for (job in prefVisibilityJobs) {
            job.cancel()
        }
        prefVisibilityJobs.clear()
        for (row in items.indices) {
            val settingItem = items[row]
            val settingId = settingItem.settingId
            if (settingIdToXmlPreferences.containsKey(settingId)) {
                val pref = settingIdToXmlPreferences[settingId]!!.apply { order = row }
                fragment.preferenceScreen.addPreference(pref)
            } else {
                val prefKey = getPreferenceKey(settingId)
                prefVisibilityJobs.add(
                    getDevicesSettingForRow(layout, row)
                        .onEach { logItemShown(prefKey, it.isNotEmpty()) }
                        .launchIn(fragment.lifecycleScope)
                )
                val pref =
                    ComposePreference(context)
                        .apply {
                            key = prefKey
                            order = row
                        }
                        .also { pref -> pref.setContent { buildPreference(layout, row, prefKey) } }
                fragment.preferenceScreen.addPreference(pref)
            }
        }
        // TODO(b/343317785): figure out how to remove the foot preference.
        fragment.preferenceScreen.addPreference(ComposePreference(context).apply {
            order = 10000
            isEnabled = false
            isSelectable = false
            setContent { Spacer(modifier = Modifier.height(1.dp)) }
        })

        for (row in items.indices) {
            val settingItem = items[row]
            val settingId = settingItem.settingId
            if (settingIdToXmlPreferences.containsKey(settingId)) {
                val pref = fragment.preferenceScreen.getPreference(row)
                if (settingId == DeviceSettingId.DEVICE_SETTING_ID_BLUETOOTH_PROFILES) {
                    (getController(pref.key) as? BluetoothDetailsProfilesController)?.run {
                        if (settingItem is DeviceSettingConfigItemModel.BuiltinItem.BluetoothProfilesItem) {
                            setInvisibleProfiles(settingItem.invisibleProfiles)
                            setHasExtraSpace(false)
                        }
                    }
                }
                getController(pref.key)?.displayPreference(fragment.preferenceScreen)
                logItemShown(pref.key, pref.isVisible)
            }
        }

        fragment.lifecycleScope.launch {
            if (isLoading) {
                fragment.setLoading(false, false)
                isLoading = false
            }
        }
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

    private fun getDevicesSettingForRow(
        layout: DeviceSettingLayout,
        row: Int,
    ): Flow<List<DeviceSettingPreferenceModel>> =
        layout.rows[row].columns.flatMapLatest { columns ->
            if (columns.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    columns.map { column ->
                        viewModel.getDeviceSetting(cachedDevice, column.settingId)
                    }
                ) {
                    it.toList().filterNotNull()
                }
            }
        }

    @Composable
    private fun buildPreference(layout: DeviceSettingLayout, row: Int, prefKey: String) {
        val contents by
        remember(row) { getDevicesSettingForRow(layout, row) }
            .collectAsStateWithLifecycle(initialValue = listOf())

        val highlighted by
        remember(row) {
            layout.rows[row].columns.map { columns -> columns.any { it.highlighted } }
        }
            .collectAsStateWithLifecycle(initialValue = false)

        val settings = contents
        AnimatedVisibility(visible = settings.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
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
                            )
                ) {}
                buildPreferences(settings, prefKey)
            }
        }
    }

    @Composable
    fun buildPreferences(settings: List<DeviceSettingPreferenceModel?>, prefKey: String) {
        when (settings.size) {
            0 -> {}
            1 -> {
                when (val setting = settings[0]) {
                    is DeviceSettingPreferenceModel.PlainPreference -> {
                        buildPlainPreference(setting, prefKey)
                    }
                    is DeviceSettingPreferenceModel.SwitchPreference -> {
                        buildSwitchPreference(setting, prefKey)
                    }
                    is DeviceSettingPreferenceModel.MultiTogglePreference -> {
                        buildMultiTogglePreference(setting, prefKey)
                    }
                    is DeviceSettingPreferenceModel.FooterPreference -> {
                        buildFooterPreference(setting)
                    }
                    is DeviceSettingPreferenceModel.MoreSettingsPreference -> {
                        buildMoreSettingsPreference(prefKey)
                    }
                    is DeviceSettingPreferenceModel.HelpPreference -> {}
                    null -> {}
                }
            }
            else -> {}
        }
    }

    @Composable
    private fun buildMultiTogglePreference(
        pref: DeviceSettingPreferenceModel.MultiTogglePreference,
        prefKey: String,
    ) {
        MultiTogglePreference(
            pref.copy(
                onSelectedChange = { newState ->
                    logItemClick(prefKey, newState)
                    pref.onSelectedChange(newState)
                }
            )
        )
    }

    @Composable
    private fun buildSwitchPreference(
        model: DeviceSettingPreferenceModel.SwitchPreference,
        prefKey: String,
    ) {
        val switchPrefModel =
            object : SwitchPreferenceModel {
                override val title = model.title
                override val summary = { model.summary ?: "" }
                override val checked = { model.checked }
                override val onCheckedChange = { newState: Boolean ->
                    logItemClick(prefKey, if (newState) EVENT_SWITCH_ON else EVENT_SWITCH_OFF)
                    model.onCheckedChange(newState)
                }
                override val changeable = { !model.disabled }
                override val icon: (@Composable () -> Unit)?
                    get() {
                        if (model.icon == null) {
                            return null
                        }
                        return { deviceSettingIcon(model.icon) }
                    }
            }
        if (model.action != null) {
            TwoTargetSwitchPreference(
                switchPrefModel,
                primaryOnClick = {
                    logItemClick(prefKey, EVENT_CLICK_PRIMARY)
                    triggerAction(model.action)
                },
                primaryEnabled = { !model.disabled },
            )
        } else {
            SwitchPreference(switchPrefModel)
        }
    }

    @Composable
    private fun buildPlainPreference(
        model: DeviceSettingPreferenceModel.PlainPreference,
        prefKey: String,
    ) {
        SpaPreference(
            object : PreferenceModel {
                override val title = model.title
                override val summary = { model.summary ?: "" }
                override val onClick = {
                    logItemClick(prefKey, EVENT_CLICK_PRIMARY)
                    model.action?.let { triggerAction(it) }
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
    fun buildMoreSettingsPreference(prefKey: String) {
        SpaPreference(
            object : PreferenceModel {
                override val title =
                    stringResource(R.string.bluetooth_device_more_settings_preference_title)
                override val summary = {
                    context.getString(R.string.bluetooth_device_more_settings_preference_summary)
                }
                override val onClick = {
                    logItemClick(prefKey, EVENT_CLICK_PRIMARY)
                    SubSettingLauncher(context)
                        .setDestination(DeviceDetailsMoreSettingsFragment::class.java.name)
                        .setSourceMetricsCategory(fragment.getMetricsCategory())
                        .setArguments(
                            Bundle().apply { putString(KEY_DEVICE_ADDRESS, cachedDevice.address) }
                        )
                        .launch()
                }
                override val icon =
                    @Composable {
                        deviceSettingIcon(
                            DeviceSettingIcon.ResourceIcon(R.drawable.ic_chevron_right_24dp)
                        )
                    }
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

    private fun logItemClick(preferenceKey: String, value: Int = 0) {
        logAction(preferenceKey, SettingsEnums.ACTION_BLUETOOTH_DEVICE_DETAILS_ITEM_CLICKED, value)
    }

    private fun logItemShown(preferenceKey: String, visible: Boolean) {
        if (!visible && !prefVisibility.containsKey(preferenceKey)) {
            return
        }
        prefVisibility
            .computeIfAbsent(preferenceKey) {
                MutableStateFlow(true).also { visibilityFlow ->
                    visibilityFlow
                        .onEach {
                            logAction(
                                preferenceKey,
                                SettingsEnums.ACTION_BLUETOOTH_DEVICE_DETAILS_ITEM_SHOWN,
                                if (it) EVENT_VISIBLE else EVENT_INVISIBLE,
                            )
                        }
                        .launchIn(fragment.lifecycleScope)
                }
            }
            .value = visible
    }

    private fun logAction(preferenceKey: String, action: Int, value: Int) {
        metricsFeatureProvider.action(SettingsEnums.PAGE_UNKNOWN, action, 0, preferenceKey, value)
    }

    private fun triggerAction(action: DeviceSettingActionModel) {
        when (action) {
            is DeviceSettingActionModel.IntentAction -> {
                action.intent.removeFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(action.intent)
            }
            is DeviceSettingActionModel.PendingIntentAction -> {
                val options =
                    ActivityOptions.makeBasic()
                        .setPendingIntentBackgroundActivityStartMode(
                            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
                        )
                action.pendingIntent.send(options.toBundle())
            }
        }
    }

    private fun getController(key: String): AbstractPreferenceController? {
        return prefKeyToController[key]
    }

    private fun disableController(controller: AbstractPreferenceController) {
        if (controller is LifecycleObserver) {
            fragment.settingsLifecycle.removeObserver(controller as LifecycleObserver)
        }

        if (controller is BlockingPrefWithSliceController) {
            // Make UiBlockListener finished, otherwise UI will flicker.
            controller.onChanged(null)
        }

        if (controller is OnPause) {
            (controller as OnPause).onPause()
        }

        if (controller is OnStop) {
            (controller as OnStop).onStop()
        }
    }

    private fun getPreferenceKey(settingId: Int) = "DEVICE_SETTING_${settingId}"

    private companion object {
        const val TAG = "DeviceDetailsFormatter"
        const val EVENT_SWITCH_OFF = 0
        const val EVENT_SWITCH_ON = 1
        const val EVENT_CLICK_PRIMARY = 2
        const val EVENT_INVISIBLE = 0
        const val EVENT_VISIBLE = 1
    }
}
