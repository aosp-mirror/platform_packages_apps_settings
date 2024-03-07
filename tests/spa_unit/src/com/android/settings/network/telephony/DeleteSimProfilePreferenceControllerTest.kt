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

package com.android.settings.network.telephony

import android.app.KeyguardManager
import android.content.Context
import android.os.UserManager
import android.telephony.SubscriptionInfo
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.network.SubscriptionUtil
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class DeleteSimProfilePreferenceControllerTest {
    private val subscriptionInfo = mock<SubscriptionInfo> {
        on { subscriptionId } doReturn SUB_ID
        on { isEmbedded } doReturn true
    }

    private val mockKeyguardManager = mock<KeyguardManager>() {
        on { isKeyguardSecure() } doReturn false
    }

    private var context: Context = spy(ApplicationProvider.getApplicationContext()) {
        doNothing().whenever(mock).startActivity(any())
        on { getSystemService(Context.KEYGUARD_SERVICE) } doReturn mockKeyguardManager
    }

    private val preference = Preference(context).apply { key = PREF_KEY }
    private val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)
        .apply { addPreference(preference) }
    private var controller = DeleteSimProfilePreferenceController(context, PREF_KEY)

    @Before
    fun setUp() {
        SubscriptionUtil.setAvailableSubscriptionsForTesting(listOf(subscriptionInfo))
    }

    @After
    fun tearDown() {
        SubscriptionUtil.setAvailableSubscriptionsForTesting(null)
    }

    @Test
    fun getAvailabilityStatus_noSubs_notAvailable() {
        SubscriptionUtil.setAvailableSubscriptionsForTesting(emptyList())

        controller.init(SUB_ID)

        assertThat(controller.isAvailable()).isFalse()
    }

    @Test
    fun getAvailabilityStatus_physicalSim_notAvailable() {
        whenever(subscriptionInfo.isEmbedded).thenReturn(false)

        controller.init(SUB_ID)

        assertThat(controller.isAvailable()).isFalse()
    }

    @Test
    fun getAvailabilityStatus_unknownSim_notAvailable() {
        whenever(subscriptionInfo.subscriptionId).thenReturn(OTHER_ID)

        controller.init(SUB_ID)

        assertThat(controller.isAvailable()).isFalse()
    }

    @Test
    fun getAvailabilityStatus_knownEsim_isAvailable() {
        controller.init(SUB_ID)

        assertThat(controller.isAvailable()).isTrue()
    }

    @Test
    fun onPreferenceClick_startsIntent() {
        controller.init(SUB_ID)
        controller.displayPreference(preferenceScreen)

        controller.handlePreferenceTreeClick(preference)

        verify(context, times(1)).startActivity(any())
    }

    private companion object {
        const val PREF_KEY = "delete_profile_key"
        const val SUB_ID = 1234
        const val OTHER_ID = 5678
    }
}
