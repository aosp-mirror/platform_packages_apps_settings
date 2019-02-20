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

import static android.provider.Settings.Secure.NOTIFICATION_DISMISS_RTL;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

@RunWith(RobolectricTestRunner.class)
public class SwipeDirectionPreferenceControllerTest {

    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;

    private SwipeDirectionPreferenceController mController;
    private ListPreference mPreference;

    private static final String KEY = "swipe";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new SwipeDirectionPreferenceController(mContext, KEY);
        mPreference = new ListPreference(RuntimeEnvironment.application);
        mPreference.setKey(mController.getPreferenceKey());
        when(mScreen.findPreference(mPreference.getKey())).thenReturn(mPreference);
    }

    @Test
    public void display_shouldDisplay() {
        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void updateState_SettingIsOn() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                NOTIFICATION_DISMISS_RTL,
                1);

        mController.updateState(mPreference);

        assertThat(mPreference.getValue()).isEqualTo("1");
    }

    @Test
    public void updateState_SettingIsOff() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                NOTIFICATION_DISMISS_RTL,
                0);

        mController.updateState(mPreference);

        assertThat(mPreference.getValue()).isEqualTo("0");
    }

    @Test
    public void onPreferenceChange_LTR() {
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_DISMISS_RTL, 1);

        mController.onPreferenceChange(mPreference, "0");

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                NOTIFICATION_DISMISS_RTL, 1)).isEqualTo(0);

        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getResources().getStringArray(R.array.swipe_direction_titles)[1]);
    }

    @Test
    public void onPreferenceChange_On() {
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_DISMISS_RTL, 0);

        mController.onPreferenceChange(mPreference, "1");

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                NOTIFICATION_DISMISS_RTL, 0)).isEqualTo(1);

        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getResources().getStringArray(R.array.swipe_direction_titles)[0]);
    }
}
