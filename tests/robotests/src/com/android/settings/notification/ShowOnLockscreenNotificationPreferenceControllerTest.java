/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_SECURE_NOTIFICATIONS;
import static android.provider.Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS;
import static android.provider.Settings.Secure.LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.RestrictedListPreference;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;
import com.android.settings.testutils.shadow.ShadowUserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import androidx.preference.PreferenceScreen;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class, ShadowRestrictedLockUtilsInternal.class})
public class ShowOnLockscreenNotificationPreferenceControllerTest {

    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;
    @Mock
    DevicePolicyManager mDpm;

    private ShowOnLockScreenNotificationPreferenceController mController;
    private RestrictedListPreference mPreference;

    private static final String KEY = "key";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new ShowOnLockScreenNotificationPreferenceController(mContext, KEY);
        mPreference = new RestrictedListPreference(
                mContext, Robolectric.buildAttributeSet().build());
        mPreference.setKey(mController.getPreferenceKey());
        when(mScreen.findPreference(mPreference.getKey())).thenReturn(mPreference);
        mController.setDpm(mDpm);
    }

    @Test
    public void display_shouldDisplay() {
        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void updateState_noNotifsOnLockscreen() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_NOTIFICATIONS,
                0);
        // should be ignored
        Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS,
                1);

        mController.updateState(mPreference);

        assertThat(mPreference.getValue()).isEqualTo(
                String.valueOf(R.string.lock_screen_notifs_show_none));

        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.lock_screen_notifs_show_none));
    }

    @Test
    public void updateState_alertingNotifsOnLockscreen() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_NOTIFICATIONS,
                1);
        Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS,
                0);

        mController.updateState(mPreference);

        assertThat(mPreference.getValue()).isEqualTo(
                String.valueOf(R.string.lock_screen_notifs_show_alerting));
        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.lock_screen_notifs_show_alerting));
    }

    @Test
    public void updateState_allNotifsOnLockscreen() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_NOTIFICATIONS,
                1);
        Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS,
                1);

        mController.updateState(mPreference);

        assertThat(mPreference.getValue()).isEqualTo(
                String.valueOf(R.string.lock_screen_notifs_show_all));
        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.lock_screen_notifs_show_all));
    }

    @Test
    public void updateState_allNotifsOnLockscreen_isDefault() {
        // settings don't exist

        mController.updateState(mPreference);

        assertThat(mPreference.getValue()).isEqualTo(
                String.valueOf(R.string.lock_screen_notifs_show_all));
        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.lock_screen_notifs_show_all));
    }

    @Test
    public void updateState_dpmSaysNo() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_NOTIFICATIONS,
                1);
        Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS,
                1);

        when(mDpm.getKeyguardDisabledFeatures(null))
                .thenReturn(KEYGUARD_DISABLE_SECURE_NOTIFICATIONS);
        ShadowRestrictedLockUtilsInternal.setKeyguardDisabledFeatures(
                KEYGUARD_DISABLE_SECURE_NOTIFICATIONS);

        mController.updateState(mPreference);

        assertThat(mPreference.getValue()).isEqualTo(
                String.valueOf(R.string.lock_screen_notifs_show_none));
        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.lock_screen_notifs_show_none));

        assertThat(mPreference.isRestrictedForEntry(
                mContext.getString(R.string.lock_screen_notifs_show_all))).isTrue();
        assertThat(mPreference.isRestrictedForEntry(
                mContext.getString(R.string.lock_screen_notifs_show_alerting))).isTrue();
    }

    @Test
    public void onPreferenceChange_allToAlerting() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_NOTIFICATIONS,
                1);
        Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS,
                1);

        mController.onPreferenceChange(mPreference,
                Integer.toString(R.string.lock_screen_notifs_show_alerting));

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_NOTIFICATIONS, 1)).isEqualTo(1);
        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS, 1)).isEqualTo(0);

        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.lock_screen_notifs_show_alerting));
    }

    @Test
    public void onPreferenceChange_noneToAll() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_NOTIFICATIONS,
                0);
        Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS,
                0);

        mController.onPreferenceChange(mPreference,
                Integer.toString(R.string.lock_screen_notifs_show_all));

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_NOTIFICATIONS, 1)).isEqualTo(1);
        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS, 1)).isEqualTo(1);

        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.lock_screen_notifs_show_all));
    }

    @Test
    public void onPreferenceChange_alertingToNone() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_NOTIFICATIONS,
                1);
        Settings.Secure.putInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS,
                0);

        mController.onPreferenceChange(mPreference,
                Integer.toString(R.string.lock_screen_notifs_show_none));

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_NOTIFICATIONS, 1)).isEqualTo(0);
        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS, 1)).isEqualTo(0);

        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.lock_screen_notifs_show_none));
    }
}
