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

package com.android.settings.spa.wifi.dpp

import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricPrompt
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settings.wifi.dpp.WifiDppUtils
import java.security.InvalidKeyException
import java.security.Key
import javax.crypto.Cipher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoSession
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@RunWith(AndroidJUnit4::class)
class WifiDppUtilsTest {
    private lateinit var mockSession: MockitoSession

    private val runnable = mock<Runnable>()
    private val cipher = mock<Cipher>()
    private var mockKeyguardManager = mock<KeyguardManager>()
    private var context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            on { getSystemService(KeyguardManager::class.java) } doReturn mockKeyguardManager
        }

    @Before
    fun setUp() {
        mockSession =
            ExtendedMockito.mockitoSession()
                .initMocks(this)
                .mockStatic(Cipher::class.java)
                .mockStatic(BiometricPrompt::class.java)
                .mockStatic(BiometricPrompt.Builder::class.java)
                .strictness(Strictness.LENIENT)
                .startMocking()
        whenever(context.applicationContext).thenReturn(context)
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
    }

    @Test
    fun showLockScreen_notKeyguardSecure_runRunnable() {
        mockKeyguardManager.stub { on { isKeyguardSecure } doReturn false }

        WifiDppUtils.showLockScreen(context, runnable)

        verify(runnable).run()
    }

    @Test
    fun showLockScreen_isKeyguardSecure_doNotRunRunnable() {
        mockKeyguardManager.stub { on { isKeyguardSecure } doReturn true }

        try {
            WifiDppUtils.showLockScreen(context, runnable)
        } catch (_: Exception) {}

        verify(runnable, never()).run()
    }

    @Test
    fun showLockScreenForWifiSharing_deviceUnlockedRecently_runRunnable() {
        mockKeyguardManager.stub { on { isKeyguardSecure } doReturn true }
        whenever(Cipher.getInstance(WifiDppUtils.AES_CBC_PKCS7_PADDING)).thenReturn(cipher)

        WifiDppUtils.showLockScreenForWifiSharing(context, runnable)

        verify(runnable).run()
    }

    @Test
    fun showLockScreenForWifiSharing_deviceNotUnlockedRecently_doNotRunRunnable() {
        mockKeyguardManager.stub { on { isKeyguardSecure } doReturn true }
        whenever(Cipher.getInstance(WifiDppUtils.AES_CBC_PKCS7_PADDING)).thenReturn(cipher)
        doThrow(InvalidKeyException()).whenever(cipher).init(anyInt(), any<Key>())

        try {
            WifiDppUtils.showLockScreenForWifiSharing(context, runnable)
        } catch (_: Exception) {}

        verify(runnable, never()).run()
    }
}
