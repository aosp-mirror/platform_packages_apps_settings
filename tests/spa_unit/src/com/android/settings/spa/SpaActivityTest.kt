/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.spa

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.UserHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.spa.SpaActivity.Companion.startSpaActivity
import com.android.settings.spa.SpaActivity.Companion.startSpaActivityForApp
import com.android.settingslib.spa.framework.util.KEY_DESTINATION
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

@RunWith(AndroidJUnit4::class)
class SpaActivityTest {
    @get:Rule
    val mockito: MockitoRule = MockitoJUnit.rule()

    @Mock
    private lateinit var context: Context

    @Test
    fun startSpaActivity() {
        context.startSpaActivity(DESTINATION)

        val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
        verify(context).startActivity(intentCaptor.capture())
        val intent = intentCaptor.value
        assertThat(intent.component?.className).isEqualTo(SpaActivity::class.qualifiedName)
        assertThat(intent.getStringExtra(KEY_DESTINATION)).isEqualTo(DESTINATION)
    }

    @Test
    fun startSpaActivityForApp() {
        val intent = Intent().apply {
            data = Uri.parse("package:$PACKAGE_NAME")
            putExtra(Intent.EXTRA_USER_HANDLE, UserHandle.of(USER_ID))
        }

        context.startSpaActivityForApp(DESTINATION, intent)

        val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
        verify(context).startActivity(intentCaptor.capture())
        val capturedIntent = intentCaptor.value
        assertThat(capturedIntent.component?.className).isEqualTo(SpaActivity::class.qualifiedName)
        assertThat(capturedIntent.getStringExtra(KEY_DESTINATION))
            .isEqualTo("Destination/package.name/1")
    }

    private companion object {
        const val DESTINATION = "Destination"
        const val PACKAGE_NAME = "package.name"
        const val USER_ID = 1
    }
}
