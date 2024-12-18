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

package com.android.settings.network.telephony

import android.content.Context
import android.telephony.TelephonyManager
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.network.ims.VolteQueryImsState
import com.android.settings.network.ims.VtQueryImsState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class VideoCallingPreferenceControllerTest {

    private val mockVtQueryImsState = mock<VtQueryImsState> {}

    private var mockQueryVoLteState = mock<VolteQueryImsState> {}

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val mockCallStateRepository = mock<CallStateRepository> {}

    private var controller =
        spy(
            VideoCallingPreferenceController(
                context = context,
                key = TEST_KEY,
                callStateRepository = mockCallStateRepository,
            )
        ) {
            on { queryImsState(SUB_ID) } doReturn mockVtQueryImsState
            on { queryVoLteState(SUB_ID) } doReturn mockQueryVoLteState
        }

    private val preference = SwitchPreferenceCompat(context).apply { key = TEST_KEY }
    private val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)

    @Before
    fun setUp() {
        controller.init(SUB_ID, CallingPreferenceCategoryController(context, "calling_category"))
        preferenceScreen.addPreference(preference)
        controller.displayPreference(preferenceScreen)
    }

    @Test
    fun updateState_4gLteOff_disabledAndUnchecked() {
        mockQueryVoLteState.stub { on { isEnabledByUser } doReturn false }

        controller.updateState(preference)

        assertThat(preference.isEnabled).isFalse()
        assertThat(preference.isChecked).isFalse()
    }

    @Test
    fun updateState_4gLteOnWithoutCall_enabledAndChecked() = runBlocking {
        mockVtQueryImsState.stub {
            on { isEnabledByUser } doReturn true
            on { isAllowUserControl } doReturn true
        }
        mockQueryVoLteState.stub { on { isEnabledByUser } doReturn true }
        mockCallStateRepository.stub {
            on { callStateFlow(SUB_ID) } doReturn flowOf(TelephonyManager.CALL_STATE_IDLE)
        }

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)
        controller.updateState(preference)

        assertThat(preference.isEnabled).isTrue()
        assertThat(preference.isChecked).isTrue()
    }

    @Test
    fun updateState_4gLteOnWithCall_disabledAndChecked() = runBlocking {
        mockVtQueryImsState.stub {
            on { isEnabledByUser } doReturn true
            on { isAllowUserControl } doReturn true
        }
        mockQueryVoLteState.stub { on { isEnabledByUser } doReturn true }
        mockCallStateRepository.stub {
            on { callStateFlow(SUB_ID) } doReturn flowOf(TelephonyManager.CALL_STATE_RINGING)
        }

        controller.onViewCreated(TestLifecycleOwner())
        delay(100)
        controller.updateState(preference)

        assertThat(preference.isEnabled).isFalse()
        assertThat(preference.isChecked).isTrue()
    }

    private companion object {
        const val TEST_KEY = "test_key"
        const val SUB_ID = 10
    }
}
