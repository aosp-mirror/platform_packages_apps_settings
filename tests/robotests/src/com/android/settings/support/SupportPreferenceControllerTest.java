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

package com.android.settings.support;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;

import android.app.Activity;

import androidx.preference.Preference;

import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class SupportPreferenceControllerTest {


    private Activity mActivity;
    private FakeFeatureFactory mFeatureFactory;
    private Preference mPreference;

    @Before
    public void setUp() {
        mActivity = Robolectric.setupActivity(Activity.class);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mPreference = new Preference(mActivity);
        mPreference.setKey("test_key");
    }

    @Test
    public void getAvailability_noSupport_unavailable() {
        ReflectionHelpers.setField(mFeatureFactory, "supportFeatureProvider", null);
        assertThat(new SupportPreferenceController(mActivity, "test_key").getAvailabilityStatus())
                .isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailability_hasSupport_available() {
        assertThat(new SupportPreferenceController(mActivity, "test_key").getAvailabilityStatus())
                .isEqualTo(AVAILABLE);
    }

    @Test
    public void handlePreferenceTreeClick_shouldLaunchSupport() {
        final SupportPreferenceController controller = new SupportPreferenceController(mActivity,
                mPreference.getKey());
        controller.setActivity(mActivity);

        assertThat(controller.handlePreferenceTreeClick(mPreference)).isTrue();
        verify(mFeatureFactory.supportFeatureProvider).startSupport(mActivity);
    }
}
