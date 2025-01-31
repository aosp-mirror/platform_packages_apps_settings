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

package com.android.settings.notification;

import static android.provider.Settings.Secure.LOCK_SCREEN_SHOW_ONLY_UNSEEN_NOTIFICATIONS;

import static com.android.settings.notification.ShowOnlyUnseenNotificationsOnLockscreenPreferenceController.OFF;
import static com.android.settings.notification.ShowOnlyUnseenNotificationsOnLockscreenPreferenceController.ON;

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
public class ShowOnlyUnseenNotificationsOnLockscreenPreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;

    private ShowOnlyUnseenNotificationsOnLockscreenPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(mock(DevicePolicyManager.class)).when(mContext)
                .getSystemService(Context.DEVICE_POLICY_SERVICE);
        mController = new ShowOnlyUnseenNotificationsOnLockscreenPreferenceController(mContext,
                "key");
        mPreference = new Preference(RuntimeEnvironment.application);
        mPreference.setKey(mController.getPreferenceKey());
        when(mScreen.findPreference(mPreference.getKey())).thenReturn(mPreference);
    }

    @Test
    @DisableFlags(com.android.server.notification.Flags.FLAG_NOTIFICATION_MINIMALISM)
    public void display_configUnset_shouldNotDisplay() {
        mController.displayPreference(mScreen);
        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    @DisableFlags(com.android.server.notification.Flags.FLAG_NOTIFICATION_MINIMALISM)
    public void display_configSet_showDisplay() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_ONLY_UNSEEN_NOTIFICATIONS, OFF);
        mController.displayPreference(mScreen);
        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    @EnableFlags(com.android.server.notification.Flags.FLAG_NOTIFICATION_MINIMALISM)
    public void display_configUnset_minimalismEnabled_shouldDisplay() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_ONLY_UNSEEN_NOTIFICATIONS, ON);
        mController.displayPreference(mScreen);
        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void isChecked_settingIsOff_shouldReturnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_ONLY_UNSEEN_NOTIFICATIONS, OFF);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_settingIsOn_shouldReturnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_ONLY_UNSEEN_NOTIFICATIONS, ON);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void setChecked_setFalse_disablesSetting() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_ONLY_UNSEEN_NOTIFICATIONS, ON);

        mController.setChecked(false);
        int updatedValue = Settings.Secure.getInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_ONLY_UNSEEN_NOTIFICATIONS, -1);

        assertThat(updatedValue).isEqualTo(OFF);
    }

    @Test
    public void setChecked_setTrue_enablesSetting() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_ONLY_UNSEEN_NOTIFICATIONS, OFF);

        mController.setChecked(true);
        int updatedValue = Settings.Secure.getInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_ONLY_UNSEEN_NOTIFICATIONS, -1);

        assertThat(updatedValue).isEqualTo(ON);
    }
}
