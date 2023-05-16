/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;

import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class TimeFeedbackPreferenceCategoryControllerTest {

    private TestTimeFeedbackPreferenceCategoryController mController;
    @Mock private AbstractPreferenceController mChildController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Context context = RuntimeEnvironment.getApplication();

        mController = new TestTimeFeedbackPreferenceCategoryController(context, "test_key");
        mController.addChildController(mChildController);
    }

    @Test
    public void getAvailabilityStatus_featureEnabledPrimary() {
        mController.setTimeFeedbackFeatureEnabled(false);

        when(mChildController.isAvailable()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_childControllerSecondary() {
        mController.setTimeFeedbackFeatureEnabled(true);

        when(mChildController.isAvailable()).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);

        when(mChildController.isAvailable()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    /**
     * Extend class under test to change {@link #isTimeFeedbackFeatureEnabled} to not call
     * {@link TimeFeedbackLaunchUtils} because that's non-trivial to fake.
     */
    private static class TestTimeFeedbackPreferenceCategoryController
            extends TimeFeedbackPreferenceCategoryController {

        private boolean mTimeFeedbackFeatureEnabled;

        TestTimeFeedbackPreferenceCategoryController(Context context, String preferenceKey) {
            super(context, preferenceKey);
        }

        void setTimeFeedbackFeatureEnabled(boolean value) {
            this.mTimeFeedbackFeatureEnabled = value;
        }

        @Override
        protected boolean isTimeFeedbackFeatureEnabled() {
            return mTimeFeedbackFeatureEnabled;
        }
    }
}
