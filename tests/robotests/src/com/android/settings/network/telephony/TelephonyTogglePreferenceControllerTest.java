/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.network.telephony;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class TelephonyTogglePreferenceControllerTest {

    private Context mContext;
    private FakeTelephonyToggle mFakeTelephonyToggle;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mFakeTelephonyToggle = new FakeTelephonyToggle(mContext, "key");
    }

    @Test
    public void isSliceable_byDefault_shouldReturnFalse() {
        assertThat(mFakeTelephonyToggle.isSliceable()).isFalse();
    }

    private static class FakeTelephonyToggle extends TelephonyTogglePreferenceController {

        private FakeTelephonyToggle(Context context, String preferenceKey) {
            super(context, preferenceKey);
        }

        @Override
        public boolean isChecked() {
            return false;
        }

        @Override
        public boolean setChecked(boolean isChecked) {
            return false;
        }

        @Override
        public int getAvailabilityStatus(int subId) {
            return 0;
        }
    }
}
