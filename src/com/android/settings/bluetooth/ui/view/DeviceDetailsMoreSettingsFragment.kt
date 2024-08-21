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

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import com.android.settings.R
import com.android.settings.bluetooth.BluetoothDetailsProfilesController
import com.android.settings.bluetooth.Utils
import com.android.settings.bluetooth.ui.model.FragmentTypeModel
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.bluetooth.LocalBluetoothManager
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.core.lifecycle.LifecycleObserver

class DeviceDetailsMoreSettingsFragment : DashboardFragment() {
    private lateinit var formatter: DeviceDetailsFragmentFormatter
    private lateinit var localBluetoothManager: LocalBluetoothManager
    private lateinit var cachedDevice: CachedBluetoothDevice

    // TODO(b/343317785): add metrics category
    override fun getMetricsCategory(): Int = 0

    override fun getPreferenceScreenResId(): Int {
        return R.xml.bluetooth_device_more_settings_fragment
    }

    override fun addPreferenceController(controller: AbstractPreferenceController) {
        val keys: List<String>? =
            formatter.getVisiblePreferenceKeys(FragmentTypeModel.DeviceDetailsMoreSettingsFragment)
        val lifecycle = settingsLifecycle
        if (keys == null || keys.contains(controller.preferenceKey)) {
            super.addPreferenceController(controller)
        } else if (controller is LifecycleObserver) {
            lifecycle.removeObserver((controller as LifecycleObserver))
        }
    }

    private fun getCachedDevice(): CachedBluetoothDevice? {
        val bluetoothAddress = arguments?.getString(KEY_DEVICE_ADDRESS) ?: return null
        localBluetoothManager = Utils.getLocalBtManager(context) ?: return null
        val remoteDevice: BluetoothDevice =
            localBluetoothManager.bluetoothAdapter.getRemoteDevice(bluetoothAddress) ?: return null
        return Utils.getLocalBtManager(context).cachedDeviceManager.findDevice(remoteDevice)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        formatter.updateLayout(FragmentTypeModel.DeviceDetailsMoreSettingsFragment)
    }

    override fun createPreferenceControllers(context: Context): List<AbstractPreferenceController> {
        val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
        cachedDevice =
            getCachedDevice()
                ?: run {
                    finish()
                    return emptyList()
                }
        formatter =
            featureFactory.bluetoothFeatureProvider.getDeviceDetailsFragmentFormatter(
                requireContext(), this, bluetoothManager.adapter, cachedDevice)
        return listOf(
            BluetoothDetailsProfilesController(
                context, this, localBluetoothManager, cachedDevice, settingsLifecycle))
    }

    override fun getLogTag(): String = TAG

    companion object {
        const val TAG: String = "DeviceMoreSettingsFrg"
        const val KEY_DEVICE_ADDRESS: String = "device_address"
    }
}
