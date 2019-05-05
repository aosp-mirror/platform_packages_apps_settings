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

import com.android.settings.testutils.FakeFeatureFactory;

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
public class SilentStatusBarPreferenceControllerTest {

    @Mock
    private NotificationBackend mBackend;
    @Mock
    private PreferenceScreen mScreen;

    private Context mContext;
    private SilentStatusBarPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new SilentStatusBarPreferenceController(mContext);
        mController.setBackend(mBackend);
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
        when(mBackend.shouldHideSilentStatusBarIcons(any())).thenReturn(true);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_settingIsOn_true() {
        when(mBackend.shouldHideSilentStatusBarIcons(any())).thenReturn(false);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void onPreferenceChange_on() {
        mController.onPreferenceChange(mPreference, true);
        verify(mBackend).setHideSilentStatusIcons(false);
    }

    @Test
    public void onPreferenceChange_off() {
        mController.onPreferenceChange(mPreference, false);
        verify(mBackend).setHideSilentStatusIcons(true);
    }

    @Test
    public void listenerTriggered() {
        SilentStatusBarPreferenceController.Listener listener = mock(
                SilentStatusBarPreferenceController.Listener.class);
        mController.setListener(listener);

        mController.setChecked(false);
        verify(listener).onChange(false);

        mController.setChecked(true);
        verify(listener).onChange(true);
    }
}

