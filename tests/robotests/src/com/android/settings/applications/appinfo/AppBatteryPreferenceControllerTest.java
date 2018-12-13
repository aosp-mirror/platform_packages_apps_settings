/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.BatteryStats;
import android.os.Bundle;

import androidx.loader.app.LoaderManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.SettingsActivity;
import com.android.settings.fuelgauge.BatteryUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AppBatteryPreferenceControllerTest {

    private static final int TARGET_UID = 111;
    private static final int OTHER_UID = 222;
    private static final double BATTERY_LEVEL = 60;

    @Mock
    private SettingsActivity mActivity;
    @Mock
    private BatteryUtils mBatteryUtils;
    @Mock
    private BatterySipper mBatterySipper;
    @Mock
    private BatterySipper mOtherBatterySipper;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BatteryStatsHelper mBatteryStatsHelper;
    @Mock
    private BatteryStats.Uid mUid;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private LoaderManager mLoaderManager;

    private Context mContext;
    private AppInfoDashboardFragment mFragment;
    private AppBatteryPreferenceController mController;
    private Preference mBatteryPreference;

    @Before
    public void setUp() throws NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);

        mFragment = spy(new AppInfoDashboardFragment());

        mBatteryPreference = spy(new Preference(RuntimeEnvironment.application));

        mBatterySipper.drainType = BatterySipper.DrainType.IDLE;
        mBatterySipper.uidObj = mUid;
        doReturn(TARGET_UID).when(mBatterySipper).getUid();
        doReturn(OTHER_UID).when(mOtherBatterySipper).getUid();

        mController = spy(new AppBatteryPreferenceController(
            RuntimeEnvironment.application, mFragment, "package1", null /* lifecycle */));
        mController.mBatteryUtils = mBatteryUtils;
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mBatteryPreference);
    }

    @Test
    public void testAppBattery_byDefault_shouldBeShown() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void testAppBattery_ifDisabled_shouldNotBeShown() {
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void findTargetSipper_findCorrectSipper() {
        final List<BatterySipper> usageList = new ArrayList<>();
        usageList.add(mBatterySipper);
        usageList.add(mOtherBatterySipper);
        when(mBatteryStatsHelper.getUsageList()).thenReturn(usageList);

        assertThat(mController.findTargetSipper(mBatteryStatsHelper, TARGET_UID))
            .isEqualTo(mBatterySipper);
    }

    @Test
    public void updateBattery_noBatteryStats_summaryNo() {
        mController.displayPreference(mScreen);

        mController.updateBattery();

        assertThat(mBatteryPreference.getSummary())
            .isEqualTo("No battery use since last full charge");
    }

    @Test
    public void updateBattery_hasBatteryStats_summaryPercent() {
        mController.mBatteryHelper = mBatteryStatsHelper;
        mController.mSipper = mBatterySipper;
        doReturn(BATTERY_LEVEL).when(mBatteryUtils).calculateBatteryPercent(anyDouble(),
                anyDouble(), anyDouble(), anyInt());
        doReturn(new ArrayList<>()).when(mBatteryStatsHelper).getUsageList();
        mController.displayPreference(mScreen);

        mController.updateBattery();

        assertThat(mBatteryPreference.getSummary()).isEqualTo("60% use since last full charge");
    }

    @Test
    public void isBatteryStatsAvailable_hasBatteryStatsHelperAndSipper_returnTrue() {
        mController.mBatteryHelper = mBatteryStatsHelper;
        mController.mSipper = mBatterySipper;

        assertThat(mController.isBatteryStatsAvailable()).isTrue();
    }

    @Test
    public void isBatteryStatsAvailable_parametersNull_returnFalse() {
        assertThat(mController.isBatteryStatsAvailable()).isFalse();
    }

    @Test
    public void launchPowerUsageDetailFragment_shouldNotCrash() {
        when(mActivity.getSystemService(Context.APP_OPS_SERVICE))
                .thenReturn(mock(AppOpsManager.class));
        when(mFragment.getActivity()).thenReturn(mActivity);
        final String key = mController.getPreferenceKey();
        when(mBatteryPreference.getKey()).thenReturn(key);
        mController.mSipper = mBatterySipper;
        mController.mBatteryHelper = mBatteryStatsHelper;

        // Should not crash
        mController.handlePreferenceTreeClick(mBatteryPreference);
    }

    @Test
    public void onResume_shouldRestartBatteryStatsLoader() {
        doReturn(mLoaderManager).when(mFragment).getLoaderManager();

        mController.onResume();

        verify(mLoaderManager)
            .restartLoader(AppInfoDashboardFragment.LOADER_BATTERY, Bundle.EMPTY, mController);
    }

    @Test
    public void onPause_shouldDestroyBatteryStatsLoader() {
        doReturn(mLoaderManager).when(mFragment).getLoaderManager();

        mController.onPause();

        verify(mLoaderManager).destroyLoader(AppInfoDashboardFragment.LOADER_BATTERY);
    }
}
