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

import static android.provider.Settings.Secure.NOTIFICATION_NEW_INTERRUPTION_MODEL;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

@RunWith(RobolectricTestRunner.class)
public class SilentLockscreenPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;

    private Context mContext;
    private SilentLockscreenPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new SilentLockscreenPreferenceController(mContext);
        mPreference = new Preference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @Test
    public void isAvailable_featureEnabled() {
        Settings.Secure.putInt(
                mContext.getContentResolver(), NOTIFICATION_NEW_INTERRUPTION_MODEL, 1);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_featureDisabled() {
        Settings.Secure.putInt(
                mContext.getContentResolver(), NOTIFICATION_NEW_INTERRUPTION_MODEL, 0);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isChecked_settingIsOff_false() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS, 0);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_settingIsOn_true() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS, 1);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void onPreferenceChange_on() {
        mController.onPreferenceChange(mPreference, true);
        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS, 0)).isEqualTo(1);
    }

    @Test
    public void onPreferenceChange_off() {
        mController.onPreferenceChange(mPreference, false);
        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS, 1)).isEqualTo(0);
    }

    @Test
    public void listenerTriggered() {
        SilentLockscreenPreferenceController.Listener listener = mock(
                SilentLockscreenPreferenceController.Listener.class);
        mController.setListener(listener);

        mController.setChecked(false);
        verify(listener).onChange(false);

        mController.setChecked(true);
        verify(listener).onChange(true);
    }
}

