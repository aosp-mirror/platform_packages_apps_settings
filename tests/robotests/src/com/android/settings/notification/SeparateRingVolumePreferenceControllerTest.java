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

package com.android.settings.notification;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Vibrator;
import android.provider.DeviceConfig;
import android.telephony.TelephonyManager;

import com.android.internal.config.sysui.SystemUiDeviceConfigFlags;
import com.android.settings.testutils.shadow.ShadowDeviceConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowDeviceConfig.class})
public class SeparateRingVolumePreferenceControllerTest {

    @Mock
    private AudioHelper mHelper;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private AudioManager mAudioManager;
    @Mock
    private Vibrator mVibrator;
    @Mock
    private NotificationManager mNotificationManager;
    @Mock
    private ComponentName mSuppressor;
    @Mock
    private Resources mResources;

    private Context mContext;

    private SeparateRingVolumePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowContext = ShadowApplication.getInstance();
        shadowContext.setSystemService(Context.TELEPHONY_SERVICE, mTelephonyManager);
        shadowContext.setSystemService(Context.AUDIO_SERVICE, mAudioManager);
        shadowContext.setSystemService(Context.VIBRATOR_SERVICE, mVibrator);
        shadowContext.setSystemService(Context.NOTIFICATION_SERVICE, mNotificationManager);
        mContext = spy(RuntimeEnvironment.application);
        when(mNotificationManager.getEffectsSuppressor()).thenReturn(mSuppressor);
        when(mContext.getResources()).thenReturn(mResources);
        mController = new SeparateRingVolumePreferenceController(mContext);
        mController.setAudioHelper(mHelper);
    }

    @Test
    public void isAvailable_ringNotificationAliased_shouldReturnFalse() {
        when(mHelper.isSingleVolume()).thenReturn(true);
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);

        assertThat(mController.isAvailable()).isFalse();
    }

    /**
     * Maintain that the device does not need to be voice capable to display this slider
     */
    @Test
    public void isAvailable_ringNotificationSeparated_isNotVoiceCapable_shouldReturnTrue() {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_SYSTEMUI,
                SystemUiDeviceConfigFlags.VOLUME_SEPARATE_NOTIFICATION, "true", false);
        when(mHelper.isSingleVolume()).thenReturn(false);
        when(mTelephonyManager.isVoiceCapable()).thenReturn(false);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void getAudioStream_shouldReturnRing() {
        assertThat(mController.getAudioStream()).isEqualTo(AudioManager.STREAM_RING);
    }

    @Test
    public void isSliceableCorrectKey_returnsTrue() {
        final SeparateRingVolumePreferenceController controller =
                new SeparateRingVolumePreferenceController(mContext);
        assertThat(controller.isSliceable()).isTrue();
    }

    @Test
    public void isPublicSlice_returnTrue() {
        assertThat(mController.isPublicSlice()).isTrue();
    }

}
