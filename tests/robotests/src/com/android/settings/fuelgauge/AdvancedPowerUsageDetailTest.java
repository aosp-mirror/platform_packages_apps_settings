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
import static com.android.settings.fuelgauge.BatteryOptimizeHistoricalLogEntry.Action;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.fuelgauge.batteryusage.BatteryDiffEntry;
import com.android.settings.fuelgauge.batteryusage.BatteryEntry;
import com.android.settings.fuelgauge.batteryusage.ConvertUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowEntityHeaderController;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.instantapps.InstantAppDataProvider;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.datastore.ChangeReason;
import com.android.settingslib.datastore.Observer;
import com.android.settingslib.widget.LayoutPreference;

import com.google.common.util.concurrent.MoreExecutors;

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
import org.robolectric.util.ReflectionHelpers;

import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowEntityHeaderController.class,
            com.android.settings.testutils.shadow.ShadowFragment.class,
        })
public class AdvancedPowerUsageDetailTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final String APP_LABEL = "app label";
    private static final String SUMMARY = "summary";
    private static final String[] PACKAGE_NAME = {"com.android.app"};
    private static final String USAGE_PERCENT = "16%";
    private static final int ICON_ID = 123;
    private static final int UID = 1;
    private static final long FOREGROUND_TIME_MS = 444;
    private static final long FOREGROUND_SERVICE_TIME_MS = 123;
    private static final long BACKGROUND_TIME_MS = 100;
    private static final long SCREEN_ON_TIME_MS = 321;
    private static final String KEY_ALLOW_BACKGROUND_USAGE = "allow_background_usage";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FragmentActivity mActivity;

    @Mock private EntityHeaderController mEntityHeaderController;
    @Mock private LayoutPreference mHeaderPreference;
    @Mock private ApplicationsState mState;
    @Mock private ApplicationsState.AppEntry mAppEntry;
    @Mock private BatteryEntry mBatteryEntry;
    @Mock private PackageManager mPackageManager;
    @Mock private InstallSourceInfo mInstallSourceInfo;
    @Mock private AppOpsManager mAppOpsManager;
    @Mock private LoaderManager mLoaderManager;
    @Mock private BatteryOptimizeUtils mBatteryOptimizeUtils;
    @Mock private Observer mObserver;

    private Context mContext;
    private BatterySettingsStorage mBatterySettingsStorage;
    private PrimarySwitchPreference mAllowBackgroundUsagePreference;
    private AdvancedPowerUsageDetail mFragment;
    private SettingsActivity mTestActivity;
    private FakeFeatureFactory mFeatureFactory;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private BatteryDiffEntry mBatteryDiffEntry;
    private Bundle mBundle;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mBatterySettingsStorage = BatterySettingsStorage.get(mContext);
        when(mContext.getPackageName()).thenReturn("foo");
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mMetricsFeatureProvider = mFeatureFactory.metricsFeatureProvider;

        mFragment = spy(new AdvancedPowerUsageDetail());
        mBundle = spy(new Bundle());
        doReturn(mContext).when(mFragment).getContext();
        doReturn(mActivity).when(mFragment).getActivity();
        doReturn(SUMMARY).when(mFragment).getString(anyInt());
        doReturn(APP_LABEL).when(mBundle).getString(nullable(String.class));
        when(mFragment.getArguments()).thenReturn(mBundle);
        doReturn(mLoaderManager).when(mFragment).getLoaderManager();

        ShadowEntityHeaderController.setUseMock(mEntityHeaderController);
        doReturn(mEntityHeaderController)
                .when(mEntityHeaderController)
                .setButtonActions(anyInt(), anyInt());
        doReturn(mEntityHeaderController)
                .when(mEntityHeaderController)
                .setIcon(nullable(Drawable.class));
        doReturn(mEntityHeaderController)
                .when(mEntityHeaderController)
                .setIcon(nullable(ApplicationsState.AppEntry.class));
        doReturn(mEntityHeaderController)
                .when(mEntityHeaderController)
                .setLabel(nullable(String.class));
        doReturn(mEntityHeaderController)
                .when(mEntityHeaderController)
                .setLabel(nullable(String.class));
        doReturn(mEntityHeaderController)
                .when(mEntityHeaderController)
                .setLabel(nullable(ApplicationsState.AppEntry.class));
        doReturn(mEntityHeaderController)
                .when(mEntityHeaderController)
                .setSummary(nullable(String.class));

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

        mFragment.mHeaderPreference = mHeaderPreference;
        mFragment.mState = mState;
        mFragment.mBatteryOptimizeUtils = mBatteryOptimizeUtils;
        mFragment.mLogStringBuilder = new StringBuilder();
        mAppEntry.info = mock(ApplicationInfo.class);

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

        mAllowBackgroundUsagePreference = new PrimarySwitchPreference(mContext);
        mAllowBackgroundUsagePreference.setKey(KEY_ALLOW_BACKGROUND_USAGE);
        mFragment.mAllowBackgroundUsagePreference = mAllowBackgroundUsagePreference;
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
    public void initHeader_NoAppEntry_BuildByBundle() {
        mFragment.mAppEntry = null;
        mFragment.initHeader();

        verify(mEntityHeaderController).setIcon(nullable(Drawable.class));
        verify(mEntityHeaderController).setLabel(APP_LABEL);
    }

    @Test
    public void initHeader_HasAppEntry_BuildByAppEntry() {
        ReflectionHelpers.setStaticField(
                AppUtils.class,
                "sInstantAppDataProvider",
                new InstantAppDataProvider() {
                    @Override
                    public boolean isInstantApp(ApplicationInfo info) {
                        return false;
                    }
                });
        mFragment.mAppEntry = mAppEntry;
        mFragment.initHeader();

        verify(mEntityHeaderController).setIcon(mAppEntry);
        verify(mEntityHeaderController).setLabel(mAppEntry);
        verify(mEntityHeaderController).setIsInstantApp(false);
    }

    @Test
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

        verify(mEntityHeaderController).setIcon(mAppEntry);
        verify(mEntityHeaderController).setLabel(mAppEntry);
        verify(mEntityHeaderController).setIsInstantApp(true);
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
        assertThat(mBundle.getString(AdvancedPowerUsageDetail.EXTRA_SLOT_TIME))
                .isEqualTo(null);
    }


    @Test
    public void startBatteryDetailPage_noBatteryUsage_hasBasicData() {
        final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);

        AdvancedPowerUsageDetail.startBatteryDetailPage(
                mActivity, mFragment, PACKAGE_NAME[0], UserHandle.OWNER);

        verify(mActivity).startActivity(captor.capture());

        assertThat(
                        captor.getValue()
                                .getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS)
                                .getString(AdvancedPowerUsageDetail.EXTRA_PACKAGE_NAME))
                .isEqualTo(PACKAGE_NAME[0]);

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
        doReturn(UID).when(mPackageManager).getPackageUid(PACKAGE_NAME[0], 0 /* no flag */);

        AdvancedPowerUsageDetail.startBatteryDetailPage(
                mActivity, mFragment, PACKAGE_NAME[0], UserHandle.OWNER);

        assertThat(mBundle.getInt(AdvancedPowerUsageDetail.EXTRA_UID)).isEqualTo(UID);
    }

    @Test
    public void initFooter_isValidPackageName_hasCorrectString() {
        when(mBatteryOptimizeUtils.isDisabledForOptimizeModeOnly()).thenReturn(true);

        mFragment.initFooter();

        assertThat(mAllowBackgroundUsagePreference.getSummary().toString())
                .isEqualTo("This app requires optimized battery usage.");
    }

    @Test
    public void initFooter_isSystemOrDefaultApp_hasCorrectString() {
        when(mBatteryOptimizeUtils.isDisabledForOptimizeModeOnly()).thenReturn(false);
        when(mBatteryOptimizeUtils.isSystemOrDefaultApp()).thenReturn(true);

        mFragment.initFooter();

        assertThat(mAllowBackgroundUsagePreference.getSummary().toString())
                .isEqualTo("This app requires unrestricted battery usage.");
    }

    @Test
    public void initFooter_hasCorrectString() {
        when(mBatteryOptimizeUtils.isDisabledForOptimizeModeOnly()).thenReturn(false);
        when(mBatteryOptimizeUtils.isSystemOrDefaultApp()).thenReturn(false);

        mFragment.initFooter();

        assertThat(mAllowBackgroundUsagePreference.getSummary().toString())
                .isEqualTo("Enable for real-time updates, disable to save battery");
    }

    @Test
    public void onPause_optimizationModeChanged_logPreference()
            throws PackageManager.NameNotFoundException, InterruptedException {
        final String packageName = "testPackageName";
        final int restrictedMode = BatteryOptimizeUtils.MODE_RESTRICTED;
        final int optimizedMode = BatteryOptimizeUtils.MODE_OPTIMIZED;
        mFragment.mOptimizationMode = restrictedMode;
        when(mBatteryOptimizeUtils.getAppOptimizationMode()).thenReturn(restrictedMode);
        when(mBatteryOptimizeUtils.getPackageName()).thenReturn(packageName);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.getInstallSourceInfo(anyString())).thenReturn(mInstallSourceInfo);
        when(mInstallSourceInfo.getInitiatingPackageName()).thenReturn("com.android.vending");

        mFragment.onPreferenceChange(mAllowBackgroundUsagePreference, true);
        verify(mBatteryOptimizeUtils).setAppUsageState(optimizedMode, Action.APPLY);
        when(mBatteryOptimizeUtils.getAppOptimizationMode()).thenReturn(optimizedMode);
        mFragment.onPause();

        TimeUnit.SECONDS.sleep(1);
        verify(mMetricsFeatureProvider)
                .action(
                        SettingsEnums.LEAVE_APP_BATTERY_USAGE,
                        SettingsEnums.ACTION_APP_BATTERY_USAGE_ALLOW_BACKGROUND,
                        SettingsEnums.FUELGAUGE_POWER_USAGE_DETAIL,
                        packageName,
                        /* consumed battery */ 0);
    }

    @Test
    public void onPause_optimizationModeIsNotChanged_notInvokeLogging()
            throws PackageManager.NameNotFoundException, InterruptedException {
        final int restrictedMode = BatteryOptimizeUtils.MODE_RESTRICTED;
        final int optimizedMode = BatteryOptimizeUtils.MODE_OPTIMIZED;
        mFragment.mOptimizationMode = restrictedMode;
        when(mBatteryOptimizeUtils.getAppOptimizationMode()).thenReturn(restrictedMode);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.getInstallSourceInfo(anyString())).thenReturn(mInstallSourceInfo);
        when(mInstallSourceInfo.getInitiatingPackageName()).thenReturn("com.android.vending");

        mFragment.onPreferenceChange(mAllowBackgroundUsagePreference, true);
        verify(mBatteryOptimizeUtils).setAppUsageState(optimizedMode, Action.APPLY);
        when(mBatteryOptimizeUtils.getAppOptimizationMode()).thenReturn(optimizedMode);
        mFragment.onPreferenceChange(mAllowBackgroundUsagePreference, false);
        verify(mBatteryOptimizeUtils).setAppUsageState(restrictedMode, Action.APPLY);
        when(mBatteryOptimizeUtils.getAppOptimizationMode()).thenReturn(restrictedMode);
        mFragment.onPause();

        TimeUnit.SECONDS.sleep(1);
        verifyNoInteractions(mMetricsFeatureProvider);
    }

    @Test
    public void notifyBackupManager_optimizationModeIsNotChanged_notInvokeDataChanged() {
        mBatterySettingsStorage.addObserver(mObserver, MoreExecutors.directExecutor());
        final int mode = BatteryOptimizeUtils.MODE_RESTRICTED;
        mFragment.mOptimizationMode = mode;
        when(mBatteryOptimizeUtils.getAppOptimizationMode()).thenReturn(mode);

        mFragment.notifyBackupManager();

        verifyNoInteractions(mObserver);
    }

    @Test
    public void notifyBackupManager_optimizationModeIsChanged_invokeDataChanged() {
        mBatterySettingsStorage.addObserver(mObserver, MoreExecutors.directExecutor());
        mFragment.mOptimizationMode = BatteryOptimizeUtils.MODE_RESTRICTED;
        when(mBatteryOptimizeUtils.getAppOptimizationMode())
                .thenReturn(BatteryOptimizeUtils.MODE_UNRESTRICTED);

        mFragment.notifyBackupManager();

        verify(mObserver).onChanged(ChangeReason.UPDATE);
    }
}
