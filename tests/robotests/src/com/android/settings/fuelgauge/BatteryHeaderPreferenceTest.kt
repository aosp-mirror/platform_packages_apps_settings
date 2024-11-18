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

package com.android.settings.fuelgauge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager.EXTRA_LEVEL
import android.os.BatteryManager.EXTRA_SCALE
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.UsageProgressBarPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
class BatteryHeaderPreferenceTest {
    private val mockLifecycleContext = mock<PreferenceLifecycleContext>()
    private val mockBatteryBroadcastReceiver = mock<BatteryBroadcastReceiver>()
    private val batteryHeaderPreference = BatteryHeaderPreference()

    private val context: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter?) =
                Intent(Intent.ACTION_BATTERY_CHANGED)
                    .putExtra(EXTRA_LEVEL, 60)
                    .putExtra(EXTRA_SCALE, 100)
        }

    @Test
    fun createAndBindWidget_selectableIsFalse() {
        val usageProgressBarPreference =
            batteryHeaderPreference.createAndBindWidget<UsageProgressBarPreference>(context)

        assertThat(usageProgressBarPreference.isSelectable).isFalse()
    }

    @Test
    fun onCreate_createBatteryBroadcastReceiver() {
        batteryHeaderPreference.onCreate(mockLifecycleContext)

        assertThat(batteryHeaderPreference.batteryBroadcastReceiver).isNotNull()
    }

    @Test
    fun onStart_invokeRegisterMethod() {
        batteryHeaderPreference.batteryBroadcastReceiver = mockBatteryBroadcastReceiver

        batteryHeaderPreference.onStart(mockLifecycleContext)

        verify(mockBatteryBroadcastReceiver).register()
    }

    @Test
    fun onStop_invokeUnRegisterMethod() {
        batteryHeaderPreference.batteryBroadcastReceiver = mockBatteryBroadcastReceiver

        batteryHeaderPreference.onStop(mockLifecycleContext)

        verify(mockBatteryBroadcastReceiver).unRegister()
    }
}
// LINT.ThenChange(BatteryHeaderPreferenceControllerTest.java)
