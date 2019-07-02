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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AccessibilityUtilTest {
    private static final int ON = 1;
    private static final int OFF = 0;
    private static final String SECURE_TEST_KEY = "secure_test_key";
    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void getSummary_hasValueAndEqualsToOne_shouldReturnOnString() {
        Settings.Secure.putInt(mContext.getContentResolver(), SECURE_TEST_KEY, ON);

        final CharSequence result = AccessibilityUtil.getSummary(mContext, SECURE_TEST_KEY);

        assertThat(result)
                .isEqualTo(mContext.getText(R.string.accessibility_feature_state_on));
    }

    @Test
    public void getSummary_hasValueAndEqualsToZero_shouldReturnOffString() {
        Settings.Secure.putInt(mContext.getContentResolver(), SECURE_TEST_KEY, OFF);

        final CharSequence result = AccessibilityUtil.getSummary(mContext, SECURE_TEST_KEY);

        assertThat(result)
                .isEqualTo(mContext.getText(R.string.accessibility_feature_state_off));
    }

    @Test
    public void getSummary_noValue_shouldReturnOffString() {
        final CharSequence result = AccessibilityUtil.getSummary(mContext, SECURE_TEST_KEY);

        assertThat(result)
                .isEqualTo(mContext.getText(R.string.accessibility_feature_state_off));
    }
}
