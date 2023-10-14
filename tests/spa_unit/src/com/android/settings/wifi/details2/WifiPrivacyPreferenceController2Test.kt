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

package com.android.settings.wifi.details2

import android.content.Context
import androidx.preference.ListPreference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.wifitrackerlib.WifiEntry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class WifiPrivacyPreferenceController2Test {
    private var mockWifiEntry = mock<WifiEntry>()

    private var context: Context = ApplicationProvider.getApplicationContext()

    private var controller = spy(WifiPrivacyPreferenceController2(context).apply {
        setWifiEntry(mockWifiEntry)
    })

    private var preference = ListPreference(context).apply {
        setEntries(R.array.wifi_privacy_entries)
        setEntryValues(R.array.wifi_privacy_values)
    }

    private var preferenceStrings = context.resources.getStringArray(R.array.wifi_privacy_entries)

    @Test
    fun updateState_wifiPrivacy_setCorrectValue() {
        controller.stub {
            doReturn(WifiEntry.PRIVACY_DEVICE_MAC).whenever(mock).randomizationValue
        }

        controller.updateState(preference)

        val prefValue = WifiPrivacyPreferenceController2.translateMacRandomizedValueToPrefValue(
            WifiEntry.PRIVACY_DEVICE_MAC
        )
        assertThat(preference.entry).isEqualTo(preferenceStrings[prefValue])
    }

    @Test
    fun updateState_wifiNotMetered_setCorrectValue() {
        controller.stub {
            doReturn(WifiEntry.PRIVACY_RANDOMIZED_MAC).whenever(mock).randomizationValue
        }

        controller.updateState(preference)

        val prefValue = WifiPrivacyPreferenceController2.translateMacRandomizedValueToPrefValue(
            WifiEntry.PRIVACY_RANDOMIZED_MAC
        )
        assertThat(preference.entry).isEqualTo(preferenceStrings[prefValue])
    }

    @Test
    fun updateState_canSetPrivacyInNextUpdate_shouldBeSelectable() {
        mockWifiEntry.stub {
            // Return false in WifiEntry#canSetPrivacy to make preference un-selectable first.
            on { canSetPrivacy() } doReturn false
        }
        controller.updateState(preference)
        assertThat(preference.isSelectable).isFalse()

        mockWifiEntry.stub {
            // Return true in WifiEntry#canSetPrivacy to verify preference back to selectable.
            on { canSetPrivacy() } doReturn true
        }
        controller.updateState(preference)
        assertThat(preference.isSelectable).isTrue()
    }

    @Test
    fun updateState_canNotSetPrivacyInNextUpdate_shouldNotBeSelectable() {
        mockWifiEntry.stub {
            // Return true in WifiEntry#canSetPrivacy to make preference selectable first.
            on { canSetPrivacy() } doReturn true
        }
        controller.updateState(preference)
        assertThat(preference.isSelectable).isTrue()

        mockWifiEntry.stub {
            // Return false in WifiEntry#canSetPrivacy to verify preference back to un-selectable.
            on { canSetPrivacy() } doReturn false
        }
        controller.updateState(preference)
        assertThat(preference.isSelectable).isFalse()
    }

    @Test
    fun onPreferenceChange_sameNewValue_doNoting() {
        mockWifiEntry.stub {
            on { privacy } doReturn 0
            on { connectedState } doReturn WifiEntry.CONNECTED_STATE_CONNECTED
        }

        controller.onPreferenceChange(preference, "0")

        verify(mockWifiEntry, never()).privacy = any()
        verify(mockWifiEntry, never()).disconnect(null)
        verify(mockWifiEntry, never()).connect(null)
    }

    @Test
    fun onPreferenceChange_differentNewValue_setAndReconnect() {
        mockWifiEntry.stub {
            on { privacy } doReturn 0
            on { connectedState } doReturn WifiEntry.CONNECTED_STATE_CONNECTED
        }

        controller.onPreferenceChange(preference, "1")

        verify(mockWifiEntry).privacy = 1
        verify(mockWifiEntry).disconnect(null)
        verify(mockWifiEntry).connect(null)
    }
}
