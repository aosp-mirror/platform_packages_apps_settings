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
package com.android.settings.location

import android.content.Context
import android.content.ContextWrapper
import android.location.LocationManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.preference.CatalystScreenTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class LocationScreenTest : CatalystScreenTestCase() {
    override val preferenceScreenCreator = LocationScreen()

    override val flagName: String
        get() = Flags.FLAG_CATALYST_LOCATION_SETTINGS

    private val mockLocationManager = mock<LocationManager>()

    private val context =
            object : ContextWrapper(appContext) {
                override fun getSystemService(name: String): Any =
                    when (name) {
                        Context.LOCATION_SERVICE -> mockLocationManager
                        else -> super.getSystemService(name)
                    }
            }

    @Test
    fun key() {
        assertThat(preferenceScreenCreator.key).isEqualTo(LocationScreen.KEY)
    }

    @Test
    fun getSummary_enableLocation_shouldReturnLoading() {
        mockLocationManager.stub { on { isLocationEnabled } doReturn true }

        assertThat(preferenceScreenCreator.getSummary(context)).isEqualTo(
                context.getString(R.string.location_settings_loading_app_permission_stats))
    }

    @Test
    fun getSummary_disableLocation_shouldReturnLocationOff() {
        mockLocationManager.stub { on { isLocationEnabled } doReturn false }

        assertThat(preferenceScreenCreator.getSummary(context)).isEqualTo(
                context.getString(R.string.location_settings_summary_location_off))
    }

    override fun migration() {
    }
}
