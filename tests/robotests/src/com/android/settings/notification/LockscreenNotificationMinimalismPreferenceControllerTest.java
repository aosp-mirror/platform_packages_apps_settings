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

package com.android.settings.notification;

import static android.provider.Settings.Secure.LOCK_SCREEN_NOTIFICATION_MINIMALISM;
import static android.provider.Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.server.notification.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Disable FLAG_NOTIFICATION_LOCK_SCREEN_SETTINGS because this toggle will be replaced by the new
 * settings page.
 */
@RunWith(RobolectricTestRunner.class)
@DisableFlags(Flags.FLAG_NOTIFICATION_LOCK_SCREEN_SETTINGS)
public class LockscreenNotificationMinimalismPreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;

    private LockscreenNotificationMinimalismPreferenceController mController;
    private Preference mPreference;
    static final int ON = 1;
    static final int OFF = 0;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(mock(DevicePolicyManager.class)).when(mContext)
                .getSystemService(Context.DEVICE_POLICY_SERVICE);
        mController = new LockscreenNotificationMinimalismPreferenceController(mContext,
                "key");
        mPreference = new Preference(RuntimeEnvironment.application);
        mPreference.setKey(mController.getPreferenceKey());
        when(mScreen.findPreference(mPreference.getKey())).thenReturn(mPreference);
    }

    @Test
    @DisableFlags(Flags.FLAG_NOTIFICATION_MINIMALISM)
    public void display_featureFlagOff_shouldNotDisplay() {
        // Given: lockscreen show notifications, FLAG_NOTIFICATION_MINIMALISM is disabled
        Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_NOTIFICATIONS, ON);

        // When: displayPreference
        mController.displayPreference(mScreen);

        // Then: The controller is CONDITIONALLY_UNAVAILABLE, preference is not visible
        assertThat(mPreference.isVisible()).isFalse();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @EnableFlags(com.android.server.notification.Flags.FLAG_NOTIFICATION_MINIMALISM)
    public void display_featureFlagOn_shouldDisplay() {
        // Given: lockscreen show notifications, FLAG_NOTIFICATION_MINIMALISM is enabled
        Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_NOTIFICATIONS, ON);

        // When: displayPreference
        mController.displayPreference(mScreen);

        // Then: The controller is AVAILABLE, preference is visible
        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @EnableFlags(com.android.server.notification.Flags.FLAG_NOTIFICATION_MINIMALISM)
    public void isChecked_settingIsOff_shouldReturnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_NOTIFICATION_MINIMALISM, OFF);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    @EnableFlags(com.android.server.notification.Flags.FLAG_NOTIFICATION_MINIMALISM)
    public void isChecked_settingIsOn_shouldReturnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_NOTIFICATION_MINIMALISM, ON);

        assertThat(mController.isChecked()).isTrue();
    }
}
