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

package com.android.settings.privacy;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class EnableContentCapturePreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;

    private Context mContext;
    private EnableContentCapturePreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new EnableContentCapturePreferenceController(mContext, "THE_KEY_TO_SUCCESS");
        mPreference = new Preference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void isChecked_settingIsOff_false() throws Exception {
        setProperty(0);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_settingIsOn_true() throws Exception {
        setProperty(1);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void changePref_turnOn_shouldChangeSettingTo1() throws Exception {
        setProperty(0);

        mController.onPreferenceChange(mPreference, true);

        assertThat(mController.isChecked()).isTrue();
        assertProperty(1);
    }

    @Test
    public void changePref_turnOff_shouldChangeSettingTo0() throws Exception {
        setProperty(1);

        mController.onPreferenceChange(mPreference, false);

        assertThat(mController.isChecked()).isFalse();
        assertProperty(0);
    }

    private void setProperty(int newValue) throws SettingNotFoundException {
        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Secure.putInt(contentResolver, Settings.Secure.CONTENT_CAPTURE_ENABLED, newValue);
    }

    private void assertProperty(int expectedValue) throws SettingNotFoundException {
        final ContentResolver contentResolver = mContext.getContentResolver();
        assertThat(Settings.Secure.getInt(contentResolver, Settings.Secure.CONTENT_CAPTURE_ENABLED))
                .isEqualTo(expectedValue);
    }
}
