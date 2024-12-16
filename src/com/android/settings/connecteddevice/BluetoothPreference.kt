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

package com.android.settings.connecteddevice

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.UserManager
import android.provider.Settings
import android.widget.Toast
import androidx.preference.Preference
import com.android.settings.PreferenceRestrictionMixin
import com.android.settings.R
import com.android.settings.network.SatelliteRepository.Companion.isSatelliteOn
import com.android.settings.network.SatelliteWarningDialogActivity
import com.android.settings.widget.MainSwitchBarMetadata
import com.android.settingslib.WirelessUtils
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.DataChangeReason
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel

@SuppressLint("MissingPermission")
class BluetoothPreference(private val bluetoothDataStore: BluetoothDataStore) :
    MainSwitchBarMetadata, PreferenceRestrictionMixin, Preference.OnPreferenceChangeListener {

    override val key
        get() = KEY

    override val title
        get() = R.string.bluetooth_main_switch_title

    override val restrictionKeys: Array<String>
        get() = arrayOf(UserManager.DISALLOW_BLUETOOTH, UserManager.DISALLOW_CONFIG_BLUETOOTH)

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int,
    ) =
        when {
            isSatelliteOn(context, 3000) ||
                (value == true &&
                    !WirelessUtils.isRadioAllowed(context, Settings.Global.RADIO_BLUETOOTH)) ->
                ReadWritePermit.DISALLOW
            else -> ReadWritePermit.ALLOW
        }

    override val sensitivityLevel
        get() = SensitivityLevel.LOW_SENSITIVITY

    override fun storage(context: Context) = bluetoothDataStore

    override fun isEnabled(context: Context): Boolean {
        return super<PreferenceRestrictionMixin>.isEnabled(context) &&
            bluetoothDataStore.bluetoothAdapter?.state.let {
                it == BluetoothAdapter.STATE_ON || it == BluetoothAdapter.STATE_OFF
            }
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val context = preference.context

        if (isSatelliteOn(context, 3000)) {
            context.startActivity(
                Intent(context, SatelliteWarningDialogActivity::class.java)
                    .putExtra(
                        SatelliteWarningDialogActivity.EXTRA_TYPE_OF_SATELLITE_WARNING_DIALOG,
                        SatelliteWarningDialogActivity.TYPE_IS_BLUETOOTH,
                    )
            )
            return false
        }

        // Show toast message if Bluetooth is not allowed in airplane mode
        if (
            newValue == true &&
                !WirelessUtils.isRadioAllowed(context, Settings.Global.RADIO_BLUETOOTH)
        ) {
            Toast.makeText(context, R.string.wifi_in_airplane_mode, Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    @Suppress("UNCHECKED_CAST")
    private class BluetoothStorage(
        private val context: Context,
        override val bluetoothAdapter: BluetoothAdapter?,
    ) : AbstractKeyedDataObservable<String>(), BluetoothDataStore {

        private var broadcastReceiver: BroadcastReceiver? = null

        override fun contains(key: String) = key == KEY && bluetoothAdapter != null

        override fun <T : Any> getValue(key: String, valueType: Class<T>): T {
            return (bluetoothAdapter?.state.let {
                it == BluetoothAdapter.STATE_ON || it == BluetoothAdapter.STATE_TURNING_ON
            })
                as T
        }

        @Suppress("DEPRECATION")
        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
            if (value is Boolean) {
                if (value) {
                    bluetoothAdapter?.enable()
                } else {
                    bluetoothAdapter?.disable()
                }
            }
        }

        @SuppressLint("WrongConstant")
        override fun onFirstObserverAdded() {
            broadcastReceiver =
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        notifyChange(KEY, DataChangeReason.UPDATE)
                    }
                }
            context.registerReceiver(
                broadcastReceiver,
                IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
                Context.RECEIVER_EXPORTED_UNAUDITED,
            )
        }

        override fun onLastObserverRemoved() {
            context.unregisterReceiver(broadcastReceiver)
        }
    }

    companion object {
        const val KEY = "use_bluetooth"

        @Suppress("DEPRECATION")
        fun createDataStore(context: Context) =
            createDataStore(context, BluetoothAdapter.getDefaultAdapter())

        fun createDataStore(
            context: Context,
            bluetoothAdapter: BluetoothAdapter?,
        ): BluetoothDataStore = BluetoothStorage(context, bluetoothAdapter)
    }
}

/** Datastore of the bluetooth preference. */
interface BluetoothDataStore : KeyValueStore {
    val bluetoothAdapter: BluetoothAdapter?
}
