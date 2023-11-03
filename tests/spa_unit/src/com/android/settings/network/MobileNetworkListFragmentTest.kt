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

package com.android.settings.network

import android.content.Context
import android.content.res.Resources
import android.os.UserManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.spaprivileged.framework.common.userManager
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class MobileNetworkListFragmentTest {
    private val mockUserManager = mock<UserManager>()

    private val mockResources = mock<Resources>()

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { userManager } doReturn mockUserManager
        on { resources } doReturn mockResources
    }

    @Test
    fun isPageSearchEnabled_adminUser_shouldReturnTrue() {
        mockUserManager.stub {
            on { isAdminUser } doReturn true
        }
        mockResources.stub {
            on { getBoolean(R.bool.config_show_sim_info) } doReturn true
        }

        val isEnabled =
            MobileNetworkListFragment.SEARCH_INDEX_DATA_PROVIDER.isPageSearchEnabled(context)

        assertThat(isEnabled).isTrue()
    }

    @Test
    fun isPageSearchEnabled_nonAdminUser_shouldReturnFalse() {
        mockUserManager.stub {
            on { isAdminUser } doReturn false
        }
        mockResources.stub {
            on { getBoolean(R.bool.config_show_sim_info) } doReturn true
        }

        val isEnabled =
            MobileNetworkListFragment.SEARCH_INDEX_DATA_PROVIDER.isPageSearchEnabled(context)

        assertThat(isEnabled).isFalse()
    }
}
