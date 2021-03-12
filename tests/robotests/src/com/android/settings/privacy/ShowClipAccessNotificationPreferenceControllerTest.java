/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ShowClipAccessNotificationPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;

    private Context mContext;
    private ShowClipAccessNotificationPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        mController = new ShowClipAccessNotificationPreferenceController(mContext);
        mPreference = new Preference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void isChecked_settingIsOff_shouldReturnFalse() throws Exception {
        setProperty(0);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_settingIsOn_shouldReturnTrue() throws Exception {
        setProperty(1);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void onPreferenceChange_turnOn_shouldChangeSettingTo1() throws Exception {
        setProperty(0);

        mController.onPreferenceChange(mPreference, true);

        assertThat(mController.isChecked()).isTrue();
        assertProperty(1);
    }

    @Test
    public void onPreferenceChange_turnOff_shouldChangeSettingTo0() throws Exception {
        setProperty(1);

        mController.onPreferenceChange(mPreference, false);

        assertThat(mController.isChecked()).isFalse();
        assertProperty(0);
    }

    private void setProperty(int newValue) {
        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Secure.putInt(
                contentResolver, Settings.Secure.CLIPBOARD_SHOW_ACCESS_NOTIFICATIONS, newValue);
    }

    private void assertProperty(int expectedValue) throws SettingNotFoundException {
        final ContentResolver contentResolver = mContext.getContentResolver();
        assertThat(Settings.Secure.getInt(
                contentResolver, Settings.Secure.CLIPBOARD_SHOW_ACCESS_NOTIFICATIONS))
                .isEqualTo(expectedValue);
    }
}
