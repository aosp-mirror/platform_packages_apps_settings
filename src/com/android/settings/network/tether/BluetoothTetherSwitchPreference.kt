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

package com.android.settings.network.tether

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothPan
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.TetheringManager
import android.os.Handler
import android.os.Looper
import com.android.settings.R
import com.android.settings.datausage.DataSaverBackend
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedDataObservable
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference
import java.util.concurrent.atomic.AtomicReference

// LINT.IfChange
@Suppress("DEPRECATION")
class BluetoothTetherSwitchPreference :
    SwitchPreference(KEY, R.string.bluetooth_tether_checkbox_text),
    PreferenceAvailabilityProvider,
    PreferenceLifecycleProvider {

    private var tetherChangeReceiver: BroadcastReceiver? = null

    override val summary: Int
        get() = R.string.bluetooth_tethering_subtext

    override val keywords: Int
        get() = R.string.keywords_hotspot_tethering

    override fun storage(context: Context): KeyValueStore = BluetoothTetherStore(context)

    override fun isAvailable(context: Context): Boolean {
        BluetoothAdapter.getDefaultAdapter() ?: return false
        val tetheringManager = context.getSystemService(TetheringManager::class.java)
        val bluetoothRegexs = tetheringManager?.tetherableBluetoothRegexs
        return bluetoothRegexs?.isNotEmpty() == true
    }

    override fun isEnabled(context: Context): Boolean {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false
        val btState = adapter.state
        /* TODO: when bluetooth is off, btstate will be `state_turning_on` -> `state_off` ->
        `state_turning_on` -> `state_on`, causing preference enable status incorrect. */
        when (btState) {
            BluetoothAdapter.STATE_TURNING_OFF,
            BluetoothAdapter.STATE_TURNING_ON -> return false
            else -> {}
        }
        val dataSaverBackend = DataSaverBackend(context)
        return !dataSaverBackend.isDataSaverEnabled
    }

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int,
    ) = ReadWritePermit.ALLOW

    override val sensitivityLevel: Int
        get() = SensitivityLevel.LOW_SENSITIVITY

    override fun onCreate(context: PreferenceLifecycleContext) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(content: Context, intent: Intent) {
                    when (intent.action) {
                        TetheringManager.ACTION_TETHER_STATE_CHANGED,
                        Intent.ACTION_MEDIA_SHARED,
                        Intent.ACTION_MEDIA_UNSHARED,
                        BluetoothAdapter.ACTION_STATE_CHANGED,
                        BluetoothPan.ACTION_TETHERING_STATE_CHANGED ->
                            context.notifyPreferenceChange(KEY)
                    }
                }
            }
        tetherChangeReceiver = receiver
        var filter = IntentFilter(TetheringManager.ACTION_TETHER_STATE_CHANGED)
        val intent = context.registerReceiver(receiver, filter)

        filter = IntentFilter()
        filter.addAction(Intent.ACTION_MEDIA_SHARED)
        filter.addAction(Intent.ACTION_MEDIA_UNSHARED)
        filter.addDataScheme("file")
        context.registerReceiver(receiver, filter)

        filter = IntentFilter()
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.addAction(BluetoothPan.ACTION_TETHERING_STATE_CHANGED)
        context.registerReceiver(receiver, filter)
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        tetherChangeReceiver?.let {
            context.unregisterReceiver(it)
            tetherChangeReceiver = null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private class BluetoothTetherStore(private val context: Context) :
        KeyedDataObservable<String>(), KeyValueStore {

        val bluetoothPan = AtomicReference<BluetoothPan>()

        override fun contains(key: String) = key == KEY

        override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
            // TODO: support async operation in background thread
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false as T
            if (bluetoothPan.get() == null) {
                val profileServiceListener: BluetoothProfile.ServiceListener =
                    object : BluetoothProfile.ServiceListener {
                        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                            if (bluetoothPan.get() == null) {
                                bluetoothPan.set(proxy as BluetoothPan)
                                notifyChange(KEY, 0)
                            }
                        }

                        override fun onServiceDisconnected(profile: Int) {
                            /* Do nothing */
                        }
                    }
                // TODO: adapter.closeProfileProxy(bluetoothPan.get())
                adapter.getProfileProxy(
                    context.applicationContext,
                    profileServiceListener,
                    BluetoothProfile.PAN,
                )
            }

            val btState = adapter.state
            val pan = bluetoothPan.get()
            return ((btState == BluetoothAdapter.STATE_ON ||
                btState == BluetoothAdapter.STATE_TURNING_OFF) && pan != null && pan.isTetheringOn)
                as T?
        }

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
            if (value == null) return
            val connectivityManager =
                context.getSystemService(ConnectivityManager::class.java) ?: return
            if (value as Boolean) {
                val handler by lazy { Handler(Looper.getMainLooper()) }
                val startTetheringCallback = OnStartTetheringCallback()
                fun startTethering() {
                    connectivityManager.startTethering(
                        ConnectivityManager.TETHERING_BLUETOOTH,
                        true,
                        startTetheringCallback,
                        handler,
                    )
                }

                val adapter = BluetoothAdapter.getDefaultAdapter()
                if (adapter.state == BluetoothAdapter.STATE_OFF) {
                    adapter.enable()
                    val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                    val tetherChangeReceiver =
                        object : BroadcastReceiver() {
                            override fun onReceive(context: Context, intent: Intent) {
                                if (
                                    intent.getIntExtra(
                                        BluetoothAdapter.EXTRA_STATE,
                                        BluetoothAdapter.ERROR,
                                    ) == BluetoothAdapter.STATE_ON
                                ) {
                                    startTethering()
                                    context.unregisterReceiver(this)
                                }
                            }
                        }
                    val intent = context.registerReceiver(tetherChangeReceiver, filter)
                    if (intent != null) tetherChangeReceiver.onReceive(context, intent)
                } else {
                    startTethering()
                }
            } else {
                connectivityManager.stopTethering(ConnectivityManager.TETHERING_BLUETOOTH)
            }
        }

        private inner class OnStartTetheringCallback :
            ConnectivityManager.OnStartTetheringCallback() {
            override fun onTetheringStarted() {
                notifyChange(KEY, 0)
            }

            override fun onTetheringFailed() {
                notifyChange(KEY, 0)
            }
        }
    }

    companion object {
        const val KEY = "enable_bluetooth_tethering"
    }
}
// LINT.ThenChange(TetherSettings.java)
