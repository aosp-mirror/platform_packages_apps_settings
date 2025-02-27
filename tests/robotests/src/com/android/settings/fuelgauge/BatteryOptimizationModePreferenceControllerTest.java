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

import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BatteryOptimizationModePreferenceControllerTest {
    private static final int UID = 12345;
    private static final String PACKAGE_NAME = "com.android.app";

    private int mTestMode;
    private Context mContext;
    private BatteryOptimizationModePreferenceController mBackgroundUsageController;
    private BatteryOptimizeUtils mBatteryOptimizeUtils;

    @Mock MainSwitchPreference mBackgroundUsageAllowabilityPreference;
    @Mock SelectorWithWidgetPreference mOptimizedPreference;
    @Mock SelectorWithWidgetPreference mUnrestrictedPreference;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        prepareTestBatteryOptimizationUtils();
        mBackgroundUsageController =
                spy(
                        new BatteryOptimizationModePreferenceController(
                                mContext, "test", mBatteryOptimizeUtils));
        mBackgroundUsageController.mBackgroundUsageAllowabilityPreference =
                mBackgroundUsageAllowabilityPreference;
        mBackgroundUsageController.mOptimizedPreference = mOptimizedPreference;
        mBackgroundUsageController.mUnrestrictedPreference = mUnrestrictedPreference;
    }

    @Test
    public void initPreferences_mutableMode_setEnabled() {
        doReturn(true).when(mBatteryOptimizeUtils).isOptimizeModeMutable();

        mBackgroundUsageController.initPreferences();

        verify(mBackgroundUsageAllowabilityPreference).setEnabled(true);
        verify(mOptimizedPreference).setEnabled(true);
        verify(mUnrestrictedPreference).setEnabled(true);
        verify(mBackgroundUsageAllowabilityPreference, never()).setOnPreferenceClickListener(any());
        verify(mBackgroundUsageAllowabilityPreference).setOnPreferenceChangeListener(any());
        verify(mOptimizedPreference).setOnPreferenceClickListener(any());
        verify(mUnrestrictedPreference).setOnPreferenceClickListener(any());
    }

    @Test
    public void initPreferences_immutableMode_setDisabledAndSkipSetListeners() {
        doReturn(false).when(mBatteryOptimizeUtils).isOptimizeModeMutable();

        mBackgroundUsageController.initPreferences();

        verify(mBackgroundUsageAllowabilityPreference).setEnabled(false);
        verify(mOptimizedPreference).setEnabled(false);
        verify(mUnrestrictedPreference).setEnabled(false);
        verify(mBackgroundUsageAllowabilityPreference, never()).setOnPreferenceClickListener(any());
        verify(mBackgroundUsageAllowabilityPreference, never())
                .setOnPreferenceChangeListener(any());
        verify(mOptimizedPreference, never()).setOnPreferenceClickListener(any());
        verify(mUnrestrictedPreference, never()).setOnPreferenceClickListener(any());
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
                .setAppUsageState(
                        BatteryOptimizeUtils.MODE_OPTIMIZED,
                        BatteryOptimizeHistoricalLogEntry.Action.APPLY);
        assertThat(mTestMode).isEqualTo(BatteryOptimizeUtils.MODE_OPTIMIZED);
        verifyPreferences(mBatteryOptimizeUtils.getAppOptimizationMode());
    }

    @Test
    public void handleBatteryOptimizeModeUpdated_modeNotChange_setExpectedPrefStatus() {
        mTestMode = BatteryOptimizeUtils.MODE_RESTRICTED;

        mBackgroundUsageController.handleBatteryOptimizeModeUpdated(
                BatteryOptimizeUtils.MODE_RESTRICTED);

        verify(mBatteryOptimizeUtils, never()).setAppUsageState(anyInt(), any());
        assertThat(mTestMode).isEqualTo(BatteryOptimizeUtils.MODE_RESTRICTED);
        verify(mBackgroundUsageController, never()).updatePreferences(anyInt());
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
        verify(mOptimizedPreference).setEnabled(isAllowBackground);
        verify(mUnrestrictedPreference).setEnabled(isAllowBackground);
        verify(mOptimizedPreference).setChecked(mode == BatteryOptimizeUtils.MODE_OPTIMIZED);
        verify(mUnrestrictedPreference).setChecked(mode == BatteryOptimizeUtils.MODE_UNRESTRICTED);
    }
}
