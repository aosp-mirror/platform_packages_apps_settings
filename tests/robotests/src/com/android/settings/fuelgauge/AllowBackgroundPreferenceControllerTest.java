/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;

import com.android.settingslib.widget.MainSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AllowBackgroundPreferenceControllerTest {
    private static final int UID = 12345;
    private static final String PACKAGE_NAME = "com.android.app";

    private AllowBackgroundPreferenceController mController;
    private MainSwitchPreference mMainSwitchPreference;
    private BatteryOptimizeUtils mBatteryOptimizeUtils;

    @Mock private PackageManager mMockPackageManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        Context context = spy(RuntimeEnvironment.application);
        BatteryUtils.getInstance(context).reset();
        doReturn(UID)
                .when(mMockPackageManager)
                .getPackageUid(PACKAGE_NAME, PackageManager.GET_META_DATA);

        mController = new AllowBackgroundPreferenceController(context, UID, PACKAGE_NAME);
        mMainSwitchPreference = new MainSwitchPreference(RuntimeEnvironment.application);
        mBatteryOptimizeUtils = spy(new BatteryOptimizeUtils(context, UID, PACKAGE_NAME));
        mController.mBatteryOptimizeUtils = mBatteryOptimizeUtils;
    }

    @Test
    public void testUpdateState_isValidPackage_prefEnabled() {
        when(mBatteryOptimizeUtils.isDisabledForOptimizeModeOnly()).thenReturn(false);
        when(mBatteryOptimizeUtils.isSystemOrDefaultApp()).thenReturn(false);

        mController.updateState(mMainSwitchPreference);

        assertThat(mBatteryOptimizeUtils.isOptimizeModeMutable()).isTrue();
        assertThat(mMainSwitchPreference.isEnabled()).isTrue();
    }

    @Test
    public void testUpdateState_invalidPackage_prefDisabled() {
        when(mBatteryOptimizeUtils.isDisabledForOptimizeModeOnly()).thenReturn(true);
        when(mBatteryOptimizeUtils.isSystemOrDefaultApp()).thenReturn(false);

        mController.updateState(mMainSwitchPreference);

        assertThat(mBatteryOptimizeUtils.isOptimizeModeMutable()).isFalse();
        assertThat(mMainSwitchPreference.isEnabled()).isFalse();
    }

    @Test
    public void testUpdateState_isSystemOrDefaultAppAndRestrictedStates_prefChecked() {
        when(mBatteryOptimizeUtils.isDisabledForOptimizeModeOnly()).thenReturn(false);
        when(mBatteryOptimizeUtils.isSystemOrDefaultApp()).thenReturn(true);
        when(mBatteryOptimizeUtils.getAppOptimizationMode())
                .thenReturn(BatteryOptimizeUtils.MODE_RESTRICTED);

        mController.updateState(mMainSwitchPreference);

        assertThat(mMainSwitchPreference.isEnabled()).isFalse();
        assertThat(mMainSwitchPreference.isChecked()).isFalse();
    }

    @Test
    public void testUpdateState_isSystemOrDefaultApp_prefUnchecked() {
        when(mBatteryOptimizeUtils.isDisabledForOptimizeModeOnly()).thenReturn(false);
        when(mBatteryOptimizeUtils.isSystemOrDefaultApp()).thenReturn(true);
        when(mBatteryOptimizeUtils.getAppOptimizationMode())
                .thenReturn(BatteryOptimizeUtils.MODE_OPTIMIZED);

        mController.updateState(mMainSwitchPreference);

        assertThat(mMainSwitchPreference.isEnabled()).isFalse();
        assertThat(mMainSwitchPreference.isChecked()).isTrue();
    }

    @Test
    public void testUpdateState_isRestrictedStates_prefChecked() {
        when(mBatteryOptimizeUtils.isOptimizeModeMutable()).thenReturn(true);
        when(mBatteryOptimizeUtils.getAppOptimizationMode())
                .thenReturn(BatteryOptimizeUtils.MODE_RESTRICTED);

        mController.updateState(mMainSwitchPreference);

        assertThat(mMainSwitchPreference.isEnabled()).isTrue();
        assertThat(mMainSwitchPreference.isChecked()).isFalse();
    }

    @Test
    public void testUpdateState_prefUnchecked() {
        when(mBatteryOptimizeUtils.isOptimizeModeMutable()).thenReturn(true);
        when(mBatteryOptimizeUtils.getAppOptimizationMode())
                .thenReturn(BatteryOptimizeUtils.MODE_OPTIMIZED);

        mController.updateState(mMainSwitchPreference);

        assertThat(mMainSwitchPreference.isEnabled()).isTrue();
        assertThat(mMainSwitchPreference.isChecked()).isTrue();
    }

    @Test
    public void testHandlePreferenceTreeClick_samePrefKey_verifyAction() {
        mMainSwitchPreference.setKey(
                AllowBackgroundPreferenceController.KEY_ALLOW_BACKGROUND_USAGE);
        mController.handlePreferenceTreeClick(mMainSwitchPreference);

        assertThat(mController.handlePreferenceTreeClick(mMainSwitchPreference)).isTrue();
    }

    @Test
    public void testHandlePreferenceTreeClick_incorrectPrefKey_noAction() {
        assertThat(mController.handlePreferenceTreeClick(mMainSwitchPreference)).isFalse();
    }
}
