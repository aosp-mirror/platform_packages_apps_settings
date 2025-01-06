/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.media.RingtoneManager;
import android.media.audio.Flags;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.telephony.TelephonyManager;


import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PhoneRingtonePreferenceControllerTest {

    @Mock
    private TelephonyManager mTelephonyManager;

    @Mock
    private Context mMockContext;

    @Mock
    private Resources mMockResources;

    private PhoneRingtonePreferenceController mController;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mMockContext.getResources()).thenReturn(mMockResources);
        when(mMockContext.getSystemService(
                Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        mController = new PhoneRingtonePreferenceController(mMockContext);
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_RINGTONE_HAPTICS_CUSTOMIZATION)
    public void isAvailable_notVoiceCapable_shouldReturnFalse() {
        when(mMockResources
                .getBoolean(com.android.internal.R.bool.config_ringtoneVibrationSettingsSupported))
                .thenReturn(false);
        when(mTelephonyManager.isVoiceCapable()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_RINGTONE_HAPTICS_CUSTOMIZATION)
    public void isAvailable_VoiceCapable_shouldReturnTrue() {
        when(mMockResources
                .getBoolean(com.android.internal.R.bool.config_ringtoneVibrationSettingsSupported))
                .thenReturn(false);
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_RINGTONE_HAPTICS_CUSTOMIZATION)
    public void isAvailable_vibrationSupported_shouldReturnFalse() {
        when(mMockResources
                .getBoolean(com.android.internal.R.bool.config_ringtoneVibrationSettingsSupported))
                .thenReturn(true);
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void getRingtoneType_shouldReturnRingtone() {
        assertThat(mController.getRingtoneType()).isEqualTo(RingtoneManager.TYPE_RINGTONE);
    }
}
