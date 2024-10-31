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

import android.content.Intent
import android.provider.Settings.Global
import androidx.preference.PreferenceFragmentCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.Settings.BluetoothDashboardActivity
import com.android.settings.flags.Flags
import com.android.settingslib.preference.CatalystScreenTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BluetoothDashboardScreenTest : CatalystScreenTestCase() {
    override val preferenceScreenCreator = BluetoothDashboardScreen()

    override val flagName: String
        get() = Flags.FLAG_CATALYST_BLUETOOTH_SWITCHBAR_SCREEN

    @Test
    fun key() {
        assertThat(preferenceScreenCreator.key).isEqualTo(BluetoothDashboardScreen.KEY)
    }

    override fun launchFragment(
        fragmentClass: Class<PreferenceFragmentCompat>,
        action: (PreferenceFragmentCompat) -> Unit,
    ) {
        Global.putInt(appContext.contentResolver, Global.DEVICE_PROVISIONED, 1)
        val intent = Intent(appContext, BluetoothDashboardActivity::class.java)
        ActivityScenario.launch<BluetoothDashboardActivity>(intent).use {
            it.onActivity { activity ->
                val fragment = activity.supportFragmentManager.fragments[0]
                assertThat(fragment.javaClass).isEqualTo(fragmentClass)
                action(fragment as PreferenceFragmentCompat)
            }
        }
    }
}
