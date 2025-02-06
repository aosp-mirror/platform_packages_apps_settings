/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.network.ethernet

import android.content.Context
import android.net.EthernetManager
import android.net.IpConfiguration
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

class EthernetInterfaceTracker(private val context: Context) :
    EthernetManager.InterfaceStateListener {
    interface EthernetInterfaceListListener {
        fun onInterfaceListChanged()
    }

    private val ethernetManager =
        context.getSystemService(Context.ETHERNET_SERVICE) as EthernetManager
    private val TAG = "EthernetInterfaceTracker"

    // Maps ethernet interface identifier to EthernetInterface object
    private val ethernetInterfaces = mutableMapOf<String, EthernetInterface>()
    private val interfaceListeners = mutableListOf<EthernetInterfaceListListener>()
    private val mExecutor = ContextCompat.getMainExecutor(context)

    init {
        ethernetManager.addInterfaceStateListener(mExecutor, this)
    }

    fun getInterface(id: String): EthernetInterface? {
        return ethernetInterfaces.get(id)
    }

    fun getAvailableInterfaces(): Collection<EthernetInterface> {
        return ethernetInterfaces.values
    }

    fun registerInterfaceListener(listener: EthernetInterfaceListListener) {
        interfaceListeners.add(listener)
    }

    fun unregisterInterfaceListener(listener: EthernetInterfaceListListener) {
        interfaceListeners.remove(listener)
    }

    override fun onInterfaceStateChanged(id: String, state: Int, role: Int, cfg: IpConfiguration?) {
        var interfacesChanged = false
        if (!ethernetInterfaces.contains(id) && state != EthernetManager.STATE_ABSENT) {
            ethernetInterfaces.put(id, EthernetInterface(context, id))
            interfacesChanged = true
        } else if (ethernetInterfaces.contains(id) && state == EthernetManager.STATE_ABSENT) {
            ethernetInterfaces.remove(id)
            interfacesChanged = true
        }
        if (interfacesChanged) {
            for (listener in interfaceListeners) {
                listener.onInterfaceListChanged()
            }
        }
    }
}
