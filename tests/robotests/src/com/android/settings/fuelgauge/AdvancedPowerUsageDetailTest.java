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

package com.android.settings.fuelgauge;

import static com.android.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.app.AppOpsManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.fuelgauge.BatteryOptimizeHistoricalLogEntry.Action;
import com.android.settings.fuelgauge.batteryusage.BatteryDiffEntry;
import com.android.settings.fuelgauge.batteryusage.BatteryEntry;
import com.android.settings.fuelgauge.batteryusage.ConvertUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowEntityHeaderController;
import com.android.settingslib.Utils;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.instantapps.InstantAppDataProvider;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.IntroPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;

import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            com.android.settings.testutils.shadow.ShadowFragment.class,
        })
public class AdvancedPowerUsageDetailTest {

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final String APP_LABEL = "app label";
    private static final String APP_ENTRY_LABEL = "app entry label";
    private static final String SUMMARY = "summary";
    private static final String PACKAGE_NAME = "com.android.app";
    private static final String INITIATING_PACKAGE_NAME = "com.android.vending";
    private static final String USAGE_PERCENT = "16%";
    private static final int ICON_ID = 123;
    private static final int UID = 1;
    private static final long FOREGROUND_TIME_MS = 444;
    private static final long FOREGROUND_SERVICE_TIME_MS = 123;
    private static final long BACKGROUND_TIME_MS = 100;
    private static final long SCREEN_ON_TIME_MS = 321;
    private static final Drawable TEST_DRAWABLE = new ColorDrawable(0);

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FragmentActivity mActivity;

    @Mock private ApplicationsState mState;
    @Mock private ApplicationsState.AppEntry mAppEntry;
    @Mock private BatteryEntry mBatteryEntry;
    @Mock private PackageManager mPackageManager;
    @Mock private InstallSourceInfo mInstallSourceInfo;
    @Mock private AppOpsManager mAppOpsManager;
    @Mock private LoaderManager mLoaderManager;

    private int mTestMode;
    private Context mContext;
    private AdvancedPowerUsageDetail mFragment;
    private SettingsActivity mTestActivity;
    private FakeFeatureFactory mFeatureFactory;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private BatteryDiffEntry mBatteryDiffEntry;
    private Bundle mBundle;
    private BatteryOptimizeUtils mBatteryOptimizeUtils;
    private IntroPreference mIntroPreference;

    @Implements(Utils.class)
    private static class ShadowUtils {
        @Implementation
        public static Drawable getBadgedIcon(Context context, ApplicationInfo appInfo) {
            return AdvancedPowerUsageDetailTest.TEST_DRAWABLE;
        }
    }

