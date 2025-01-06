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

@file:Suppress("DEPRECATION", "MissingPermission")

package com.android.settings.wifi.utils

import android.content.Context
import android.net.TetheringManager
import android.net.wifi.WifiManager

/**
 * Gets the {@link android.net.wifi.WifiManager} system service.
 *
 * Use application context to get system services to avoid memory leaks.
 */
val Context.wifiManager: WifiManager?
    get() = applicationContext.getSystemService(WifiManager::class.java)

/** Return the UTF-8 String set to be the SSID for the Soft AP. */
val Context.wifiSoftApSsid
    get() = wifiManager?.softApConfiguration?.ssid

/** Gets the tethered Wi-Fi hotspot enabled state. */
val Context.wifiApState
    get() = wifiManager?.wifiApState

/** Gets/Sets the Wi-Fi enabled state. */
var Context.isWifiEnabled: Boolean
    get() = wifiManager?.isWifiEnabled == true
    set(value) {
        wifiManager?.isWifiEnabled = value
    }

/**
 * Gets the {@link android.net.TetheringManager} system service.
 *
 * Use application context to get system services to avoid memory leaks.
 */
val Context.tetheringManager: TetheringManager?
    get() = applicationContext.getSystemService(TetheringManager::class.java)
