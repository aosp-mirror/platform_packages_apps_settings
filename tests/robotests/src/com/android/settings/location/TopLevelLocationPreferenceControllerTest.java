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

package com.android.settings.location;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.location.LocationManager;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class TopLevelLocationPreferenceControllerTest {
    private static final String PREFERENCE_KEY = "top_level_location";
    private Context mContext;
    private TopLevelLocationPreferenceController mController;
    private LocationManager mLocationManager;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new TopLevelLocationPreferenceController(mContext, PREFERENCE_KEY);
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
    }

    @Test
    public void isAvailable_byDefault_shouldReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void getSummary_whenLocationIsOff_shouldReturnStringForOff() {
        mLocationManager.setLocationEnabledForUser(false, android.os.Process.myUserHandle());
        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.location_settings_summary_location_off));
    }

    @Test
    public void getSummary_whenLocationIsOn_shouldShowLoadingString() {
        mLocationManager.setLocationEnabledForUser(true, android.os.Process.myUserHandle());
        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.location_settings_loading_app_permission_stats));
    }

    @Test
    public void getSummary_whenLocationAppCountIsOne_shouldShowSingularString() {
        final int LOCATION_APP_COUNT = 1;
        mLocationManager.setLocationEnabledForUser(true, android.os.Process.myUserHandle());
        mController.setLocationAppCount(LOCATION_APP_COUNT);
        assertThat(mController.getSummary()).isEqualTo(
                mContext.getResources().getQuantityString(
                        R.plurals.location_settings_summary_location_on,
                        LOCATION_APP_COUNT, LOCATION_APP_COUNT));
    }

    @Test
    public void getSummary_whenLocationAppCountIsGreaterThanOne_shouldShowPluralString() {
        final int LOCATION_APP_COUNT = 5;
        mLocationManager.setLocationEnabledForUser(true, android.os.Process.myUserHandle());
        mController.setLocationAppCount(LOCATION_APP_COUNT);
        assertThat(mController.getSummary()).isEqualTo(
                mContext.getResources().getQuantityString(
                        R.plurals.location_settings_summary_location_on,
                        LOCATION_APP_COUNT, LOCATION_APP_COUNT));
    }
}