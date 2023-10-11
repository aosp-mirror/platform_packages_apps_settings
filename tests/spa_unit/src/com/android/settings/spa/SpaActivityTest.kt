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
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settings.spa.SpaActivity.Companion.isSuwAndPageBlocked
import com.android.settings.spa.SpaActivity.Companion.startSpaActivity
import com.android.settings.spa.SpaActivity.Companion.startSpaActivityForApp
import com.android.settings.spa.app.AllAppListPageProvider
import com.android.settings.spa.app.appinfo.AppInfoSettingsProvider
import com.android.settingslib.spa.framework.util.KEY_DESTINATION
import com.google.android.setupcompat.util.WizardManagerHelper
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoSession
import org.mockito.quality.Strictness
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class SpaActivityTest {
    private lateinit var mockSession: MockitoSession

    @Mock
    private lateinit var context: Context

    @Before
    fun setUp() {
        mockSession = ExtendedMockito.mockitoSession()
            .initMocks(this)
            .mockStatic(WizardManagerHelper::class.java)
            .strictness(Strictness.LENIENT)
            .startMocking()
        whenever(context.applicationContext).thenReturn(context)
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
    }

    @Test
    fun isSuwAndPageBlocked_regularPage_notBlocked() {
        val isBlocked = context.isSuwAndPageBlocked(AllAppListPageProvider.name)

        assertThat(isBlocked).isFalse()
    }

    @Test
    fun isSuwAndPageBlocked_blocklistedPageInSuw_blocked() {
        whenever(WizardManagerHelper.isDeviceProvisioned(context)).thenReturn(false)

        val isBlocked = context.isSuwAndPageBlocked(AppInfoSettingsProvider.name)

        assertThat(isBlocked).isTrue()
    }

    @Test
    fun isSuwAndPageBlocked_blocklistedPageNotInSuw_notBlocked() {
        whenever(WizardManagerHelper.isDeviceProvisioned(context)).thenReturn(true)

        val isBlocked = context.isSuwAndPageBlocked(AppInfoSettingsProvider.name)

        assertThat(isBlocked).isFalse()
    }

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
        }

        context.startSpaActivityForApp(DESTINATION, intent)

        val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
        verify(context).startActivity(intentCaptor.capture())
        val capturedIntent = intentCaptor.value
        assertThat(capturedIntent.component?.className).isEqualTo(SpaActivity::class.qualifiedName)
        assertThat(capturedIntent.getStringExtra(KEY_DESTINATION))
            .isEqualTo("Destination/package.name/${UserHandle.myUserId()}")
    }

    private companion object {
        const val DESTINATION = "Destination"
        const val PACKAGE_NAME = "package.name"
    }
}
