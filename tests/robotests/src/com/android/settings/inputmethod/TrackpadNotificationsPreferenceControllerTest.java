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

package com.android.settings.inputmethod;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link TrackpadNotificationsPreferenceController} */
@RunWith(RobolectricTestRunner.class)
public class TrackpadNotificationsPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "gesture_notifications";
    private static final String SETTING_KEY = Settings.Secure.TRACKPAD_GESTURE_NOTIFICATION_ENABLED;

    private Context mContext;
    private TrackpadNotificationsPreferenceController mController;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mController = new TrackpadNotificationsPreferenceController(mContext, PREFERENCE_KEY);
    }

    @Test
    public void getAvailabilityStatus_expected() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getSliceHighlightMenuRes_expected() {
        assertThat(mController.getSliceHighlightMenuRes()).isEqualTo(R.string.menu_key_system);
    }

    @Test
    public void setChecked_true_shouldReturn1() {
        mController.setChecked(true);

        int result = Settings.Secure.getInt(mContext.getContentResolver(), SETTING_KEY, 1);

        assertThat(result).isEqualTo(1);
    }

    @Test
    public void setChecked_false_shouldReturn0() {
        mController.setChecked(false);

        int result = Settings.Secure.getInt(mContext.getContentResolver(), SETTING_KEY, 1);

        assertThat(result).isEqualTo(0);
    }

    @Test
    public void isChecked_providerPutInt1_returnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(), SETTING_KEY, 1);

        boolean result = mController.isChecked();

        assertThat(result).isTrue();
    }

    @Test
    public void isChecked_providerPutInt0_returnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(), SETTING_KEY, 0);

        boolean result = mController.isChecked();

        assertThat(result).isFalse();
    }
}
