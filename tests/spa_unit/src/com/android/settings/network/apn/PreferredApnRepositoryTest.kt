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

package com.android.settings.network.apn

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class PreferredApnRepositoryTest {

    private val contentResolver = mock<ContentResolver>()

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { contentResolver } doReturn contentResolver
    }

    private val repository = PreferredApnRepository(context, SUB_ID)

    @Test
    fun restorePreferredApn() {
        repository.restorePreferredApn()

        verify(contentResolver).delete(
            Uri.parse("content://telephony/carriers/restore/subId/2"),
            null,
            null,
        )
    }

    @Test
    fun setPreferredApn() {
        val apnId = "10"

        repository.setPreferredApn(apnId)

        verify(contentResolver).update(
            eq(Uri.parse("content://telephony/carriers/preferapn/subId/2")),
            argThat { getAsString(ApnSettings.APN_ID) == apnId },
            isNull(),
            isNull(),
        )
    }

    @Test
    fun preferredApnFlow() = runBlocking {
        val expectedPreferredApn = "10"
        val mockCursor = mock<Cursor> {
            on { moveToNext() } doReturn true
            on { getColumnIndex(Telephony.Carriers._ID) } doReturn 0
            on { getString(0) } doReturn expectedPreferredApn
        }
        contentResolver.stub {
            on {
                query(
                    Uri.parse("content://telephony/carriers/preferapn/subId/2"),
                    arrayOf(Telephony.Carriers._ID),
                    null,
                    null,
                    Telephony.Carriers.DEFAULT_SORT_ORDER,
                )
            } doReturn mockCursor
        }

        val preferredApn = repository.preferredApnFlow().firstWithTimeoutOrNull()

        assertThat(preferredApn).isEqualTo(expectedPreferredApn)
    }

    private companion object {
        const val SUB_ID = 2
    }
}
