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
package com.android.settings.spa.core.instrumentation

import android.app.settings.SettingsEnums
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spa.framework.common.LOG_DATA_SESSION_NAME
import com.android.settingslib.spa.framework.common.LogEvent
import com.android.settingslib.spa.framework.util.SESSION_BROWSE
import com.android.settingslib.spa.framework.util.SESSION_SEARCH
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for {@link SpaLogData}. */
@RunWith(AndroidJUnit4::class)
class SpaLogDataTest {
    private val TEST_PID = "pseudo_page_id"

    private lateinit var bundle: Bundle
    private lateinit var dataModel: MetricsDataModel

    @Before
    fun setUp() {
        bundle = Bundle()
        dataModel = MetricsDataModel()
    }

    @Test
    fun getSessionType_withoutSessionExtraData_returnSessionUnknow() {
        val spaLogData = SpaLogData(TEST_PID, LogEvent.PAGE_ENTER, bundle, dataModel)

        assertThat(spaLogData.getSessionType()).isEqualTo(SettingsEnums.SESSION_UNKNOWN)
    }

    @Test
    fun getSessionType_hasSessionBrowseExtraData_returnSessionBrowse() {
        bundle.putString(LOG_DATA_SESSION_NAME, SESSION_BROWSE)
        val spaLogData = SpaLogData(TEST_PID, LogEvent.PAGE_ENTER, bundle, dataModel)

        assertThat(spaLogData.getSessionType()).isEqualTo(SettingsEnums.SESSION_BROWSE)
    }

    @Test
    fun getSessionType_hasSessionSearchExtraData_returnSessionSearch() {
        bundle.putString(LOG_DATA_SESSION_NAME, SESSION_SEARCH)
        val spaLogData = SpaLogData(TEST_PID, LogEvent.PAGE_ENTER, bundle, dataModel)

        assertThat(spaLogData.getSessionType()).isEqualTo(SettingsEnums.SESSION_SEARCH)
    }

    @Test
    fun getSessionType_hasSessionUnknownExtraData_returnSessionUnknow() {
        bundle.putString(LOG_DATA_SESSION_NAME, "SESSION_OTHER")
        val spaLogData = SpaLogData(TEST_PID, LogEvent.PAGE_ENTER, bundle, dataModel)

        assertThat(spaLogData.getSessionType()).isEqualTo(SettingsEnums.SESSION_UNKNOWN)
    }

    @Test
    fun getPageId_withPageEvent_returnInputId() {
        val spaLogData1 = SpaLogData(TEST_PID, LogEvent.PAGE_ENTER, bundle, dataModel)
        assertThat(spaLogData1.getPageId()).isEqualTo(TEST_PID)

        val spaLogData2 = SpaLogData(TEST_PID, LogEvent.PAGE_LEAVE, bundle, dataModel)
        assertThat(spaLogData2.getPageId()).isEqualTo(TEST_PID)
    }
}
