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

package com.android.settings.datetime;


import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.verify;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class TimeFormatPreferenceControllerTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;
    @Mock
    private UpdateTimeAndDateCallback mCallback;

    private ShadowApplication mApplication;
    private Context mContext;
    private SwitchPreference mPreference;
    private TimeFormatPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mApplication = ShadowApplication.getInstance();
        mContext = mApplication.getApplicationContext();
    }

    @Test
    public void isCalledFromSUW_NotAvailable() {
        mController = new TimeFormatPreferenceController(mContext, mCallback, true);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void notCalledFromSUW_shouldBeAvailable() {
        mController = new TimeFormatPreferenceController(mContext, mCallback, false);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_24HourSet_shouldCheckPreference() {
        mController = new TimeFormatPreferenceController(mContext, mCallback, false);
        mPreference = new SwitchPreference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        Settings.System.putString(mContext.getContentResolver(), Settings.System.TIME_12_24,
                TimeFormatPreferenceController.HOURS_24);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void updateState_12HourSet_shouldNotCheckPreference() {
        mController = new TimeFormatPreferenceController(mContext, mCallback, false);
        mPreference = new SwitchPreference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        Settings.System.putString(mContext.getContentResolver(), Settings.System.TIME_12_24,
                TimeFormatPreferenceController.HOURS_12);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void updatePreference_12HourSet_shouldSendIntent() {
        mController = new TimeFormatPreferenceController(mContext, mCallback, false);
        mPreference = new SwitchPreference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        mPreference.setChecked(false);

        boolean result = mController.handlePreferenceTreeClick(mPreference);

        assertThat(result).isTrue();

        List<Intent> intentsFired = mApplication.getBroadcastIntents();
        assertThat(intentsFired.size()).isEqualTo(1);
        Intent intentFired = intentsFired.get(0);
        assertThat(intentFired.getAction()).isEqualTo(Intent.ACTION_TIME_CHANGED);
        assertThat(intentFired.getIntExtra(Intent.EXTRA_TIME_PREF_24_HOUR_FORMAT, -1))
                .isEqualTo(Intent.EXTRA_TIME_PREF_VALUE_USE_12_HOUR);
    }

    @Test
    public void updatePreference_24HourSet_shouldSendIntent() {
        mController = new TimeFormatPreferenceController(mContext, mCallback, false);
        mPreference = new SwitchPreference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        mPreference.setChecked(true);

        boolean result = mController.handlePreferenceTreeClick(mPreference);

        assertThat(result).isTrue();

        List<Intent> intentsFired = mApplication.getBroadcastIntents();
        assertThat(intentsFired.size()).isEqualTo(1);
        Intent intentFired = intentsFired.get(0);
        assertThat(intentFired.getAction()).isEqualTo(Intent.ACTION_TIME_CHANGED);
        assertThat(intentFired.getIntExtra(Intent.EXTRA_TIME_PREF_24_HOUR_FORMAT, -1))
                .isEqualTo(Intent.EXTRA_TIME_PREF_VALUE_USE_24_HOUR);
    }
}
