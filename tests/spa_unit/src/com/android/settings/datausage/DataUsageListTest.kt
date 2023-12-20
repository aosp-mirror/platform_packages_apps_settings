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

package com.android.settings.datausage

import android.content.Context
import android.content.Intent
import android.net.NetworkTemplate
import android.os.UserManager
import android.provider.Settings
import android.telephony.SubscriptionManager
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragment
import androidx.fragment.app.testing.withFragment
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spaprivileged.framework.common.userManager
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

private val mockUserManager: UserManager = mock<UserManager>()

private val mockContext: Context = spy(ApplicationProvider.getApplicationContext()) {
    on { userManager } doReturn mockUserManager
}

private var fakeIntent = Intent()

@RunWith(AndroidJUnit4::class)
class DataUsageListTest {

    @Before
    fun setUp() {
        mockUserManager.stub {
            on { isGuestUser } doReturn false
        }
        fakeIntent = Intent()
    }

    @Test
    fun launchFragment_withoutArguments_finish() {
        val scenario = launchFragment<TestDataUsageList>(initialState = Lifecycle.State.CREATED)

        scenario.withFragment {
            assertThat(template).isNull()
            assertThat(subId).isEqualTo(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
            assertThat(activity!!.isFinishing).isTrue()
        }
    }

    @Test
    fun launchFragment_isGuestUser_finish() {
        mockUserManager.stub {
            on { isGuestUser } doReturn true
        }
        val fragmentArgs = bundleOf(
            DataUsageList.EXTRA_NETWORK_TEMPLATE to mock<NetworkTemplate>(),
            DataUsageList.EXTRA_SUB_ID to 3,
        )

        val scenario = launchFragment<TestDataUsageList>(
            fragmentArgs = fragmentArgs,
            initialState = Lifecycle.State.CREATED,
        )

        scenario.withFragment {
            assertThat(activity!!.isFinishing).isTrue()
        }
    }

    @Test
    fun launchFragment_withArguments_getTemplateFromArgument() {
        val fragmentArgs = bundleOf(
            DataUsageList.EXTRA_NETWORK_TEMPLATE to mock<NetworkTemplate>(),
            DataUsageList.EXTRA_SUB_ID to 3,
        )

        val scenario = launchFragment<TestDataUsageList>(
            fragmentArgs = fragmentArgs,
            initialState = Lifecycle.State.CREATED,
        )

        scenario.withFragment {
            assertThat(template).isNotNull()
            assertThat(subId).isEqualTo(3)
            assertThat(activity!!.isFinishing).isFalse()
        }
    }

    @Test
    fun launchFragment_withIntent_getTemplateFromIntent() {
        fakeIntent = Intent().apply {
            putExtra(Settings.EXTRA_NETWORK_TEMPLATE, mock<NetworkTemplate>())
            putExtra(Settings.EXTRA_SUB_ID, 2)
        }

        val scenario = launchFragment<TestDataUsageList>(initialState = Lifecycle.State.CREATED)

        scenario.withFragment {
            assertThat(template).isNotNull()
            assertThat(subId).isEqualTo(2)
            assertThat(activity!!.isFinishing).isFalse()
        }
    }
}

class TestDataUsageList : DataUsageList() {
    override fun getContext() = mockContext

    override fun getIntent() = fakeIntent
}
