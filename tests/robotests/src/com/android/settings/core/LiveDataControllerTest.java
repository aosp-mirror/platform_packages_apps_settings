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
package com.android.settings.core;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

@RunWith(RobolectricTestRunner.class)
public class LiveDataControllerTest {
    private static final String KEY = "test_key";
    private static final CharSequence TEST_DATA = "test_data";

    private Context mContext;
    private TestPreferenceController mTestController;
    private TestFragment mFragment;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mFragment = new TestFragment();
        mTestController = new TestPreferenceController(mContext, KEY);
    }

    @Test
    public void newController_returnPlaceHolderSummary() {
        assertThat(mTestController.getSummary()).isEqualTo(
                mContext.getText(R.string.summary_placeholder));
    }

    @Test
    @Config(shadows = ShadowLiveDataController.class)
    public void initLifeCycleOwner_returnTestData() {
        mTestController.initLifeCycleOwner(mFragment);
        assertThat(mTestController.getSummary()).isEqualTo(TEST_DATA);
    }

    static class TestPreferenceController extends LiveDataController {
        TestPreferenceController(Context context, String preferenceKey) {
            super(context, preferenceKey);
        }

        @Override
        public CharSequence getSummaryTextInBackground() {
            return TEST_DATA;
        }

        @Override
        public int getAvailabilityStatus() {
            return AVAILABLE;
        }
    }

    static class TestFragment extends SettingsPreferenceFragment {
        @Override
        public int getMetricsCategory() {
            return 0;
        }
    }

    @Implements(LiveDataController.class)
    public static class ShadowLiveDataController {
        @RealObject LiveDataController mRealController;

        @Implementation
        protected void initLifeCycleOwner(@NonNull Fragment fragment) {
            mRealController.mSummary = TEST_DATA;
        }
    }
}
