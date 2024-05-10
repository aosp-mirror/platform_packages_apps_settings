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

package com.android.settings.deviceinfo.regulatory

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.SystemProperties
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.toBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.dx.mockito.inline.extended.ExtendedMockito.doReturn
import com.android.settings.deviceinfo.regulatory.RegulatoryInfo.KEY_COO
import com.android.settings.deviceinfo.regulatory.RegulatoryInfo.KEY_SKU
import com.android.settings.deviceinfo.regulatory.RegulatoryInfo.getRegulatoryInfo
import com.android.settings.tests.spa_unit.R
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoSession
import org.mockito.Spy
import org.mockito.quality.Strictness

@RunWith(AndroidJUnit4::class)
class RegulatoryInfoTest {
    private lateinit var mockSession: MockitoSession

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        mockSession = ExtendedMockito.mockitoSession()
            .initMocks(this)
            .mockStatic(SystemProperties::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
    }

    @Test
    fun getRegulatoryInfo_noSkuProperty_shouldReturnDefaultLabel() {
        doReturn("").`when` { SystemProperties.get(KEY_SKU) }

        val regulatoryInfo = context.getRegulatoryInfo()

        assertDrawableSameAs(regulatoryInfo, R.drawable.regulatory_info)
    }

    @Test
    fun getResourceId_noCooProperty_shouldReturnSkuLabel() {
        doReturn("sku").`when` { SystemProperties.get(KEY_SKU) }
        doReturn("").`when` { SystemProperties.get(KEY_COO) }

        val regulatoryInfo = context.getRegulatoryInfo()

        assertDrawableSameAs(regulatoryInfo, R.drawable.regulatory_info_sku)
    }

    @Test
    fun getResourceId_hasSkuAndCooProperties_shouldReturnCooLabel() {
        doReturn("sku1").`when` { SystemProperties.get(KEY_SKU) }
        doReturn("coo").`when` { SystemProperties.get(KEY_COO) }

        val regulatoryInfo = context.getRegulatoryInfo()

        assertDrawableSameAs(regulatoryInfo, R.drawable.regulatory_info_sku1_coo)
    }

    @Test
    fun getResourceId_noCorrespondingCooLabel_shouldReturnSkuLabel() {
        doReturn("sku").`when` { SystemProperties.get(KEY_SKU) }
        doReturn("unknown").`when` { SystemProperties.get(KEY_COO) }

        val regulatoryInfo = context.getRegulatoryInfo()

        assertDrawableSameAs(regulatoryInfo, R.drawable.regulatory_info_sku)
    }

    private fun assertDrawableSameAs(drawable: Drawable?, @DrawableRes resId: Int) {
        val expected = context.getDrawable(resId)!!.toBitmap()
        assertThat(drawable!!.toBitmap().sameAs(expected)).isTrue()
    }
}
