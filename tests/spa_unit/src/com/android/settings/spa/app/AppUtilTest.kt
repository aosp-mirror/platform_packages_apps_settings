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

package com.android.settings.spa.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.UserHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.eq
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

@RunWith(AndroidJUnit4::class)
class AppUtilTest {
    @get:Rule
    val mockito: MockitoRule = MockitoJUnit.rule()

    @Mock
    private lateinit var context: Context

    @Test
    fun startUninstallActivity() {
        context.startUninstallActivity(packageName = PACKAGE_NAME, userHandle = USER_HANDLE)

        val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
        verify(context).startActivityAsUser(intentCaptor.capture(), eq(USER_HANDLE))
        val intent = intentCaptor.value
        assertThat(intent.action).isEqualTo(Intent.ACTION_UNINSTALL_PACKAGE)
        assertThat(intent.data).isEqualTo(Uri.parse("package:$PACKAGE_NAME"))
        assertThat(intent.extras?.getBoolean(Intent.EXTRA_UNINSTALL_ALL_USERS)).isFalse()
    }

    private companion object {
        const val PACKAGE_NAME = "package.name"
        val USER_HANDLE: UserHandle = UserHandle.of(0)
    }
}