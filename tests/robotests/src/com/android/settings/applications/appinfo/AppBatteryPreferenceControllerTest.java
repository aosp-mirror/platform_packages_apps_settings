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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.BatteryUsageStats;
import android.os.Bundle;
import android.os.UidBatteryConsumer;

import androidx.loader.app.LoaderManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.SettingsActivity;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batteryusage.BatteryDiffEntry;
import com.android.settingslib.applications.ApplicationsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class AppBatteryPreferenceControllerTest {

    private static final int TARGET_UID = 111;
    private static final int OTHER_UID = 222;
    private static final double BATTERY_LEVEL = 60;

    @Mock
    private SettingsActivity mActivity;
    @Mock
    private BatteryUtils mBatteryUtils;
    @Mock
    private BatteryUsageStats mBatteryUsageStats;
    @Mock
    private UidBatteryConsumer mUidBatteryConsumer;
    @Mock
    private UidBatteryConsumer mOtherUidBatteryConsumer;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private LoaderManager mLoaderManager;
    @Mock
    private BatteryDiffEntry mBatteryDiffEntry;

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

        when(mUidBatteryConsumer.getUid()).thenReturn(TARGET_UID);
        when(mOtherUidBatteryConsumer.getUid()).thenReturn(OTHER_UID);

        mController = spy(new AppBatteryPreferenceController(
                RuntimeEnvironment.application,
                mFragment,
                "package1" /* packageName */,
                0 /* uId */,
                null /* lifecycle */));
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
    public void findTargetBatteryConsumer_findCorrectBatteryConsumer() {
        final List<UidBatteryConsumer> uidBatteryConsumers = new ArrayList<>();
        uidBatteryConsumers.add(mUidBatteryConsumer);
        uidBatteryConsumers.add(mOtherUidBatteryConsumer);
        when(mBatteryUsageStats.getUidBatteryConsumers()).thenReturn(uidBatteryConsumers);

        assertThat(mController.findTargetUidBatteryConsumer(mBatteryUsageStats, TARGET_UID))
            .isEqualTo(mUidBatteryConsumer);
    }

    @Test
    public void updateBatteryWithDiffEntry_noConsumePower_summaryNo() {
        mController.displayPreference(mScreen);

        mController.updateBatteryWithDiffEntry();

        assertThat(mBatteryPreference.getSummary().toString()).isEqualTo(
                "No battery use since last full charge");
    }

    @Test
    public void updateBatteryWithDiffEntry_withConsumePower_summaryPercent() {
        mController.displayPreference(mScreen);
        mBatteryDiffEntry.mConsumePower = 1;
        mController.mBatteryDiffEntry = mBatteryDiffEntry;
        when(mBatteryDiffEntry.getPercentage()).thenReturn(60.0);

        mController.updateBatteryWithDiffEntry();

        assertThat(mBatteryPreference.getSummary().toString()).isEqualTo(
                "60% use since last full charge");
    }

    @Test
    public void displayPreference_noEntry_preferenceShouldSetEmptySummary() {
        mController.mParent.setAppEntry(null);

        mController.displayPreference(mScreen);

        assertThat(mBatteryPreference.getSummary()).isEqualTo("");
    }

    @Test
    public void displayPreference_appIsNotInstalled_preferenceShouldSetEmptySummary() {
        final ApplicationsState.AppEntry appEntry = mock(ApplicationsState.AppEntry.class);
        appEntry.info = new ApplicationInfo();
        mController.mParent.setAppEntry(appEntry);

        mController.displayPreference(mScreen);

        assertThat(mBatteryPreference.getSummary()).isEqualTo("");
    }

    @Test
    public void isBatteryStatsAvailable_hasBatteryStatsHelperAndSipper_returnTrue() {
        mController.mBatteryUsageStats = mBatteryUsageStats;
        mController.mUidBatteryConsumer = mUidBatteryConsumer;

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
        mController.mBatteryUsageStats = mBatteryUsageStats;
        mController.mUidBatteryConsumer = mUidBatteryConsumer;

        // Should not crash
        mController.handlePreferenceTreeClick(mBatteryPreference);
    }

    @Test
    public void onResume_shouldRestartBatteryStatsLoader() {
        doReturn(mLoaderManager).when(mFragment).getLoaderManager();

        mController.onResume();

        verify(mLoaderManager)
                .restartLoader(AppInfoDashboardFragment.LOADER_BATTERY_USAGE_STATS, Bundle.EMPTY,
                        mController.mBatteryUsageStatsLoaderCallbacks);
    }

    @Test
    public void onPause_shouldDestroyBatteryStatsLoader() {
        doReturn(mLoaderManager).when(mFragment).getLoaderManager();

        mController.onPause();

        verify(mLoaderManager).destroyLoader(AppInfoDashboardFragment.LOADER_BATTERY_USAGE_STATS);
    }
}
