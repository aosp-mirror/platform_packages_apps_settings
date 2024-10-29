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

package com.android.settings.notification

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.media.AudioManager
import android.media.AudioManager.STREAM_BLUETOOTH_SCO
import android.media.AudioManager.STREAM_VOICE_CALL
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
class CallVolumePreferenceTest {
    private var audioHelper = mock<AudioHelper>()
    private var mockResources = mock<Resources>()

    private var audioManager: AudioManager? = null

    private var callVolumePreference = CallVolumePreference()
    private val context = object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
        override fun getSystemService(name: String): Any? =
            when (name) {
                Context.AUDIO_SERVICE -> audioManager
                else -> super.getSystemService(name)
            }

        override fun getResources(): Resources = mockResources
    }

    @Test
    fun isAvailable_configTrueAndNoSingleVolume_shouldReturnTrue() {
        mockResources.stub { on { getBoolean(anyInt()) } doReturn true }
        audioHelper = mock { on { isSingleVolume } doReturn false }
        callVolumePreference = spy(callVolumePreference).stub {
            onGeneric { createAudioHelper(context) } doReturn audioHelper
        }

        assertThat(callVolumePreference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_configTrueAndSingleVolume_shouldReturnFalse() {
        mockResources.stub { on { getBoolean(anyInt()) } doReturn true }
        audioHelper = mock { on { isSingleVolume } doReturn true }
        callVolumePreference = spy(callVolumePreference).stub {
            onGeneric { createAudioHelper(context) } doReturn audioHelper
        }

        assertThat(callVolumePreference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_configFalse_shouldReturnFalse() {
        mockResources.stub { on { getBoolean(anyInt()) } doReturn false }

        assertThat(callVolumePreference.isAvailable(context)).isFalse()
    }

    @Test
    @Suppress("DEPRECATION")
    fun getAudioStream_onBluetoothScoOn_shouldEqualToStreamBluetoothSco() {
        audioManager = mock { on { isBluetoothScoOn } doReturn true }

        assertThat(callVolumePreference.getAudioStream(context)).isEqualTo(STREAM_BLUETOOTH_SCO)
    }

    @Test
    @Suppress("DEPRECATION")
    fun getAudioStream_onBluetoothScoOff_shouldEqualToStreamVoiceCall() {
        audioManager = mock { on { isBluetoothScoOn } doReturn false }

        assertThat(callVolumePreference.getAudioStream(context)).isEqualTo(STREAM_VOICE_CALL)
    }
}
// LINT.ThenChange(CallVolumePreferenceControllerTest.java)
