/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.format.DateFormat;

import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import java.util.List;
import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
public class AutoTimeFormatPreferenceControllerTest {

    @Mock
    private UpdateTimeAndDateCallback mCallback;

    private ShadowApplication mApplication;
    private Context mContext;
    private SwitchPreference mPreference;
    private TestAutoTimeFormatPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mApplication = ShadowApplication.getInstance();
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void updateState_24HourSet_shouldCheckPreference() {
        mController = new TestAutoTimeFormatPreferenceController(mContext, mCallback);
        mPreference = new SwitchPreference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        Settings.System.putString(mContext.getContentResolver(), Settings.System.TIME_12_24,
                TimeFormatPreferenceController.HOURS_24);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void updateState_12HourSet_shouldCheckPreference() {
        mController = new TestAutoTimeFormatPreferenceController(mContext, mCallback);
        mPreference = new SwitchPreference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        Settings.System.putString(mContext.getContentResolver(), Settings.System.TIME_12_24,
                TimeFormatPreferenceController.HOURS_12);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void updateState_autoSet_shouldNotCheckPreference() {
        mController = new TestAutoTimeFormatPreferenceController(mContext, mCallback);
        mPreference = new SwitchPreference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        Settings.System.putString(mContext.getContentResolver(), Settings.System.TIME_12_24, null);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void updatePreference_autoSet_shouldSendIntent_12HourLocale() {
        mController = new TestAutoTimeFormatPreferenceController(mContext, mCallback);
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
    public void updatePreference_autoSet_shouldSendIntent_24HourLocale() {
        mController = new TestAutoTimeFormatPreferenceController(mContext, mCallback);
        mPreference = new SwitchPreference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        mPreference.setChecked(false);

        mController.setIs24HourLocale(true);
        boolean result = mController.handlePreferenceTreeClick(mPreference);

        assertThat(result).isTrue();

        List<Intent> intentsFired = mApplication.getBroadcastIntents();
        assertThat(intentsFired.size()).isEqualTo(1);
        Intent intentFired = intentsFired.get(0);
        assertThat(intentFired.getAction()).isEqualTo(Intent.ACTION_TIME_CHANGED);
        assertThat(intentFired.getIntExtra(Intent.EXTRA_TIME_PREF_24_HOUR_FORMAT, -1))
                .isEqualTo(Intent.EXTRA_TIME_PREF_VALUE_USE_24_HOUR);
    }

    @Test
    public void updatePreference_24HourSet_shouldSendIntent() {
        mController = new TestAutoTimeFormatPreferenceController(mContext, mCallback);
        mPreference = new SwitchPreference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        mPreference.setChecked(true);

        mController.setIs24HourLocale(false);
        boolean result = mController.handlePreferenceTreeClick(mPreference);

        assertThat(result).isTrue();

        List<Intent> intentsFired = mApplication.getBroadcastIntents();
        assertThat(intentsFired.size()).isEqualTo(1);
        Intent intentFired = intentsFired.get(0);
        assertThat(intentFired.getAction()).isEqualTo(Intent.ACTION_TIME_CHANGED);
        assertThat(intentFired.getIntExtra(Intent.EXTRA_TIME_PREF_24_HOUR_FORMAT, -1))
                .isEqualTo(Intent.EXTRA_TIME_PREF_VALUE_USE_LOCALE_DEFAULT);
    }

    /**
     * Extend class under test to change {@link #is24HourLocale} to not call
     * {@link DateFormat#is24HourLocale(Locale)} because that's not available in roboelectric.
     */
    private static class TestAutoTimeFormatPreferenceController
            extends AutoTimeFormatPreferenceController {

        private boolean is24HourLocale = false;

        private TestAutoTimeFormatPreferenceController(Context context,
              UpdateTimeAndDateCallback callback) {
            super(context, callback);
        }

        void setIs24HourLocale(boolean value) {
            is24HourLocale = value;
        }

        @Override
        boolean is24HourLocale(Locale locale) {
            return is24HourLocale;
        }
    }
}
