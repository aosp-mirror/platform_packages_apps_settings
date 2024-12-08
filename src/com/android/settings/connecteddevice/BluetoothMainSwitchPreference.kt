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

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.android.settings.R
import com.android.settings.widget.MainSwitchBarMetadata
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.NoOpKeyedObservable
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.ReadWritePermit

class BluetoothMainSwitchPreference(private val bluetoothAdapter: BluetoothAdapter?) :
    MainSwitchBarMetadata, PreferenceLifecycleProvider {

    private lateinit var broadcastReceiver: BroadcastReceiver

    override val key
        get() = "use_bluetooth"

    override val title
        get() = R.string.bluetooth_main_switch_title

    override fun getReadPermit(context: Context, myUid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, value: Boolean?, myUid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun storage(context: Context) = BluetoothStateStore(bluetoothAdapter)

    override fun onStart(context: PreferenceLifecycleContext) {
        broadcastReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(receiverContext: Context, intent: Intent) {
                    context.notifyPreferenceChange(key)
                }
            }
        context.registerReceiver(
            broadcastReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
            Context.RECEIVER_EXPORTED_UNAUDITED
        )
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        if (::broadcastReceiver.isInitialized) {
            context.unregisterReceiver(broadcastReceiver)
        }
    }

    override fun isEnabled(context: Context): Boolean {
        return bluetoothAdapter?.state.let {
            it == BluetoothAdapter.STATE_ON || it == BluetoothAdapter.STATE_OFF
        }
    }

    @Suppress("UNCHECKED_CAST")
    class BluetoothStateStore(private val bluetoothAdapter: BluetoothAdapter?) :
        NoOpKeyedObservable<String>(), KeyValueStore {

        override fun contains(key: String) = true

        override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
            return (bluetoothAdapter?.state.let {
                it == BluetoothAdapter.STATE_ON || it == BluetoothAdapter.STATE_TURNING_ON
            }) as T
        }

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
            if (value is Boolean) {
                if (value) {
                    bluetoothAdapter?.enable()
                } else {
                    bluetoothAdapter?.disable()
                }
            }
        }
    }
}
