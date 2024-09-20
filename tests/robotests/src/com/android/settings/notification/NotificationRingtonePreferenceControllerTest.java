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
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import com.android.server.notification.Flags;
import com.android.settings.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class NotificationRingtonePreferenceControllerTest {

    private NotificationRingtonePreferenceController mController;
    @Mock private Context mMockContext;
    @Mock private Resources mMockResources;
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mMockContext.getResources()).thenReturn(mMockResources);
        mController = new NotificationRingtonePreferenceController(mMockContext);
    }

    @Test
    @DisableFlags(Flags.FLAG_NOTIFICATION_VIBRATION_IN_SOUND_URI)
    public void isAvailable_byDefault_isTrue() {
        when(mMockResources
                .getBoolean(com.android.internal.R.bool.config_ringtoneVibrationSettingsSupported))
                .thenReturn(false);
        when(mMockResources.getBoolean(R.bool.config_show_notification_ringtone))
                .thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    @Config(qualifiers = "mcc999")
    @DisableFlags(Flags.FLAG_NOTIFICATION_VIBRATION_IN_SOUND_URI)
    public void isAvailable_whenNotVisible_isFalse() {
        when(mMockResources
                .getBoolean(com.android.internal.R.bool.config_ringtoneVibrationSettingsSupported))
                .thenReturn(false);
        when(mMockResources.getBoolean(R.bool.config_show_notification_ringtone))
                .thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_NOTIFICATION_VIBRATION_IN_SOUND_URI)
    public void isAvailable_whenFlagsNotificationVibrationInSoundUri_isFalse() {
        when(mMockResources
                .getBoolean(com.android.internal.R.bool.config_ringtoneVibrationSettingsSupported))
                .thenReturn(true);
        when(mMockResources.getBoolean(R.bool.config_show_notification_ringtone))
                .thenReturn(true);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void getRingtoneType_shouldReturnNotification() {
        assertThat(mController.getRingtoneType()).isEqualTo(RingtoneManager.TYPE_NOTIFICATION);
    }
}
