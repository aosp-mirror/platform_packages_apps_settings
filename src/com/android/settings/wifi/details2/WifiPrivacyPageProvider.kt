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

package com.android.settings.wifi.details2

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import android.os.SimpleClock
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.widget.preference.ListPreferenceModel
import com.android.settingslib.spa.widget.preference.ListPreferenceOption
import com.android.settingslib.spa.widget.preference.RadioPreferences
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import com.android.settingslib.spa.widget.ui.CategoryTitle
import com.android.wifitrackerlib.WifiEntry
import java.time.Clock
import java.time.ZoneOffset

const val WIFI_ENTRY_KEY = "wifiEntryKey"

object WifiPrivacyPageProvider : SettingsPageProvider {
    override val name = "WifiPrivacy"
    const val TAG = "WifiPrivacyPageProvider"

    override val parameter = listOf(
        navArgument(WIFI_ENTRY_KEY) { type = NavType.StringType },
    )

    @Composable
    override fun Page(arguments: Bundle?) {
        val wifiEntryKey = arguments!!.getString(WIFI_ENTRY_KEY)
        if (wifiEntryKey != null) {
            val context = LocalContext.current
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            val wifiEntry = remember {
                getWifiEntry(context, wifiEntryKey, lifecycle)
            }
            WifiPrivacyPage(wifiEntry)
        }
    }

    fun getRoute(
        wifiEntryKey: String,
    ): String = "${name}/$wifiEntryKey"
}

@Composable
fun WifiPrivacyPage(wifiEntry: WifiEntry) {
    val isSelectable: Boolean = wifiEntry.canSetPrivacy()
    RegularScaffold(
        title = stringResource(id = R.string.wifi_privacy_settings)
    ) {
        Column {
            val title = stringResource(id = R.string.wifi_privacy_mac_settings)
            val wifiPrivacyEntries = stringArrayResource(R.array.wifi_privacy_entries)
            val wifiPrivacyValues = stringArrayResource(R.array.wifi_privacy_values)
            val textsSelectedId = rememberSaveable { mutableIntStateOf(wifiEntry.privacy) }
            val dataList = remember {
                wifiPrivacyEntries.mapIndexed { index, text ->
                    ListPreferenceOption(id = wifiPrivacyValues[index].toInt(), text = text)
                }
            }
            RadioPreferences(remember {
                object : ListPreferenceModel {
                    override val title = title
                    override val options = dataList
                    override val selectedId = textsSelectedId
                    override val onIdSelected: (Int) -> Unit = {
                        textsSelectedId.intValue = it
                        onSelectedChange(wifiEntry, it)
                    }
                    override val enabled = { isSelectable }
                }
            })
            wifiEntry.wifiConfiguration?.let {
                DeviceNameSwitchPreference(it)
            }
        }
    }
}

@Composable
fun DeviceNameSwitchPreference(wifiConfiguration: WifiConfiguration){
    Spacer(modifier = Modifier.width(SettingsDimension.itemDividerHeight))
    CategoryTitle(title = stringResource(R.string.wifi_privacy_device_name_settings))
    Spacer(modifier = Modifier.width(SettingsDimension.itemDividerHeight))
    var checked by remember {
        mutableStateOf(wifiConfiguration.isSendDhcpHostnameEnabled)
    }
    val context = LocalContext.current
    val wifiManager = context.getSystemService(WifiManager::class.java)!!
    SwitchPreference(object : SwitchPreferenceModel {
        override val title =
            context.resources.getString(
                R.string.wifi_privacy_send_device_name_toggle_title
            )
        override val summary =
            {
                context.resources.getString(
                    R.string.wifi_privacy_send_device_name_toggle_summary
                )
            }
        override val checked = { checked }
        override val onCheckedChange: (Boolean) -> Unit = { newChecked ->
            wifiConfiguration.isSendDhcpHostnameEnabled = newChecked
            wifiManager.save(wifiConfiguration, null /* listener */)
            checked = newChecked
        }
    })
}

fun onSelectedChange(wifiEntry: WifiEntry, privacy: Int) {
    if (wifiEntry.privacy == privacy) {
        // Prevent disconnection + reconnection if settings not changed.
        return
    }
    wifiEntry.setPrivacy(privacy)

    // To activate changing, we need to reconnect network. WiFi will auto connect to
    // current network after disconnect(). Only needed when this is connected network.

    // To activate changing, we need to reconnect network. WiFi will auto connect to
    // current network after disconnect(). Only needed when this is connected network.
    if (wifiEntry.getConnectedState() == WifiEntry.CONNECTED_STATE_CONNECTED) {
        wifiEntry.disconnect(null /* callback */)
        wifiEntry.connect(null /* callback */)
    }
}

fun getWifiEntry(
    context: Context,
    wifiEntryKey: String,
    liftCycle: androidx.lifecycle.Lifecycle
): WifiEntry {
    // Max age of tracked WifiEntries
    val MAX_SCAN_AGE_MILLIS: Long = 15000
    // Interval between initiating SavedNetworkTracker scans
    val SCAN_INTERVAL_MILLIS: Long = 10000
    val mWorkerThread = HandlerThread(
        WifiPrivacyPageProvider.TAG,
        Process.THREAD_PRIORITY_BACKGROUND
    )
    mWorkerThread.start()
    val elapsedRealtimeClock: Clock = object : SimpleClock(ZoneOffset.UTC) {
        override fun millis(): Long {
            return android.os.SystemClock.elapsedRealtime()
        }
    }
    val mNetworkDetailsTracker = featureFactory
        .wifiTrackerLibProvider
        .createNetworkDetailsTracker(
            liftCycle,
            context,
            Handler(Looper.getMainLooper()),
            mWorkerThread.getThreadHandler(),
            elapsedRealtimeClock,
            MAX_SCAN_AGE_MILLIS,
            SCAN_INTERVAL_MILLIS,
            wifiEntryKey
        )
    return mNetworkDetailsTracker.wifiEntry
}
