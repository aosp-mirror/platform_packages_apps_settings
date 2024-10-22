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

package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.fuelgauge.BatteryOptimizeHistoricalLogEntry.Action;
import com.android.settingslib.PrimarySwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BackgroundUsageAllowabilityPreferenceControllerTest {
    private static final int UID = 12345;
    private static final String PACKAGE_NAME = "com.android.app";

    private int mTestMode;
    private Context mContext;
    private BackgroundUsageAllowabilityPreferenceController mBackgroundUsageController;
    private BatteryOptimizeUtils mBatteryOptimizeUtils;

    @Mock DashboardFragment mDashboardFragment;
    @Mock PrimarySwitchPreference mBackgroundUsageAllowabilityPreference;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        prepareTestBatteryOptimizationUtils();
        mBackgroundUsageController =
                spy(
                        new BackgroundUsageAllowabilityPreferenceController(
                                mContext,
                                mDashboardFragment,
                                /* preferenceKey= */ "test",
                                mBatteryOptimizeUtils));
        mBackgroundUsageController.mBackgroundUsageAllowabilityPreference =
                mBackgroundUsageAllowabilityPreference;
    }

    @Test
    public void initPreferences_immutableOptimized_setExpectedContent() {
        doReturn(false).when(mBatteryOptimizeUtils).isOptimizeModeMutable();
        doReturn(true).when(mBatteryOptimizeUtils).isDisabledForOptimizeModeOnly();

        mBackgroundUsageController.initPreferences();

        verify(mBackgroundUsageAllowabilityPreference).setEnabled(false);
        verify(mBackgroundUsageAllowabilityPreference).setSwitchEnabled(false);
        verify(mBackgroundUsageAllowabilityPreference)
                .setSummary(
                        mContext.getString(
                                R.string.manager_battery_usage_footer_limited,
                                mContext.getString(R.string.manager_battery_usage_optimized_only)));
        verify(mBackgroundUsageAllowabilityPreference, never())
                .setOnPreferenceChangeListener(any());
        verify(mBackgroundUsageAllowabilityPreference, never()).setOnPreferenceClickListener(any());
    }

    @Test
    public void initPreferences_immutableUnrestricted_setExpectedContent() {
        doReturn(false).when(mBatteryOptimizeUtils).isOptimizeModeMutable();
        doReturn(false).when(mBatteryOptimizeUtils).isDisabledForOptimizeModeOnly();
        doReturn(true).when(mBatteryOptimizeUtils).isSystemOrDefaultApp();

        mBackgroundUsageController.initPreferences();

        verify(mBackgroundUsageAllowabilityPreference).setEnabled(false);
        verify(mBackgroundUsageAllowabilityPreference).setSwitchEnabled(false);
        verify(mBackgroundUsageAllowabilityPreference)
                .setSummary(
                        mContext.getString(
                                R.string.manager_battery_usage_footer_limited,
                                mContext.getString(
                                        R.string.manager_battery_usage_unrestricted_only)));
        verify(mBackgroundUsageAllowabilityPreference, never())
                .setOnPreferenceChangeListener(any());
        verify(mBackgroundUsageAllowabilityPreference, never()).setOnPreferenceClickListener(any());
    }

    @Test
    public void initPreferences_mutableMode_setExpectedContent() {
        doReturn(true).when(mBatteryOptimizeUtils).isOptimizeModeMutable();
        doReturn(false).when(mBatteryOptimizeUtils).isDisabledForOptimizeModeOnly();
        doReturn(false).when(mBatteryOptimizeUtils).isSystemOrDefaultApp();

        mBackgroundUsageController.initPreferences();

        verify(mBackgroundUsageAllowabilityPreference).setEnabled(true);
        verify(mBackgroundUsageAllowabilityPreference).setSwitchEnabled(true);
        verify(mBackgroundUsageAllowabilityPreference)
                .setSummary(
                        mContext.getString(
                                R.string.manager_battery_usage_allow_background_usage_summary));
        verify(mBackgroundUsageAllowabilityPreference).setOnPreferenceChangeListener(any());
        verify(mBackgroundUsageAllowabilityPreference).setOnPreferenceClickListener(any());
    }

    @Test
    public void updatePreferences_setIntoUnrestrictedMode_setExpectedPrefStatus() {
        mTestMode = BatteryOptimizeUtils.MODE_UNRESTRICTED;

        mBackgroundUsageController.updatePreferences(mTestMode);

        verifyPreferences(mTestMode);
    }

    @Test
    public void updatePreferences_setIntoOptimizedMode_setExpectedPrefStatus() {
        mTestMode = BatteryOptimizeUtils.MODE_OPTIMIZED;

        mBackgroundUsageController.updatePreferences(mTestMode);

        verifyPreferences(mTestMode);
    }

    @Test
    public void updatePreferences_setIntoRestrictedMode_setExpectedPrefStatus() {
        mTestMode = BatteryOptimizeUtils.MODE_RESTRICTED;

        mBackgroundUsageController.updatePreferences(mTestMode);

        verifyPreferences(mTestMode);
    }

    @Test
    public void handleBatteryOptimizeModeUpdated_modeChange_setExpectedPrefStatus() {
        mTestMode = BatteryOptimizeUtils.MODE_RESTRICTED;

        mBackgroundUsageController.handleBatteryOptimizeModeUpdated(
                BatteryOptimizeUtils.MODE_OPTIMIZED);

        verify(mBatteryOptimizeUtils)
                .setAppUsageState(BatteryOptimizeUtils.MODE_OPTIMIZED, Action.APPLY);
        assertThat(mTestMode).isEqualTo(BatteryOptimizeUtils.MODE_OPTIMIZED);
        verifyPreferences(mTestMode);
    }

    @Test
    public void handleBatteryOptimizeModeUpdated_modeNotChange_setExpectedPrefStatus() {
        mTestMode = BatteryOptimizeUtils.MODE_RESTRICTED;

        mBackgroundUsageController.handleBatteryOptimizeModeUpdated(
                BatteryOptimizeUtils.MODE_RESTRICTED);

        verify(mBatteryOptimizeUtils, never()).setAppUsageState(anyInt(), any());
        assertThat(mTestMode).isEqualTo(BatteryOptimizeUtils.MODE_RESTRICTED);
        verify(mBackgroundUsageController, never()).updatePreferences(mTestMode);
    }

    private void prepareTestBatteryOptimizationUtils() {
        mBatteryOptimizeUtils = spy(new BatteryOptimizeUtils(mContext, UID, PACKAGE_NAME));
        Answer<Void> setTestMode =
                invocation -> {
                    mTestMode = invocation.getArgument(0);
                    return null;
                };
        doAnswer(setTestMode).when(mBatteryOptimizeUtils).setAppUsageState(anyInt(), any());
        Answer<Integer> getTestMode = invocation -> mTestMode;
        doAnswer(getTestMode).when(mBatteryOptimizeUtils).getAppOptimizationMode();
    }

    private void verifyPreferences(int mode) {
        boolean isAllowBackground = mode != BatteryOptimizeUtils.MODE_RESTRICTED;
        verify(mBackgroundUsageAllowabilityPreference).setChecked(isAllowBackground);
    }
}
