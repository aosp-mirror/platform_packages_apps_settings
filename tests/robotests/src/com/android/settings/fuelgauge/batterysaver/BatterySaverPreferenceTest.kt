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
package com.android.settings.fuelgauge.batterysaver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager.EXTRA_PLUGGED
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.MainSwitchPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
class BatterySaverPreferenceTest {
    private val powerManager = mock<PowerManager>()

    private val context: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                when {
                    name == getSystemServiceName(PowerManager::class.java) -> powerManager
                    else -> super.getSystemService(name)
                }

            override fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter?) =
                Intent().putExtra(EXTRA_PLUGGED, 0)
        }

    private val contextPlugIn: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter?) =
                Intent().putExtra(EXTRA_PLUGGED, 1)
        }

    private val batterySaverPreference = BatterySaverPreference()

    @Test
    fun lowPowerOn_preferenceIsChecked() {
        powerManager.stub { on { isPowerSaveMode } doReturn true }

        assertThat(getMainSwitchPreference().isChecked).isTrue()
    }

    @Test
    fun lowPowerOff_preferenceIsUnChecked() {
        powerManager.stub { on { isPowerSaveMode } doReturn false }

        assertThat(getMainSwitchPreference().isChecked).isFalse()
    }

    @Test
    fun storeSetOn_setPowerSaveMode() {
        batterySaverPreference
            .storage(context)
            .setValue(batterySaverPreference.key, Boolean::class.javaObjectType, true)

        verify(powerManager).setPowerSaveModeEnabled(true)
    }

    @Test
    fun storeSetOff_unsetPowerSaveMode() {
        batterySaverPreference
            .storage(context)
            .setValue(batterySaverPreference.key, Boolean::class.javaObjectType, false)

        verify(powerManager).setPowerSaveModeEnabled(false)
    }

    @Test
    fun isUnPlugIn_preferenceEnabled() {
        assertThat(getMainSwitchPreference().isEnabled).isTrue()
    }

    @Test
    fun isPlugIn_preferenceDisabled() {
        assertThat(getMainSwitchPreference(contextPlugIn).isEnabled).isFalse()
    }

    private fun getMainSwitchPreference(ctx: Context = context) =
        batterySaverPreference.createAndBindWidget<MainSwitchPreference>(ctx)
}
// LINT.ThenChange(BatterySaverButtonPreferenceControllerTest.java)
