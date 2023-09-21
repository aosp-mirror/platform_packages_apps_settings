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

package com.android.settings.system

import android.content.Context
import android.os.Bundle
import android.os.SystemUpdateManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SystemUpdateManagerExtKtTest {

    private val mockSystemUpdateManager = mock<SystemUpdateManager>()

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(SystemUpdateManager::class.java) } doReturn mockSystemUpdateManager
    }

    @Test
    fun getSystemUpdateInfo() = runTest {
        val bundle = Bundle()
        whenever(mockSystemUpdateManager.retrieveSystemUpdateInfo()).thenReturn(bundle)

        val info = context.getSystemUpdateInfo()

        assertThat(info).isSameInstanceAs(bundle)
    }
}
