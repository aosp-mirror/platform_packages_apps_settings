/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.applications.appinfo;

import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AppAllServicesPreferenceControllerTest {

    @Mock
    private AppInfoDashboardFragment mFragment;
    @Mock
    private Preference mPreference;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private AppAllServicesPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = spy(new AppAllServicesPreferenceController(mContext, "test_key"));
        mController.setParentFragment(mFragment);
        mController.setPackageName("Package1");
        final String key = mController.getPreferenceKey();
        when(mPreference.getKey()).thenReturn(key);
    }

    @Test
    public void getAvailabilityStatus_shouldReturnAvailable() {
        doReturn(true).when(mController).canPackageHandleIntent();
        doReturn(true).when(mController).isLocationProvider();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_canNotHandleIntent_shouldReturnConditionallyUnavailable() {
        doReturn(false).when(mController).canPackageHandleIntent();
        doReturn(true).when(mController).isLocationProvider();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_isNotLocationProvider_shouldReturnConditionallyUnavailable() {
        doReturn(true).when(mController).canPackageHandleIntent();
        doReturn(false).when(mController).isLocationProvider();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_shouldReturnConditionallyUnavailable() {
        doReturn(false).when(mController).canPackageHandleIntent();
        doReturn(false).when(mController).isLocationProvider();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void canPackageHandleIntent_nullPackageInfo_shouldNotCrash() {
        mController.setPackageName(null);
        mController.canPackageHandleIntent();
        // no crash
    }

}
