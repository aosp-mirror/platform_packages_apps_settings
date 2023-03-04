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

package com.android.settings.fuelgauge;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.preference.Preference;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class TopLevelBatteryPreferenceControllerTest {
    private Context mContext;
    private TopLevelBatteryPreferenceController mController;
    private BatterySettingsFeatureProvider mBatterySettingsFeatureProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(Robolectric.setupActivity(Activity.class));
        mController = new TopLevelBatteryPreferenceController(mContext, "test_key");
    }

    @Test
    public void getAvailibilityStatus_availableByDefault() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void convertClassPathToComponentName_nullInput_returnsNull() {
        assertThat(mController.convertClassPathToComponentName(null)).isNull();
    }

    @Test
    @Ignore
    public void convertClassPathToComponentName_emptyStringInput_returnsNull() {
        assertThat(mController.convertClassPathToComponentName("")).isNull();
    }

    @Test
    public void convertClassPathToComponentName_singleClassName_returnsCorrectComponentName() {
        ComponentName output = mController.convertClassPathToComponentName("ClassName");

        assertThat(output.getPackageName()).isEqualTo("");
        assertThat(output.getClassName()).isEqualTo("ClassName");
    }

    @Test
    public void convertClassPathToComponentName_validAddress_returnsCorrectComponentName() {
        ComponentName output = mController.convertClassPathToComponentName("my.fragment.ClassName");

        assertThat(output.getPackageName()).isEqualTo("my.fragment");
        assertThat(output.getClassName()).isEqualTo("ClassName");
    }

    @Test
    public void getDashboardLabel_returnsCorrectLabel() {
        mController.mPreference = new Preference(mContext);
        BatteryInfo info = new BatteryInfo();
        info.batteryPercentString = "3%";
        assertThat(mController.getDashboardLabel(mContext, info, true))
                .isEqualTo(info.batteryPercentString);

        info.remainingLabel = "Phone will shut down soon";
        assertThat(mController.getDashboardLabel(mContext, info, true))
                .isEqualTo("3% - Phone will shut down soon");

        info.discharging = false;
        info.chargeLabel = "5% - charging";
        assertThat(mController.getDashboardLabel(mContext, info, true)).isEqualTo("5% - charging");
    }

    @Test
    public void getSummary_batteryNotPresent_shouldShowWarningMessage() {
        mController.mIsBatteryPresent = false;

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getString(R.string.battery_missing_message));
    }
}
