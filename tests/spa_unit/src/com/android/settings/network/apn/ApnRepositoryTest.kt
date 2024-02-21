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

package com.android.settings.network.apn

import android.content.ContentResolver
import android.content.Context
import android.database.MatrixCursor
import android.net.Uri
import android.provider.Telephony
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ApnRepositoryTest {

    private val contentResolver = mock<ContentResolver>()

    private val mockSubscriptionInfo = mock<SubscriptionInfo> {
        on { mccString } doReturn MCC
        on { mncString } doReturn MNC
    }

    private val mockSubscriptionManager = mock<SubscriptionManager> {
        on { getActiveSubscriptionInfo(SUB_ID) } doReturn mockSubscriptionInfo
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { contentResolver } doReturn contentResolver
        on { getSystemService(SubscriptionManager::class.java) } doReturn mockSubscriptionManager
    }
    private val uri = mock<Uri> {}

    @Test
    fun getApnDataFromUri() {
        // mock out resources and the feature provider
        val cursor = MatrixCursor(Projection)
        cursor.addRow(
            arrayOf<Any>(
                0,
                "name",
                "apn",
                "proxy",
                "port",
                "userName",
                "server",
                "passWord",
                "mmsc",
                "mmsProxy",
                "mmsPort",
                0,
                "apnType",
                "apnProtocol",
                0,
                0,
                "apnRoaming",
                0,
                1,
            )
        )
        whenever(contentResolver.query(uri, Projection, null, null, null)).thenReturn(cursor)

        val apnData = getApnDataFromUri(uri, context)

        assertThat(apnData.name).isEqualTo("name")
    }

    @Test
    fun getApnIdMap_knownCarrierId() {
        mockSubscriptionInfo.stub {
            on { carrierId } doReturn CARRIER_ID
        }

        val idMap = context.getApnIdMap(SUB_ID)

        assertThat(idMap).containsExactly(Telephony.Carriers.CARRIER_ID, CARRIER_ID)
    }

    @Test
    fun getApnIdMap_unknownCarrierId() {
        mockSubscriptionInfo.stub {
            on { carrierId } doReturn TelephonyManager.UNKNOWN_CARRIER_ID
        }

        val idMap = context.getApnIdMap(SUB_ID)

        assertThat(idMap).containsExactly(Telephony.Carriers.NUMERIC, MCC + MNC)
    }

    private companion object {
        const val SUB_ID = 2
        const val CARRIER_ID = 10
        const val MCC = "310"
        const val MNC = "101"
    }
}
