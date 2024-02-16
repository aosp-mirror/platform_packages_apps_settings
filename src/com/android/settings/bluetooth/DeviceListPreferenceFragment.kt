/*
 * Copyright (C) 2023 The Android Open Source Project
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
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Bundle
import android.os.SystemProperties
import android.text.BidiFormatter
import android.util.Log
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import com.android.settings.R
import com.android.settings.dashboard.RestrictedDashboardFragment
import com.android.settingslib.bluetooth.BluetoothCallback
import com.android.settingslib.bluetooth.BluetoothDeviceFilter
import com.android.settingslib.bluetooth.BluetoothUtils
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager
import com.android.settingslib.bluetooth.LocalBluetoothManager
import com.android.settingslib.flags.Flags
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Parent class for settings fragments that contain a list of Bluetooth devices.
 *
 * @see DevicePickerFragment
 *
 * TODO: Refactor this fragment
 */
abstract class DeviceListPreferenceFragment(restrictedKey: String?) :
    RestrictedDashboardFragment(restrictedKey), BluetoothCallback {

    enum class ScanType {
        CLASSIC, LE
    }

    private var scanType = ScanType.CLASSIC
    private var filter: BluetoothDeviceFilter.Filter = BluetoothDeviceFilter.ALL_FILTER
    private var leScanFilters: List<ScanFilter>? = null

    @JvmField
    @VisibleForTesting
    var mScanEnabled = false

    @JvmField
    var mSelectedDevice: BluetoothDevice? = null

    @JvmField
    var mBluetoothAdapter: BluetoothAdapter? = null

    @JvmField
    var mLocalManager: LocalBluetoothManager? = null

    @JvmField
    var mCachedDeviceManager: CachedBluetoothDeviceManager? = null

    @JvmField
    @VisibleForTesting
    var mDeviceListGroup: PreferenceGroup? = null

    @VisibleForTesting
    val devicePreferenceMap =
        ConcurrentHashMap<CachedBluetoothDevice, BluetoothDevicePreference>()

    @JvmField
    val mSelectedList: MutableList<BluetoothDevice> = ArrayList()

    @VisibleForTesting
    var lifecycleScope: CoroutineScope? = null

    private var showDevicesWithoutNames = false

    protected fun setFilter(filterType: Int) {
        this.scanType = ScanType.CLASSIC
        this.filter = BluetoothDeviceFilter.getFilter(filterType)
    }

    /**
     * Sets the bluetooth device scanning filter with [ScanFilter]s. It will change to start
     * [BluetoothLeScanner] which will scan BLE device only.
     *
     * @param leScanFilters list of settings to filter scan result
     */
    fun setFilter(leScanFilters: List<ScanFilter>?) {
        this.scanType = ScanType.LE
        this.leScanFilters = leScanFilters
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mLocalManager = Utils.getLocalBtManager(activity)
        if (mLocalManager == null) {
            Log.e(TAG, "Bluetooth is not supported on this device")
            return
        }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        mCachedDeviceManager = mLocalManager!!.cachedDeviceManager
        showDevicesWithoutNames = SystemProperties.getBoolean(
            BLUETOOTH_SHOW_DEVICES_WITHOUT_NAMES_PROPERTY, false
        )
        initPreferencesFromPreferenceScreen()
        mDeviceListGroup = findPreference<Preference>(deviceListKey) as PreferenceCategory
    }

    /** find and update preference that already existed in preference screen  */
    protected abstract fun initPreferencesFromPreferenceScreen()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope = viewLifecycleOwner.lifecycleScope
    }

    override fun onStart() {
        super.onStart()
        if (mLocalManager == null || isUiRestricted) return
        mLocalManager!!.foregroundActivity = activity
        mLocalManager!!.eventManager.registerCallback(this)
    }

    override fun onStop() {
        super.onStop()
        if (mLocalManager == null || isUiRestricted) {
            return
        }
        removeAllDevices()
        mLocalManager!!.foregroundActivity = null
        mLocalManager!!.eventManager.unregisterCallback(this)
    }

    fun removeAllDevices() {
        devicePreferenceMap.clear()
        mDeviceListGroup!!.removeAll()
    }

    @JvmOverloads
    fun addCachedDevices(filterForCachedDevices: BluetoothDeviceFilter.Filter? = null) {
        lifecycleScope?.launch {
            withContext(Dispatchers.Default) {
                mCachedDeviceManager!!.cachedDevicesCopy
                    .filter {
                        filterForCachedDevices == null || filterForCachedDevices.matches(it.device)
                    }
                    .forEach(::onDeviceAdded)
            }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (KEY_BT_SCAN == preference.key) {
            startScanning()
            return true
        }
        if (preference is BluetoothDevicePreference) {
            val device = preference.cachedDevice.device
            mSelectedDevice = device
            mSelectedList.add(device)
            onDevicePreferenceClick(preference)
            return true
        }
        return super.onPreferenceTreeClick(preference)
    }

    protected open fun onDevicePreferenceClick(btPreference: BluetoothDevicePreference) {
        btPreference.onClicked()
    }

    override fun onDeviceAdded(cachedDevice: CachedBluetoothDevice) {
        lifecycleScope?.launch {
            addDevice(cachedDevice)
        }
    }

    private suspend fun addDevice(cachedDevice: CachedBluetoothDevice) =
        withContext(Dispatchers.Default) {
            if (mBluetoothAdapter!!.state != BluetoothAdapter.STATE_ON) {
                // Prevent updates while the list shows one of the state messages
                return@withContext
            }
            // LE filters was already applied at scan time. We just need to check if the classic
            // filter matches
            if (scanType == ScanType.LE
                || (scanType == ScanType.CLASSIC && filter.matches(cachedDevice.device) == true)) {
                createDevicePreference(cachedDevice)
            }
        }

    private suspend fun createDevicePreference(cachedDevice: CachedBluetoothDevice) {
        if (mDeviceListGroup == null) {
            Log.w(
                TAG,
                "Trying to create a device preference before the list group/category exists!",
            )
            return
        }
        if (Flags.enableHideExclusivelyManagedBluetoothDevice()) {
            if (cachedDevice.device.bondState == BluetoothDevice.BOND_BONDED
                && BluetoothUtils.isExclusivelyManagedBluetoothDevice(
                    prefContext, cachedDevice.device)) {
                Log.d(TAG, "Trying to create preference for a exclusively managed device")
                return
            }
        }
        // Only add device preference when it's not found in the map and there's no other state
        // message showing in the list
        val preference = devicePreferenceMap.computeIfAbsent(cachedDevice) {
            BluetoothDevicePreference(
                prefContext,
                cachedDevice,
                showDevicesWithoutNames,
                BluetoothDevicePreference.SortType.TYPE_FIFO,
            ).apply {
                key = cachedDevice.device.address
                //Set hideSecondTarget is true if it's bonded device.
                hideSecondTarget(true)
            }
        }
        withContext(Dispatchers.Main) {
            mDeviceListGroup!!.addPreference(preference)
            initDevicePreference(preference)
        }
    }

    protected open fun initDevicePreference(preference: BluetoothDevicePreference?) {
        // Does nothing by default
    }

    @VisibleForTesting
    fun updateFooterPreference(myDevicePreference: Preference) {
        val bidiFormatter = BidiFormatter.getInstance()
        myDevicePreference.title = getString(
            R.string.bluetooth_footer_mac_message,
            bidiFormatter.unicodeWrap(mBluetoothAdapter!!.address)
        )
    }

    override fun onDeviceDeleted(cachedDevice: CachedBluetoothDevice) {
        devicePreferenceMap.remove(cachedDevice)?.let {
            mDeviceListGroup!!.removePreference(it)
        }
    }

    @VisibleForTesting
    open fun enableScanning() {
        // BluetoothAdapter already handles repeated scan requests
        if (!mScanEnabled) {
            startScanning()
            mScanEnabled = true
        }
    }

    @VisibleForTesting
    fun disableScanning() {
        if (mScanEnabled) {
            stopScanning()
            mScanEnabled = false
        }
    }

    override fun onScanningStateChanged(started: Boolean) {
        if (!started && mScanEnabled) {
            startScanning()
        }
    }

    /**
     * Return the key of the [PreferenceGroup] that contains the bluetooth devices
     */
    abstract val deviceListKey: String

    @VisibleForTesting
    open fun startScanning() {
        if (scanType == ScanType.LE) {
            startLeScanning()
        } else {
            startClassicScanning()
        }
    }

    @VisibleForTesting
    open fun stopScanning() {
        if (scanType == ScanType.LE) {
            stopLeScanning()
        } else {
            stopClassicScanning()
        }
    }

    private fun startClassicScanning() {
        if (!mBluetoothAdapter!!.isDiscovering) {
            mBluetoothAdapter!!.startDiscovery()
        }
    }

    private fun stopClassicScanning() {
        if (mBluetoothAdapter!!.isDiscovering) {
            mBluetoothAdapter!!.cancelDiscovery()
        }
    }

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleLeScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            for (result in results.orEmpty()) {
                handleLeScanResult(result)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "BLE Scan failed with error code $errorCode")
        }
    }

    private fun startLeScanning() {
        val scanner = mBluetoothAdapter!!.bluetoothLeScanner
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner.startScan(leScanFilters, settings, leScanCallback)
    }

    private fun stopLeScanning() {
        val scanner = mBluetoothAdapter!!.bluetoothLeScanner
        scanner?.stopScan(leScanCallback)
    }

    private fun handleLeScanResult(result: ScanResult) {
        lifecycleScope?.launch {
            withContext(Dispatchers.Default) {
                val device = result.device
                val cachedDevice = mCachedDeviceManager!!.findDevice(device)
                    ?: mCachedDeviceManager!!.addDevice(device, leScanFilters)
                addDevice(cachedDevice)
            }
        }
    }

    companion object {
        private const val TAG = "DeviceListPreferenceFragment"
        private const val KEY_BT_SCAN = "bt_scan"

        // Copied from BluetoothDeviceNoNamePreferenceController.java
        private const val BLUETOOTH_SHOW_DEVICES_WITHOUT_NAMES_PROPERTY =
            "persist.bluetooth.showdeviceswithoutnames"
    }
}
