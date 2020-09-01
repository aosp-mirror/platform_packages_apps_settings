/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class FontSizePreferenceControllerTest {

    private static final String TEST_KEY = "test_key";

    private Context mContext;
    private FontSizePreferenceController mController;
    private String[] mFontSizeArray;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new FontSizePreferenceController(mContext, TEST_KEY);
        mFontSizeArray = mContext.getResources().getStringArray(R.array.entries_font_size);
    }

    @Test
    public void isAlwaysAvailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getSummary_noScale_shouldReturnDefaultSummary() {
        assertThat(mController.getSummary()).isEqualTo(mFontSizeArray[1]);
    }

    @Test
    public void getSummary_smallScale_shouldReturnSmall() {
        Settings.System.putFloat(mContext.getContentResolver(),
                Settings.System.FONT_SCALE, 0.5f);
        assertThat(mController.getSummary()).isEqualTo(mFontSizeArray[0]);
    }

    @Test
    public void getSummary_largeScale_shouldReturnLarge() {
        Settings.System.putFloat(mContext.getContentResolver(),
                Settings.System.FONT_SCALE, 1.5f);
        assertThat(mController.getSummary()).isEqualTo(mFontSizeArray[3]);
    }
}