    @Before
    public void setUp() throws Exception {
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getPackageName()).thenReturn(PACKAGE_NAME);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.getInstallSourceInfo(anyString())).thenReturn(mInstallSourceInfo);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mMetricsFeatureProvider = mFeatureFactory.metricsFeatureProvider;

        prepareTestBatteryOptimizationUtils();
        mFragment = spy(new AdvancedPowerUsageDetail());
        mFragment.mBatteryOptimizeUtils = mBatteryOptimizeUtils;
        mIntroPreference = new IntroPreference(mContext);
        doReturn(mIntroPreference).when(mFragment).findPreference(any());
        mBundle = spy(new Bundle());
        doReturn(mContext).when(mFragment).getContext();
        doReturn(mActivity).when(mFragment).getActivity();
        doReturn(SUMMARY).when(mFragment).getString(anyInt());
        doReturn(APP_LABEL).when(mBundle).getString(nullable(String.class));
        when(mFragment.getArguments()).thenReturn(mBundle);
        doReturn(mLoaderManager).when(mFragment).getLoaderManager();

        when(mBatteryEntry.getUid()).thenReturn(UID);
        when(mBatteryEntry.getLabel()).thenReturn(APP_LABEL);
        when(mBatteryEntry.getTimeInForegroundMs()).thenReturn(FOREGROUND_TIME_MS);
        when(mBatteryEntry.getTimeInForegroundServiceMs()).thenReturn(FOREGROUND_SERVICE_TIME_MS);
        when(mBatteryEntry.getTimeInBackgroundMs()).thenReturn(BACKGROUND_TIME_MS);
        mBatteryEntry.mIconId = ICON_ID;

        mBatteryDiffEntry =
                spy(
                        new BatteryDiffEntry(
                                mContext,
                                /* uid= */ UID,
                                /* userId= */ 0,
                                /* key= */ "key",
                                /* isHidden= */ false,
                                /* componentId= */ -1,
                                /* legacyPackageName= */ null,
                                /* legacyLabel= */ null,
                                /*consumerType*/ ConvertUtils.CONSUMER_TYPE_USER_BATTERY,
                                /* foregroundUsageTimeInMs= */ FOREGROUND_TIME_MS,
                                /* foregroundSerUsageTimeInMs= */ FOREGROUND_SERVICE_TIME_MS,
                                /* backgroundUsageTimeInMs= */ BACKGROUND_TIME_MS,
                                /* screenOnTimeInMs= */ SCREEN_ON_TIME_MS,
                                /* consumePower= */ 0,
                                /* foregroundUsageConsumePower= */ 0,
                                /* foregroundServiceUsageConsumePower= */ 0,
                                /* backgroundUsageConsumePower= */ 0,
                                /* cachedUsageConsumePower= */ 0));
        when(mBatteryDiffEntry.getAppLabel()).thenReturn(APP_LABEL);
        when(mBatteryDiffEntry.getAppIconId()).thenReturn(ICON_ID);

        mFragment.mState = mState;
        mFragment.mBatteryOptimizeUtils = mBatteryOptimizeUtils;
        mFragment.mLogStringBuilder = new StringBuilder();
        doNothing().when(mState).ensureIcon(mAppEntry);
        mAppEntry.info = mock(ApplicationInfo.class);
        mAppEntry.label = APP_ENTRY_LABEL;

        mTestActivity = spy(new SettingsActivity());
        doReturn(mPackageManager).when(mTestActivity).getPackageManager();
        doReturn(mPackageManager).when(mActivity).getPackageManager();
        doReturn(mAppOpsManager).when(mTestActivity).getSystemService(Context.APP_OPS_SERVICE);

        final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);

        Answer<Void> callable =
                invocation -> {
                    mBundle = captor.getValue().getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
                    System.out.println("mBundle = " + mBundle);
                    return null;
                };
        doAnswer(callable)
                .when(mActivity)
                .startActivityAsUser(captor.capture(), nullable(UserHandle.class));
        doAnswer(callable).when(mActivity).startActivity(captor.capture());
        doAnswer(callable).when(mContext).startActivity(captor.capture());
    }

    @After
    public void reset() {
        ShadowEntityHeaderController.reset();
    }

    @Test
    public void setPreferenceScreenResId_returnNewLayout() {
        assertThat(mFragment.getPreferenceScreenResId()).isEqualTo(R.xml.power_usage_detail);
    }

    @Test
    @Config(shadows = ShadowUtils.class)
    public void initHeader_NoAppEntry_BuildByBundle() {
        mFragment.mAppEntry = null;
        mFragment.initHeader();

        assertThat(mIntroPreference.getIcon()).isNotEqualTo(TEST_DRAWABLE);
        assertThat(mIntroPreference.getTitle()).isEqualTo(APP_LABEL);
    }

    @Test
    @Config(shadows = ShadowUtils.class)
    public void initHeader_HasAppEntry_BuildByAppEntry() {
        mFragment.mAppEntry = mAppEntry;
        mFragment.initHeader();

        assertThat(mIntroPreference.getIcon()).isEqualTo(TEST_DRAWABLE);
        assertThat(mIntroPreference.getTitle()).isEqualTo(mAppEntry.label);
    }

    @Test
    @Config(shadows = ShadowUtils.class)
    public void initHeader_HasAppEntry_InstantApp() {
        ReflectionHelpers.setStaticField(
                AppUtils.class,
                "sInstantAppDataProvider",
                new InstantAppDataProvider() {
                    @Override
                    public boolean isInstantApp(ApplicationInfo info) {
                        return true;
                    }
                });
        mFragment.mAppEntry = mAppEntry;
        mFragment.initHeader();

        assertThat(mIntroPreference.getIcon()).isEqualTo(TEST_DRAWABLE);
        assertThat(mIntroPreference.getTitle()).isEqualTo(mAppEntry.label);
    }

    @Test
    public void startBatteryDetailPage_invalidToShowSummary_noFGBDData() {
        mBundle.clear();
        AdvancedPowerUsageDetail.startBatteryDetailPage(
                mActivity, mFragment, mBatteryEntry, USAGE_PERCENT);

        assertThat(mBundle.getInt(AdvancedPowerUsageDetail.EXTRA_UID)).isEqualTo(UID);
        assertThat(mBundle.getLong(AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME)).isEqualTo(0);
        assertThat(mBundle.getLong(AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME)).isEqualTo(0);
        assertThat(mBundle.getLong(AdvancedPowerUsageDetail.EXTRA_SCREEN_ON_TIME)).isEqualTo(0);
        assertThat(mBundle.getString(AdvancedPowerUsageDetail.EXTRA_POWER_USAGE_PERCENT))
                .isEqualTo(USAGE_PERCENT);
    }

    @Test
    public void startBatteryDetailPage_showSummary_hasFGBDData() {
        final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        mBundle.clear();
        AdvancedPowerUsageDetail.startBatteryDetailPage(
                mContext,
                mFragment.getMetricsCategory(),
                mBatteryDiffEntry,
                USAGE_PERCENT,
                /* slotInformation= */ null,
                /* showTimeInformation= */ true,
                /* anomalyHintPrefKey= */ null,
                /* anomalyHintText= */ null);

        verify(mContext).startActivity(captor.capture());
        assertThat(mBundle.getInt(AdvancedPowerUsageDetail.EXTRA_UID)).isEqualTo(UID);
        assertThat(mBundle.getLong(AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME))
                .isEqualTo(BACKGROUND_TIME_MS + FOREGROUND_SERVICE_TIME_MS);
        assertThat(mBundle.getLong(AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME))
                .isEqualTo(FOREGROUND_TIME_MS);
        assertThat(mBundle.getLong(AdvancedPowerUsageDetail.EXTRA_SCREEN_ON_TIME))
                .isEqualTo(SCREEN_ON_TIME_MS);
        assertThat(mBundle.getString(AdvancedPowerUsageDetail.EXTRA_POWER_USAGE_PERCENT))
                .isEqualTo(USAGE_PERCENT);
        assertThat(mBundle.getString(AdvancedPowerUsageDetail.EXTRA_SLOT_TIME)).isNull();
    }

    @Test
    public void startBatteryDetailPage_noBatteryUsage_hasBasicData() {
        final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);

        AdvancedPowerUsageDetail.startBatteryDetailPage(
                mActivity, mFragment, PACKAGE_NAME, UserHandle.OWNER);

        verify(mActivity).startActivity(captor.capture());

        assertThat(
                        captor.getValue()
                                .getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS)
                                .getString(AdvancedPowerUsageDetail.EXTRA_PACKAGE_NAME))
                .isEqualTo(PACKAGE_NAME);

        assertThat(
                        captor.getValue()
                                .getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS)
                                .getString(AdvancedPowerUsageDetail.EXTRA_POWER_USAGE_PERCENT))
                .isEqualTo("0%");
    }

    @Test
    public void startBatteryDetailPage_batteryEntryNotExisted_extractUidFromPackageName()
            throws PackageManager.NameNotFoundException {
        mBundle.clear();
        doReturn(UID).when(mPackageManager).getPackageUid(PACKAGE_NAME, 0 /* no flag */);

        AdvancedPowerUsageDetail.startBatteryDetailPage(
                mActivity, mFragment, PACKAGE_NAME, UserHandle.OWNER);

        assertThat(mBundle.getInt(AdvancedPowerUsageDetail.EXTRA_UID)).isEqualTo(UID);
    }

    @Test
    public void onPause_optimizationModeIsChanged_logPreference() throws Exception {
        mFragment.mOptimizationMode = BatteryOptimizeUtils.MODE_RESTRICTED;
        when(mBatteryOptimizeUtils.getPackageName()).thenReturn(PACKAGE_NAME);
        when(mInstallSourceInfo.getInitiatingPackageName()).thenReturn(INITIATING_PACKAGE_NAME);

        mBatteryOptimizeUtils.setAppUsageState(BatteryOptimizeUtils.MODE_OPTIMIZED, Action.APPLY);
        mFragment.onPause();

        TimeUnit.SECONDS.sleep(1);
        verify(mMetricsFeatureProvider)
                .action(
                        SettingsEnums.LEAVE_APP_BATTERY_USAGE,
                        SettingsEnums.ACTION_APP_BATTERY_USAGE_ALLOW_BACKGROUND,
                        SettingsEnums.FUELGAUGE_POWER_USAGE_DETAIL,
                        PACKAGE_NAME,
                        /* consumed battery */ 0);
    }

    @Test
    public void onPause_optimizationModeIsNotChanged_notInvokeLogging() throws Exception {
        mFragment.mOptimizationMode = BatteryOptimizeUtils.MODE_RESTRICTED;
        when(mBatteryOptimizeUtils.getPackageName()).thenReturn(PACKAGE_NAME);
        when(mInstallSourceInfo.getInitiatingPackageName()).thenReturn(INITIATING_PACKAGE_NAME);

        mBatteryOptimizeUtils.setAppUsageState(BatteryOptimizeUtils.MODE_OPTIMIZED, Action.APPLY);
        mBatteryOptimizeUtils.setAppUsageState(BatteryOptimizeUtils.MODE_RESTRICTED, Action.APPLY);
        mFragment.onPause();

        TimeUnit.SECONDS.sleep(1);
        verifyNoInteractions(mMetricsFeatureProvider);
    }

    @Test
    public void shouldSkipForInitialSUW_returnTrue() {
        assertThat(mFragment.shouldSkipForInitialSUW()).isTrue();
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
}
