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

package com.android.settings.network

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothPan
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.IntentFilter
import android.net.TetheringInterface
import android.net.TetheringManager
import com.android.settingslib.spaprivileged.framework.common.broadcastReceiverFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

class TetheredRepository(private val context: Context) {
    private val tetheringManager = context.getSystemService(TetheringManager::class.java)!!

    private val adapter = context.getSystemService(BluetoothManager::class.java)!!.adapter

    fun tetheredTypesFlow(): Flow<Set<Int>> =
        combine(
            tetheredInterfacesFlow(),
            isBluetoothTetheringOnFlow(),
        ) { tetheringInterfaces, isBluetoothTetheringOn ->
            val mutableSet = tetheringInterfaces.map { it.type }.toMutableSet()
            if (isBluetoothTetheringOn) mutableSet += TetheringManager.TETHERING_BLUETOOTH
            mutableSet
        }.conflate().flowOn(Dispatchers.Default)

    private fun tetheredInterfacesFlow(): Flow<Set<TetheringInterface>> = callbackFlow {
        val callback = object : TetheringManager.TetheringEventCallback {
            override fun onTetheredInterfacesChanged(interfaces: Set<TetheringInterface>) {
                trySend(interfaces)
            }
        }

        tetheringManager.registerTetheringEventCallback(Dispatchers.Default.asExecutor(), callback)

        awaitClose { tetheringManager.unregisterTetheringEventCallback(callback) }
    }.conflate().flowOn(Dispatchers.Default)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun isBluetoothTetheringOnFlow(): Flow<Boolean> =
        merge(
            flowOf(null), // kick an initial value
            context.broadcastReceiverFlow(IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)),
        ).flatMapLatest {
            if (adapter.getState() == BluetoothAdapter.STATE_ON) {
                isBluetoothPanTetheringOnFlow()
            } else {
                flowOf(false)
            }
        }.conflate().flowOn(Dispatchers.Default)

    private fun isBluetoothPanTetheringOnFlow() = callbackFlow {
        var connectedProxy: BluetoothProfile? = null

        val listener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                connectedProxy = proxy
                launch(Dispatchers.Default) {
                    trySend((proxy as BluetoothPan).isTetheringOn)
                }
            }

            override fun onServiceDisconnected(profile: Int) {}
        }

        adapter.getProfileProxy(context, listener, BluetoothProfile.PAN)

        awaitClose {
            connectedProxy?.let { adapter.closeProfileProxy(BluetoothProfile.PAN, it) }
        }
    }.conflate().flowOn(Dispatchers.Default)
}
