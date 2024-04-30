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

package com.android.settings.spa.network

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.network.SatelliteWarningDialogActivity
import com.android.settings.network.SatelliteWarningDialogActivity.Companion.EXTRA_TYPE_OF_SATELLITE_WARNING_DIALOG
import com.android.settings.network.SatelliteWarningDialogActivity.Companion.TYPE_IS_AIRPLANE_MODE
import com.android.settings.network.SatelliteWarningDialogActivity.Companion.TYPE_IS_BLUETOOTH
import com.android.settings.network.SatelliteWarningDialogActivity.Companion.TYPE_IS_UNKNOWN
import com.android.settings.network.SatelliteWarningDialogActivity.Companion.TYPE_IS_WIFI
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SatelliteWarningDialogActivityTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SatelliteWarningDialogActivity>()
    val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun launchActivity_checkExtraValue_typeIsWifi() {
        val scenario = launchDialogActivity(TYPE_IS_WIFI)

        scenario.onActivity { activity ->
            assert(
                activity.intent.getIntExtra(
                    EXTRA_TYPE_OF_SATELLITE_WARNING_DIALOG,
                    TYPE_IS_UNKNOWN
                ) == TYPE_IS_WIFI
            )
        }
        scenario.close()
    }

    @Test
    fun launchActivity_checkExtraValue_typeIsBluetooth() {
        val scenario = launchDialogActivity(TYPE_IS_BLUETOOTH)

        scenario.onActivity { activity ->
            assert(
                activity.intent.getIntExtra(
                    EXTRA_TYPE_OF_SATELLITE_WARNING_DIALOG,
                    TYPE_IS_UNKNOWN
                ) == TYPE_IS_BLUETOOTH
            )
        }
        scenario.close()
    }

    @Test
    fun launchActivity_unknownType_destroyActivity() {
        val scenario = launchDialogActivity(TYPE_IS_UNKNOWN)

        assertTrue(scenario.state.isAtLeast(Lifecycle.State.DESTROYED))
        scenario.close()
    }

    @Test
    fun testDialogIsExisted() {
        val scenario = launchDialogActivity(TYPE_IS_WIFI)

        composeTestRule.onNodeWithText(context.getString(com.android.settingslib.R.string.okay))
            .assertIsDisplayed()
        scenario.close()
    }

    @Test
    fun testDialogTitle_titleIsIncludeWifi() {
        val scenario = launchDialogActivity(TYPE_IS_WIFI)

        composeTestRule.onNodeWithText(
            String.format(
                context.getString(R.string.satellite_warning_dialog_title),
                context.getString(R.string.wifi),
            )
        ).assertIsDisplayed()
        scenario.close()
    }

    @Test
    fun testDialogTitle_titleIsIncludeAirplaneMode() {
        val scenario = launchDialogActivity(TYPE_IS_AIRPLANE_MODE)

        composeTestRule.onNodeWithText(
            String.format(
                context.getString(R.string.satellite_warning_dialog_title),
                context.getString(R.string.airplane_mode),
            )
        ).assertIsDisplayed()
        scenario.close()
    }

    private fun launchDialogActivity(type: Int): ActivityScenario<SatelliteWarningDialogActivity> = launch(
        Intent(
            context,
            SatelliteWarningDialogActivity::class.java
        ).putExtra(EXTRA_TYPE_OF_SATELLITE_WARNING_DIALOG, type)
    )
}
