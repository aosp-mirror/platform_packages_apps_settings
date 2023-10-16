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

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for {@link MetricsDataModel}. */
@RunWith(AndroidJUnit4::class)
class MetricsDataModelTest {
    private val TEST_PID = "pseudo_page_id"

    private lateinit var metricsDataModel: MetricsDataModel

    @Before
    fun setUp() {
        metricsDataModel = MetricsDataModel()
    }

    @Test
    fun initMetricsDataModel() {
        assertThat(metricsDataModel.pageTimeStampList.size).isEqualTo(0)
    }

    @Test
    fun addTimeStamp_addOnePageTimeStamp_sizeShouldBeOne() {
        metricsDataModel.addTimeStamp(PageTimeStamp(TEST_PID, System.currentTimeMillis()))

        assertThat(metricsDataModel.pageTimeStampList.size).isEqualTo(1)
    }

    @Test
    fun addTimeStamp_addTwoSamePageTimeStamp_sizeShouldBeTwo() {
        metricsDataModel.addTimeStamp(PageTimeStamp(TEST_PID, System.currentTimeMillis()))
        metricsDataModel.addTimeStamp(PageTimeStamp(TEST_PID, System.currentTimeMillis()))

        assertThat(metricsDataModel.pageTimeStampList.size).isEqualTo(2)
    }

    @Test
    fun getPageDuration_getExistPageId_mustFoundValue() {
        metricsDataModel.addTimeStamp(PageTimeStamp(TEST_PID, System.currentTimeMillis()))
        SystemClock.sleep(5)

        assertThat(metricsDataModel.getPageDuration(TEST_PID).toInt()).isGreaterThan(0)
        assertThat(metricsDataModel.pageTimeStampList.size).isEqualTo(0)
    }

    @Test
    fun getPageDuration_getNonExistPageId_valueShouldBeZero() {
        metricsDataModel.addTimeStamp(PageTimeStamp(TEST_PID, System.currentTimeMillis()))

        assertThat(metricsDataModel.getPageDuration("WRONG_ID").toLong()).isEqualTo(0L)
    }

    @Test
    fun getPageDuration_getExistPageIdAndDonotRemoved_sizeShouldBeOne() {
        metricsDataModel.addTimeStamp(PageTimeStamp(TEST_PID, System.currentTimeMillis()))
        SystemClock.sleep(5)

        assertThat(metricsDataModel.getPageDuration(TEST_PID, false).toLong()).isGreaterThan(0L)
        assertThat(metricsDataModel.pageTimeStampList.size).isEqualTo(1)
    }

    @Test
    fun getPageDuration_getTwoExistPageId_theOrderIsLIFO() {
        metricsDataModel.addTimeStamp(PageTimeStamp(TEST_PID, 10000L))
        metricsDataModel.addTimeStamp(PageTimeStamp(TEST_PID, 20000L))

        // The formula is d1 = t1 - 20000, d2 = t2 - 10000
        // d2 - d1 = t2 - t1 + 10000, because t2 > t1 the result of d2 - d1 is greater 10000
        val duration1 = metricsDataModel.getPageDuration(TEST_PID).toLong()
        SystemClock.sleep(5)
        val duration2 = metricsDataModel.getPageDuration(TEST_PID).toLong()

        assertThat(duration2 - duration1).isGreaterThan(10000L)
    }
}