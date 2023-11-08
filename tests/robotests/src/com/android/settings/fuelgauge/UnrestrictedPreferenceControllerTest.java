/*
 * Copyright (C) 2021 The Android Open Source Project
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

import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class UnrestrictedPreferenceControllerTest {
    private static final int UID = 12345;
    private static final String PACKAGE_NAME = "com.android.app";

    private UnrestrictedPreferenceController mController;
    private SelectorWithWidgetPreference mPreference;
    private BatteryOptimizeUtils mBatteryOptimizeUtils;

    @Mock PackageManager mMockPackageManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        Context context = spy(RuntimeEnvironment.application);
        BatteryUtils.getInstance(context).reset();
        doReturn(UID)
                .when(mMockPackageManager)
                .getPackageUid(PACKAGE_NAME, PackageManager.GET_META_DATA);

        mController = new UnrestrictedPreferenceController(context, UID, PACKAGE_NAME);
        mPreference = new SelectorWithWidgetPreference(RuntimeEnvironment.application);
        mBatteryOptimizeUtils = spy(new BatteryOptimizeUtils(context, UID, PACKAGE_NAME));
        mController.mBatteryOptimizeUtils = mBatteryOptimizeUtils;
    }

    @Test
    public void testUpdateState_isValidPackage_prefEnabled() {
        when(mBatteryOptimizeUtils.isDisabledForOptimizeModeOnly()).thenReturn(false);
        when(mBatteryOptimizeUtils.isSystemOrDefaultApp()).thenReturn(false);

        mController.updateState(mPreference);

        assertThat(mBatteryOptimizeUtils.isOptimizeModeMutable()).isTrue();
        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void testUpdateState_invalidPackage_prefDisabled() {
        when(mBatteryOptimizeUtils.isDisabledForOptimizeModeOnly()).thenReturn(true);
        when(mBatteryOptimizeUtils.isSystemOrDefaultApp()).thenReturn(false);

        mController.updateState(mPreference);

        assertThat(mBatteryOptimizeUtils.isOptimizeModeMutable()).isFalse();
        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void testUpdateState_isSystemOrDefaultAppAndUnrestrictedStates_prefChecked() {
        when(mBatteryOptimizeUtils.isDisabledForOptimizeModeOnly()).thenReturn(false);
        when(mBatteryOptimizeUtils.isSystemOrDefaultApp()).thenReturn(true);
        when(mBatteryOptimizeUtils.getAppOptimizationMode())
                .thenReturn(BatteryOptimizeUtils.MODE_UNRESTRICTED);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void testUpdateState_isSystemOrDefaultApp_prefUnchecked() {
        when(mBatteryOptimizeUtils.isDisabledForOptimizeModeOnly()).thenReturn(false);
        when(mBatteryOptimizeUtils.isSystemOrDefaultApp()).thenReturn(true);
        when(mBatteryOptimizeUtils.getAppOptimizationMode())
                .thenReturn(BatteryOptimizeUtils.MODE_OPTIMIZED);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void testUpdateState_isUnrestrictedStates_prefChecked() {
        when(mBatteryOptimizeUtils.isOptimizeModeMutable()).thenReturn(true);
        when(mBatteryOptimizeUtils.getAppOptimizationMode())
                .thenReturn(BatteryOptimizeUtils.MODE_UNRESTRICTED);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void testUpdateState_prefUnchecked() {
        when(mBatteryOptimizeUtils.isOptimizeModeMutable()).thenReturn(true);
        when(mBatteryOptimizeUtils.getAppOptimizationMode())
                .thenReturn(BatteryOptimizeUtils.MODE_OPTIMIZED);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void testHandlePreferenceTreeClick_samePrefKey_verifyAction() {
        mPreference.setKey(mController.KEY_UNRESTRICTED_PREF);
        mController.handlePreferenceTreeClick(mPreference);

        assertThat(mController.handlePreferenceTreeClick(mPreference)).isTrue();
    }

    @Test
    public void testHandlePreferenceTreeClick_incorrectPrefKey_noAction() {
        assertThat(mController.handlePreferenceTreeClick(mPreference)).isFalse();
    }
}
