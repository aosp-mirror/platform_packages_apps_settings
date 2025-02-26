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

import android.content.Intent
import android.os.BatteryManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settingslib.preference.CatalystScreenTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BatterySaverScreenTest : CatalystScreenTestCase() {
    private val intent =
        Intent(Intent.ACTION_BATTERY_CHANGED).putExtra(BatteryManager.EXTRA_PLUGGED, 0)

    override val preferenceScreenCreator = BatterySaverScreen()

    override val flagName: String
        get() = Flags.FLAG_CATALYST_BATTERY_SAVER_SCREEN

    @Before
    fun setUp() {
        appContext.sendStickyBroadcast(intent)
    }

    @After
    fun tearDown() {
        appContext.removeStickyBroadcast(intent)
    }

    @Test
    fun key() {
        assertThat(preferenceScreenCreator.key).isEqualTo(BatterySaverScreen.KEY)
    }
}
