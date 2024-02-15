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
import android.bluetooth.BluetoothUuid
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.content.Context
import android.content.res.Resources
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter
import com.android.settingslib.bluetooth.BluetoothDeviceFilter
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.mockito.Mockito.`when` as whenever

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [
    ShadowBluetoothAdapter::class,
    com.android.settings.testutils.shadow.ShadowFragment::class,
])
class DeviceListPreferenceFragmentTest {
    @get:Rule
    val mockito: MockitoRule = MockitoJUnit.rule()

    @Mock
    private lateinit var resource: Resources

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var bluetoothLeScanner: BluetoothLeScanner

    @Mock
    private lateinit var cachedDeviceManager: CachedBluetoothDeviceManager

    @Mock
    private lateinit var cachedDevice: CachedBluetoothDevice

    @Spy
    private var fragment = TestFragment()

    private lateinit var myDevicePreference: Preference
    private lateinit var bluetoothAdapter: BluetoothAdapter

    @Before
    fun setUp() {
        doReturn(context).`when`(fragment).context
        doReturn(resource).`when`(fragment).resources
        doNothing().`when`(fragment).onDeviceAdded(cachedDevice)
        bluetoothAdapter = spy(BluetoothAdapter.getDefaultAdapter())
        fragment.mBluetoothAdapter = bluetoothAdapter
        fragment.mCachedDeviceManager = cachedDeviceManager

        myDevicePreference = Preference(RuntimeEnvironment.application)
    }

    @Test
    fun setUpdateMyDevicePreference_setTitleCorrectly() {
        doReturn(FOOTAGE_MAC_STRING).`when`(fragment)
            .getString(eq(R.string.bluetooth_footer_mac_message), any())

        fragment.updateFooterPreference(myDevicePreference)

        assertThat(myDevicePreference.title).isEqualTo(FOOTAGE_MAC_STRING)
    }

    @Test
    fun testEnableDisableScanning_testStateAfterEnableDisable() {
        fragment.enableScanning()
        verify(fragment).startScanning()
        assertThat(fragment.mScanEnabled).isTrue()

        fragment.disableScanning()
        verify(fragment).stopScanning()
        assertThat(fragment.mScanEnabled).isFalse()
    }

    @Test
    fun testScanningStateChanged_testScanStarted() {
        fragment.enableScanning()
        assertThat(fragment.mScanEnabled).isTrue()
        verify(fragment).startScanning()

        fragment.onScanningStateChanged(true)
        verify(fragment, times(1)).startScanning()
    }

    @Test
    fun testScanningStateChanged_testScanFinished() {
        // Could happen when last scanning not done while current scan gets enabled
        fragment.enableScanning()
        verify(fragment).startScanning()
        assertThat(fragment.mScanEnabled).isTrue()

        fragment.onScanningStateChanged(false)
        verify(fragment, times(2)).startScanning()
    }

    @Test
    fun testScanningStateChanged_testScanStateMultiple() {
        // Could happen when last scanning not done while current scan gets enabled
        fragment.enableScanning()
        assertThat(fragment.mScanEnabled).isTrue()
        verify(fragment).startScanning()

        fragment.onScanningStateChanged(true)
        verify(fragment, times(1)).startScanning()

        fragment.onScanningStateChanged(false)
        verify(fragment, times(2)).startScanning()

        fragment.onScanningStateChanged(true)
        verify(fragment, times(2)).startScanning()

        fragment.disableScanning()
        verify(fragment).stopScanning()

        fragment.onScanningStateChanged(false)
        verify(fragment, times(2)).startScanning()

        fragment.onScanningStateChanged(true)
        verify(fragment, times(2)).startScanning()
    }

    @Test
    fun testScanningStateChanged_testScanFinishedAfterDisable() {
        fragment.enableScanning()
        verify(fragment).startScanning()
        assertThat(fragment.mScanEnabled).isTrue()

        fragment.disableScanning()
        verify(fragment).stopScanning()
        assertThat(fragment.mScanEnabled).isFalse()

        fragment.onScanningStateChanged(false)
        verify(fragment, times(1)).startScanning()
    }

    @Test
    fun testScanningStateChanged_testScanStartedAfterDisable() {
        fragment.enableScanning()
        verify(fragment).startScanning()
        assertThat(fragment.mScanEnabled).isTrue()

        fragment.disableScanning()
        verify(fragment).stopScanning()
        assertThat(fragment.mScanEnabled).isFalse()

        fragment.onScanningStateChanged(true)
        verify(fragment, times(1)).startScanning()
    }

    @Test
    fun startScanning_setLeScanFilter_shouldStartLeScan() {
        val leScanFilter = ScanFilter.Builder()
            .setServiceData(BluetoothUuid.HEARING_AID, byteArrayOf(0), byteArrayOf(0))
            .build()
        doReturn(bluetoothLeScanner).`when`(bluetoothAdapter).bluetoothLeScanner

        fragment.setFilter(listOf(leScanFilter))
        fragment.startScanning()

        verify(bluetoothLeScanner).startScan(eq(listOf(leScanFilter)), any(), any<ScanCallback>())
    }

    @Test
    fun addCachedDevices_whenFilterIsNull_onDeviceAddedIsCalled() = runBlocking {
        val mockCachedDevice = mock(CachedBluetoothDevice::class.java)
        whenever(cachedDeviceManager.cachedDevicesCopy).thenReturn(listOf(mockCachedDevice))
        fragment.lifecycleScope = this

        fragment.addCachedDevices(filterForCachedDevices = null)
        delay(100)

        verify(fragment).onDeviceAdded(mockCachedDevice)
    }

    @Test
    fun addCachedDevices_whenFilterMatched_onDeviceAddedIsCalled() = runBlocking {
        val mockBluetoothDevice = mock(BluetoothDevice::class.java)
        whenever(mockBluetoothDevice.bondState).thenReturn(BluetoothDevice.BOND_NONE)
        whenever(cachedDevice.device).thenReturn(mockBluetoothDevice)
        whenever(cachedDeviceManager.cachedDevicesCopy).thenReturn(listOf(cachedDevice))
        fragment.lifecycleScope = this

        fragment.addCachedDevices(BluetoothDeviceFilter.UNBONDED_DEVICE_FILTER)
        delay(100)

        verify(fragment).onDeviceAdded(cachedDevice)
    }

    @Test
    fun addCachedDevices_whenFilterNoMatch_onDeviceAddedNotCalled() = runBlocking {
        val mockBluetoothDevice = mock(BluetoothDevice::class.java)
        whenever(mockBluetoothDevice.bondState).thenReturn(BluetoothDevice.BOND_BONDED)
        whenever(cachedDevice.device).thenReturn(mockBluetoothDevice)
        whenever(cachedDeviceManager.cachedDevicesCopy).thenReturn(listOf(cachedDevice))
        fragment.lifecycleScope = this

        fragment.addCachedDevices(BluetoothDeviceFilter.UNBONDED_DEVICE_FILTER)
        delay(100)

        verify(fragment, never()).onDeviceAdded(cachedDevice)
    }

    /**
     * Fragment to test since `DeviceListPreferenceFragment` is abstract
     */
    open class TestFragment : DeviceListPreferenceFragment(null) {
        override fun getMetricsCategory() = 0
        override fun initPreferencesFromPreferenceScreen() {}
        override val deviceListKey = "device_list"
        override fun getLogTag() = null
        override fun getPreferenceScreenResId() = 0
    }

    private companion object {
        const val FOOTAGE_MAC_STRING = "Bluetooth mac: xxxx"
    }
}
