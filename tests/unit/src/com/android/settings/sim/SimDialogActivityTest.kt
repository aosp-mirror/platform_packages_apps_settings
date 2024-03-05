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

package com.android.settings.sim

import android.content.Context
import android.os.UserManager
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spaprivileged.framework.common.userManager
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
@UiThreadTest
class SimDialogActivityTest {
    @get:Rule
    val mockito: MockitoRule = MockitoJUnit.rule()

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var userManager: UserManager

    private lateinit var activity: SimDialogActivity

    @Before
    fun setUp() {
        activity = MockSimDialogActivity()
        whenever(context.userManager).thenReturn(userManager)
        whenever(userManager.isGuestUser).thenReturn(false)
        whenever(userManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS))
            .thenReturn(false)
    }

    @Test
    fun isUiRestricted_normally_returnFalse() {
        assertThat(activity.isUiRestricted).isFalse()
    }

    @Test
    fun isUiRestricted_isGuestUser_returnTrue() {
        whenever(userManager.isGuestUser).thenReturn(true)

        assertThat(activity.isUiRestricted).isTrue()
    }

    @Test
    fun isUiRestricted_hasUserRestriction_returnTrue() {
        whenever(userManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS))
            .thenReturn(true)

        assertThat(activity.isUiRestricted).isTrue()
    }

    inner class MockSimDialogActivity : SimDialogActivity() {
        override fun getApplicationContext() = context
    }
}
