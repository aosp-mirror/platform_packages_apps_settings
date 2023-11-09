/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static android.provider.Settings.Secure.NOTIFICATION_BUBBLES;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;
import static com.android.settings.notification.BadgingNotificationPreferenceController.OFF;
import static com.android.settings.notification.BadgingNotificationPreferenceController.ON;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.content.Context;
import android.provider.Settings;
import android.widget.Switch;

import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.shadow.ShadowActivityManager;
import com.android.settingslib.testutils.shadow.ShadowInteractionJankMonitor;
import com.android.settingslib.widget.MainSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowInteractionJankMonitor.class,
        ShadowActivityManager.class,
})
public class BubbleNotificationPreferenceControllerTest {

    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;

    @Mock
    private Switch mSwitch;

    private BubbleNotificationPreferenceController mController;
    private MainSwitchPreference mPreference;

    private static final String KEY_NOTIFICATION_BUBBLES = "notification_bubbles";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new BubbleNotificationPreferenceController(mContext,
                KEY_NOTIFICATION_BUBBLES);
        mPreference = new MainSwitchPreference(RuntimeEnvironment.application);
        mPreference.setKey(mController.getPreferenceKey());
        when(mScreen.findPreference(mPreference.getKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void isAvailable_lowRam_returnsUnsupported() {
        final ShadowActivityManager activityManager =
                Shadow.extract(mContext.getSystemService(ActivityManager.class));
        activityManager.setIsLowRamDevice(true);
        assertEquals(UNSUPPORTED_ON_DEVICE, mController.getAvailabilityStatus());
    }

    @Test
    public void isAvailable_notLowRam_returnsAvailable() {
        final ShadowActivityManager activityManager =
                Shadow.extract(mContext.getSystemService(ActivityManager.class));
        activityManager.setIsLowRamDevice(false);
        assertEquals(AVAILABLE, mController.getAvailabilityStatus());
    }

    @Test
    public void updateState_settingIsOff_preferenceSetUnchecked() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, OFF);
        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                NOTIFICATION_BUBBLES, ON)).isEqualTo(OFF);

        mPreference.updateStatus(false);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void onSwitchChanged_true_settingIsOff_flagShouldOn() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, OFF);

        mController.onCheckedChanged(mSwitch, true);

        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                NOTIFICATION_BUBBLES, OFF)).isEqualTo(ON);
    }

    @Test
    public void onSwitchChanged_false_settingIsOn_flagShouldOff() {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, ON);

        mController.onCheckedChanged(mSwitch, false);

        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                NOTIFICATION_BUBBLES, ON)).isEqualTo(OFF);
    }

    @Test
    public void setChecked_setFalse_disablesSetting() {
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, ON);

        mController.setChecked(false);
        int updatedValue = Settings.Global.getInt(mContext.getContentResolver(),
                NOTIFICATION_BUBBLES, ON);

        assertThat(updatedValue).isEqualTo(OFF);
    }

    @Test
    public void setChecked_setTrue_enablesSetting() {
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES, OFF);

        mController.setChecked(true);
        int updatedValue = Settings.Global.getInt(mContext.getContentResolver(),
                NOTIFICATION_BUBBLES, OFF);

        assertThat(updatedValue).isEqualTo(ON);
    }

    @Test
    public void isSliceable_returnsFalse() {
        assertThat(mController.isSliceable()).isFalse();
    }
}
