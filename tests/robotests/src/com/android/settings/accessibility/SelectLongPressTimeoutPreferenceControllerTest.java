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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link SelectLongPressTimeoutPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class SelectLongPressTimeoutPreferenceControllerTest {
    private static final int SHORT_VALUE = 400;
    private static final int MEDIUM_VALUE = 1000;
    private static final int LONG_VALUE = 1500;
    private static final int INVALID_VALUE = 0;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private SelectLongPressTimeoutPreferenceController mController;
    private ListPreference mPreference;

    @Before
    public void setUp() {
        mController = new SelectLongPressTimeoutPreferenceController(mContext, "press_timeout");
        mPreference = new ListPreference(mContext);
        mPreference.setEntries(R.array.long_press_timeout_selector_titles);
        mPreference.setEntryValues(R.array.long_press_timeout_selector_values);
        mPreference.setSummary("%s");
    }

    @Test
    public void getAvailabilityStatus_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void updateState_toShortTimeout_shouldReturnShortSummary() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LONG_PRESS_TIMEOUT, SHORT_VALUE);
        mController.updateState(mPreference);

        assertThat(mPreference.getSummary().toString()).isEqualTo("Short");
    }

    @Test
    public void updateState_toMediumTimeout_shouldReturnMediumSummary() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LONG_PRESS_TIMEOUT, MEDIUM_VALUE);
        mController.updateState(mPreference);

        assertThat(mPreference.getSummary().toString()).isEqualTo("Medium");
    }

    @Test
    public void updateState_toLongTimeout_shouldReturnLongSummary() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LONG_PRESS_TIMEOUT, LONG_VALUE);
        mController.updateState(mPreference);

        assertThat(mPreference.getSummary().toString()).isEqualTo("Long");
    }

    @Test
    public void updateState_toInvalidTimeout_shouldReturnEmptySummary() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LONG_PRESS_TIMEOUT, INVALID_VALUE);
        mController.updateState(mPreference);

        assertThat(mPreference.getSummary().toString()).isEmpty();
    }
}
