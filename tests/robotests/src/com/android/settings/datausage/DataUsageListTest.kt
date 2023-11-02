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
import android.os.Bundle
import android.os.UserManager
import android.provider.Settings
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import com.android.settings.datausage.DataUsageListTest.ShadowDataUsageBaseFragment
import com.android.settings.datausage.TemplatePreference.NetworkServices
import com.android.settings.datausage.lib.BillingCycleRepository
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settingslib.NetworkPolicyEditor
import com.android.settingslib.core.AbstractPreferenceController
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowDataUsageBaseFragment::class])
class DataUsageListTest {
    @get:Rule
    val mockito: MockitoRule = MockitoJUnit.rule()

    @Mock
    private lateinit var networkServices: NetworkServices

    @Mock
    private lateinit var userManager: UserManager

    @Mock
    private lateinit var billingCycleRepository: BillingCycleRepository

    @Mock
    private lateinit var dataUsageListHeaderController: DataUsageListHeaderController

    @Spy
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Spy
    private val dataUsageList = TestDataUsageList()

    @Before
    fun setUp() {
        FakeFeatureFactory.setupForTest()
        networkServices.mPolicyEditor = mock(NetworkPolicyEditor::class.java)
        doReturn(context).`when`(dataUsageList).context
        doReturn(userManager).`when`(context).getSystemService(UserManager::class.java)
        doReturn(false).`when`(userManager).isGuestUser
        ReflectionHelpers.setField(dataUsageList, "services", networkServices)
        doNothing().`when`(dataUsageList).updateSubscriptionInfoEntity()
        `when`(billingCycleRepository.isBandwidthControlEnabled()).thenReturn(true)
        dataUsageList.dataUsageListHeaderController = dataUsageListHeaderController
    }

    @Test
    fun onCreate_isNotGuestUser_shouldNotFinish() {
        dataUsageList.template = mock<NetworkTemplate>(NetworkTemplate::class.java)
        doReturn(false).`when`(userManager).isGuestUser
        doNothing().`when`(dataUsageList).processArgument()
        dataUsageList.onCreate(null)
        verify(dataUsageList, never()).finish()
    }

    @Test
    fun onCreate_isGuestUser_shouldFinish() {
        doReturn(true).`when`(userManager).isGuestUser
        dataUsageList.onCreate(null)
        verify(dataUsageList).finish()
    }

    @Test
    fun processArgument_shouldGetTemplateFromArgument() {
        val args = Bundle()
        args.putParcelable(
            DataUsageList.EXTRA_NETWORK_TEMPLATE, mock(
                NetworkTemplate::class.java
            )
        )
        args.putInt(DataUsageList.EXTRA_SUB_ID, 3)
        dataUsageList.arguments = args
        dataUsageList.processArgument()
        assertThat(dataUsageList.template).isNotNull()
        assertThat(dataUsageList.subId).isEqualTo(3)
    }

    @Test
    fun processArgument_fromIntent_shouldGetTemplateFromIntent() {
        val intent = Intent()
        intent.putExtra(
            Settings.EXTRA_NETWORK_TEMPLATE, mock(
                NetworkTemplate::class.java
            )
        )
        intent.putExtra(Settings.EXTRA_SUB_ID, 3)
        doReturn(intent).`when`(dataUsageList).intent
        dataUsageList.processArgument()
        assertThat(dataUsageList.template).isNotNull()
        assertThat(dataUsageList.subId).isEqualTo(3)
    }

    @Test
    fun updatePolicy_setConfigButtonVisible() {
        dataUsageList.template = mock(NetworkTemplate::class.java)
        dataUsageList.onCreate(null)

        dataUsageList.updatePolicy()

        verify(dataUsageListHeaderController).setConfigButtonVisible(true)
    }

    @Implements(DataUsageBaseFragment::class)
    class ShadowDataUsageBaseFragment {
        @Implementation
        fun onCreate(@Suppress("UNUSED_PARAMETER") icicle: Bundle?) {
            // do nothing
        }
    }

    open inner class TestDataUsageList : DataUsageList() {
        override fun <T : AbstractPreferenceController?> use(clazz: Class<T>): T = mock(clazz)

        @Suppress("UNCHECKED_CAST")
        override fun <T : Preference?> findPreference(key: CharSequence): T =
            mock(Preference::class.java) as T

        public override fun getIntent() = Intent()

        override fun createBillingCycleRepository() = billingCycleRepository

        override fun isBillingCycleModifiable() = true
    }
}
