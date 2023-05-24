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

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.R;
import com.android.settings.gestures.OneHandedSettingsUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class OneHandedPreferenceControllerTest {
    private Context mContext;
    private OneHandedPreferenceController mController;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mController = new OneHandedPreferenceController(mContext, "one_handed");
    }

    @Test
    public void getSummary_oneHandedEnabled_showsOnWithSummary() {
        OneHandedSettingsUtils.setOneHandedModeEnabled(mContext, true);

        assertThat(mController.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.preference_summary_default_combination,
                        mContext.getText(R.string.gesture_setting_on),
                        mContext.getText(R.string.one_handed_mode_intro_text)));
    }

    @Test
    public void getSummary_oneHandedDisabled_showsOffWithSummary() {
        OneHandedSettingsUtils.setOneHandedModeEnabled(mContext, false);

        assertThat(mController.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.preference_summary_default_combination,
                        mContext.getText(R.string.gesture_setting_off),
                        mContext.getText(R.string.one_handed_mode_intro_text)));
    }
}
