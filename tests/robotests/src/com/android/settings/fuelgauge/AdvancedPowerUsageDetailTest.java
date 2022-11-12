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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.app.AppOpsManager;
import android.app.backup.BackupManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.BatteryStats;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;

import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.preference.Preference;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.fuelgauge.batteryusage.BatteryEntry;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowActivityManager;
import com.android.settings.testutils.shadow.ShadowEntityHeaderController;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.instantapps.InstantAppDataProvider;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.LayoutPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowEntityHeaderController.class, ShadowActivityManager.class})
public class AdvancedPowerUsageDetailTest {
    private static final String APP_LABEL = "app label";
    private static final String SUMMARY = "summary";
    private static final String[] PACKAGE_NAME = {"com.android.app"};
    private static final String USAGE_PERCENT = "16%";
    private static final String SLOT_TIME = "12 am-2 am";
    private static final int ICON_ID = 123;
    private static final int UID = 1;
    private static final int POWER_MAH = 150;
    private static final long BACKGROUND_TIME_MS = 100;
    private static final long FOREGROUND_ACTIVITY_TIME_MS = 123;
    private static final long FOREGROUND_SERVICE_TIME_MS = 444;
    private static final long FOREGROUND_TIME_MS =
            FOREGROUND_ACTIVITY_TIME_MS + FOREGROUND_SERVICE_TIME_MS;
    private static final long FOREGROUND_SERVICE_TIME_US = FOREGROUND_SERVICE_TIME_MS * 1000;
    private static final String KEY_PREF_UNRESTRICTED = "unrestricted_pref";
    private static final String KEY_PREF_OPTIMIZED = "optimized_pref";
    private static final String KEY_PREF_RESTRICTED = "restricted_pref";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FragmentActivity mActivity;
    @Mock
    private EntityHeaderController mEntityHeaderController;
    @Mock
    private LayoutPreference mHeaderPreference;
    @Mock
    private ApplicationsState mState;
    @Mock
    private ApplicationsState.AppEntry mAppEntry;
    @Mock
    private Bundle mBundle;
    @Mock
    private BatteryEntry mBatteryEntry;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private AppOpsManager mAppOpsManager;
    @Mock
    private LoaderManager mLoaderManager;
    @Mock
    private BatteryUtils mBatteryUtils;
    @Mock
    private BatteryOptimizeUtils mBatteryOptimizeUtils;
    @Mock
    private BackupManager mBackupManager;

    private Context mContext;
    private Preference mForegroundPreference;
    private Preference mBackgroundPreference;
    private FooterPreference mFooterPreference;
    private SelectorWithWidgetPreference mRestrictedPreference;
    private SelectorWithWidgetPreference mOptimizePreference;
    private SelectorWithWidgetPreference mUnrestrictedPreference;
    private AdvancedPowerUsageDetail mFragment;
    private SettingsActivity mTestActivity;
    private FakeFeatureFactory mFeatureFactory;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageName()).thenReturn("foo");
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mMetricsFeatureProvider = mFeatureFactory.metricsFeatureProvider;

        mFragment = spy(new AdvancedPowerUsageDetail());
        doReturn(mContext).when(mFragment).getContext();
        doReturn(mActivity).when(mFragment).getActivity();
        doReturn(SUMMARY).when(mFragment).getString(anyInt());
        doReturn(APP_LABEL).when(mBundle).getString(nullable(String.class));
        when(mFragment.getArguments()).thenReturn(mBundle);
        doReturn(mLoaderManager).when(mFragment).getLoaderManager();
        when(mFeatureFactory.powerUsageFeatureProvider.isChartGraphEnabled(mContext))
                .thenReturn(true);

        ShadowEntityHeaderController.setUseMock(mEntityHeaderController);
        doReturn(mEntityHeaderController).when(mEntityHeaderController)
                .setRecyclerView(nullable(RecyclerView.class), nullable(Lifecycle.class));
        doReturn(mEntityHeaderController).when(mEntityHeaderController)
                .setButtonActions(anyInt(), anyInt());
        doReturn(mEntityHeaderController).when(mEntityHeaderController)
                .setIcon(nullable(Drawable.class));
        doReturn(mEntityHeaderController).when(mEntityHeaderController).setIcon(nullable(
                ApplicationsState.AppEntry.class));
        doReturn(mEntityHeaderController).when(mEntityHeaderController)
                .setLabel(nullable(String.class));
        doReturn(mEntityHeaderController).when(mEntityHeaderController)
                .setLabel(nullable(String.class));
        doReturn(mEntityHeaderController).when(mEntityHeaderController)
                .setLabel(nullable(ApplicationsState.AppEntry.class));
        doReturn(mEntityHeaderController).when(mEntityHeaderController)
                .setSummary(nullable(String.class));

        when(mBatteryEntry.getUid()).thenReturn(UID);
        when(mBatteryEntry.getLabel()).thenReturn(APP_LABEL);
        when(mBatteryEntry.getTimeInBackgroundMs()).thenReturn(BACKGROUND_TIME_MS);
        when(mBatteryEntry.getTimeInForegroundMs()).thenReturn(FOREGROUND_TIME_MS);
        mBatteryEntry.mIconId = ICON_ID;

        mFragment.mHeaderPreference = mHeaderPreference;
        mFragment.mState = mState;
        mFragment.mEnableTriState = true;
        mFragment.mBatteryUtils = new BatteryUtils(RuntimeEnvironment.application);
        mFragment.mBatteryOptimizeUtils = mBatteryOptimizeUtils;
        mFragment.mBackupManager = mBackupManager;
        mAppEntry.info = mock(ApplicationInfo.class);

        mTestActivity = spy(new SettingsActivity());
        doReturn(mPackageManager).when(mTestActivity).getPackageManager();
        doReturn(mPackageManager).when(mActivity).getPackageManager();
        doReturn(mAppOpsManager).when(mTestActivity).getSystemService(Context.APP_OPS_SERVICE);

        mBatteryUtils = spy(new BatteryUtils(mContext));
        doReturn(FOREGROUND_SERVICE_TIME_US).when(mBatteryUtils).getForegroundServiceTotalTimeUs(
                any(BatteryStats.Uid.class), anyLong());

        final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);

        Answer<Void> callable = invocation -> {
            mBundle = captor.getValue().getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
            System.out.println("mBundle = " + mBundle);
            return null;
        };
        doAnswer(callable).when(mActivity).startActivityAsUser(captor.capture(),
                nullable(UserHandle.class));
        doAnswer(callable).when(mActivity).startActivity(captor.capture());

        mForegroundPreference = new Preference(mContext);
        mBackgroundPreference = new Preference(mContext);
        mFooterPreference = new FooterPreference(mContext);
        mRestrictedPreference = new SelectorWithWidgetPreference(mContext);
        mOptimizePreference = new SelectorWithWidgetPreference(mContext);
        mUnrestrictedPreference = new SelectorWithWidgetPreference(mContext);
        mFragment.mForegroundPreference = mForegroundPreference;
        mFragment.mBackgroundPreference = mBackgroundPreference;
        mFragment.mFooterPreference = mFooterPreference;
        mFragment.mRestrictedPreference = mRestrictedPreference;
        mFragment.mOptimizePreference = mOptimizePreference;
        mFragment.mUnrestrictedPreference = mUnrestrictedPreference;
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
    public void testGetPreferenceScreenResId_disableTriState_returnLegacyLayout() {
        mFragment.mEnableTriState = false;
        assertThat(mFragment.getPreferenceScreenResId()).isEqualTo(R.xml.power_usage_detail_legacy);
    }

    @Test
    public void testInitHeader_NoAppEntry_BuildByBundle() {
        mFragment.mAppEntry = null;
        mFragment.initHeader();

        verify(mEntityHeaderController).setIcon(nullable(Drawable.class));
        verify(mEntityHeaderController).setLabel(APP_LABEL);
    }

    @Test
    public void initHeader_HasAppEntry_BuildByAppEntry() {
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
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
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
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
    public void initHeader_noUsageTimeAndGraphDisabled_hasCorrectSummary() {
        when(mFeatureFactory.powerUsageFeatureProvider.isChartGraphEnabled(mContext))
                .thenReturn(false);

        Bundle bundle = new Bundle(2);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME, /* value */ 0);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME, /* value */ 0);
        when(mFragment.getArguments()).thenReturn(bundle);

        mFragment.initHeader();

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mEntityHeaderController).setSummary(captor.capture());
        assertThat(captor.getValue().toString())
                .isEqualTo("No usage from last full charge");
    }

    @Test
    public void initHeader_bgTwoMinFgZeroAndGraphDisabled_hasCorrectSummary() {
        when(mFeatureFactory.powerUsageFeatureProvider.isChartGraphEnabled(mContext))
                .thenReturn(false);

        final long backgroundTimeTwoMinutes = 120000;
        final long foregroundTimeZero = 0;
        Bundle bundle = new Bundle(2);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME, backgroundTimeTwoMinutes);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME, foregroundTimeZero);
        when(mFragment.getArguments()).thenReturn(bundle);

        mFragment.initHeader();

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mEntityHeaderController).setSummary(captor.capture());
        assertThat(captor.getValue().toString())
                .isEqualTo("2 min background from last full charge");
    }

    @Test
    public void initHeader_bgLessThanAMinFgZeroAndGraphDisabled_hasCorrectSummary() {
        when(mFeatureFactory.powerUsageFeatureProvider.isChartGraphEnabled(mContext))
                .thenReturn(false);

        final long backgroundTimeLessThanAMinute = 59999;
        final long foregroundTimeZero = 0;
        Bundle bundle = new Bundle(2);
        bundle.putLong(
                AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME, backgroundTimeLessThanAMinute);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME, foregroundTimeZero);
        when(mFragment.getArguments()).thenReturn(bundle);

        mFragment.initHeader();

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mEntityHeaderController).setSummary(captor.capture());
        assertThat(captor.getValue().toString())
                .isEqualTo("Background less than a minute from last full charge");
    }

    @Test
    public void initHeader_totalUsageLessThanAMinAndGraphDisabled_hasCorrectSummary() {
        when(mFeatureFactory.powerUsageFeatureProvider.isChartGraphEnabled(mContext))
                .thenReturn(false);

        final long backgroundTimeLessThanHalfMinute = 20000;
        final long foregroundTimeLessThanHalfMinute = 20000;
        Bundle bundle = new Bundle(2);
        bundle.putLong(
                AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME, backgroundTimeLessThanHalfMinute);
        bundle.putLong(
                AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME, foregroundTimeLessThanHalfMinute);
        when(mFragment.getArguments()).thenReturn(bundle);

        mFragment.initHeader();

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mEntityHeaderController).setSummary(captor.capture());
        assertThat(captor.getValue().toString())
                .isEqualTo("Total less than a minute from last full charge");
    }

    @Test
    public void initHeader_TotalAMinutesBgLessThanAMinAndGraphDisabled_hasCorrectSummary() {
        when(mFeatureFactory.powerUsageFeatureProvider.isChartGraphEnabled(mContext))
                .thenReturn(false);

        final long backgroundTimeZero = 59999;
        final long foregroundTimeTwoMinutes = 1;
        Bundle bundle = new Bundle(2);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME, backgroundTimeZero);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME, foregroundTimeTwoMinutes);
        when(mFragment.getArguments()).thenReturn(bundle);

        mFragment.initHeader();

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mEntityHeaderController).setSummary(captor.capture());
        assertThat(captor.getValue().toString())
                .isEqualTo("1 min total • background less than a minute\nfrom last full charge");
    }

    @Test
    public void initHeader_TotalAMinBackgroundZeroAndGraphDisabled_hasCorrectSummary() {
        when(mFeatureFactory.powerUsageFeatureProvider.isChartGraphEnabled(mContext))
                .thenReturn(false);
        final long backgroundTimeZero = 0;
        final long foregroundTimeAMinutes = 60000;
        Bundle bundle = new Bundle(2);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME, backgroundTimeZero);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME, foregroundTimeAMinutes);
        when(mFragment.getArguments()).thenReturn(bundle);

        mFragment.initHeader();

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mEntityHeaderController).setSummary(captor.capture());
        assertThat(captor.getValue().toString())
                .isEqualTo("1 min total from last full charge");
    }

    @Test
    public void initHeader_fgTwoMinBgFourMinAndGraphDisabled_hasCorrectSummary() {
        when(mFeatureFactory.powerUsageFeatureProvider.isChartGraphEnabled(mContext))
                .thenReturn(false);
        final long backgroundTimeFourMinute = 240000;
        final long foregroundTimeTwoMinutes = 120000;
        Bundle bundle = new Bundle(2);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME, backgroundTimeFourMinute);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME, foregroundTimeTwoMinutes);
        when(mFragment.getArguments()).thenReturn(bundle);
        mFragment.initHeader();

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mEntityHeaderController).setSummary(captor.capture());
        assertThat(captor.getValue().toString())
                .isEqualTo("6 min total • 4 min background\nfrom last full charge");
    }

    @Test
    public void initHeader_noUsageTime_hasCorrectSummary() {
        Bundle bundle = new Bundle(2);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME, /* value */ 0);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME, /* value */ 0);
        when(mFragment.getArguments()).thenReturn(bundle);

        mFragment.initHeader();

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mEntityHeaderController).setSummary(captor.capture());
        assertThat(captor.getValue().toString()).isEmpty();
    }

    @Test
    public void initHeader_noUsageTimeButConsumedPower_hasEmptySummary() {
        Bundle bundle = new Bundle(3);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME, /* value */ 0);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME, /* value */ 0);
        bundle.putInt(AdvancedPowerUsageDetail.EXTRA_POWER_USAGE_AMOUNT, /* value */ 10);
        when(mFragment.getArguments()).thenReturn(bundle);

        mFragment.initHeader();

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mEntityHeaderController).setSummary(captor.capture());
        assertThat(captor.getValue().toString()).isEmpty();
    }

    @Test
    public void initHeader_backgroundTwoMinForegroundZero_hasCorrectSummary() {
        final long backgroundTimeTwoMinutes = 120000;
        final long foregroundTimeZero = 0;
        Bundle bundle = new Bundle(2);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME, backgroundTimeTwoMinutes);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME, foregroundTimeZero);
        when(mFragment.getArguments()).thenReturn(bundle);

        mFragment.initHeader();

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mEntityHeaderController).setSummary(captor.capture());
        assertThat(captor.getValue().toString())
                .isEqualTo("2 min background since last full charge");
    }

    @Test
    public void initHeader_backgroundLessThanAMinForegroundZero_hasCorrectSummary() {
        final long backgroundTimeLessThanAMinute = 59999;
        final long foregroundTimeZero = 0;
        Bundle bundle = new Bundle(2);
        bundle.putLong(
                AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME, backgroundTimeLessThanAMinute);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME, foregroundTimeZero);
        when(mFragment.getArguments()).thenReturn(bundle);

        mFragment.initHeader();

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mEntityHeaderController).setSummary(captor.capture());
        assertThat(captor.getValue().toString())
                .isEqualTo("Background less than a minute since last full charge");
    }

    @Test
    public void initHeader_totalUsageLessThanAMin_hasCorrectSummary() {
        final long backgroundTimeLessThanHalfMinute = 20000;
        final long foregroundTimeLessThanHalfMinute = 20000;
        Bundle bundle = new Bundle(2);
        bundle.putLong(
                AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME, backgroundTimeLessThanHalfMinute);
        bundle.putLong(
                AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME, foregroundTimeLessThanHalfMinute);
        when(mFragment.getArguments()).thenReturn(bundle);

        mFragment.initHeader();

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mEntityHeaderController).setSummary(captor.capture());
        assertThat(captor.getValue().toString())
                .isEqualTo("Total less than a minute since last full charge");
    }

    @Test
    public void initHeader_TotalAMinutesBackgroundLessThanAMin_hasCorrectSummary() {
        final long backgroundTimeZero = 59999;
        final long foregroundTimeTwoMinutes = 1;
        Bundle bundle = new Bundle(2);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME, backgroundTimeZero);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME, foregroundTimeTwoMinutes);
        when(mFragment.getArguments()).thenReturn(bundle);

        mFragment.initHeader();

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mEntityHeaderController).setSummary(captor.capture());
        assertThat(captor.getValue().toString()).isEqualTo(
                "1 min total • background less than a minute\nsince last full charge");
    }

    @Test
    public void initHeader_TotalAMinBackgroundZero_hasCorrectSummary() {
        final long backgroundTimeZero = 0;
        final long foregroundTimeAMinutes = 60000;
        Bundle bundle = new Bundle(2);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME, backgroundTimeZero);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME, foregroundTimeAMinutes);
        when(mFragment.getArguments()).thenReturn(bundle);

        mFragment.initHeader();

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mEntityHeaderController).setSummary(captor.capture());
        assertThat(captor.getValue().toString())
                .isEqualTo("1 min total since last full charge");
    }

    @Test
    public void initHeader_foregroundTwoMinBackgroundFourMin_hasCorrectSummary() {
        final long backgroundTimeFourMinute = 240000;
        final long foregroundTimeTwoMinutes = 120000;
        Bundle bundle = new Bundle(2);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME, backgroundTimeFourMinute);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME, foregroundTimeTwoMinutes);
        when(mFragment.getArguments()).thenReturn(bundle);

        mFragment.initHeader();

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mEntityHeaderController).setSummary(captor.capture());
        assertThat(captor.getValue().toString())
                .isEqualTo("6 min total • 4 min background\nsince last full charge");
    }

    @Test
    public void initHeader_totalUsageLessThanAMinWithSlotTime_hasCorrectSummary() {
        final long backgroundTimeLessThanHalfMinute = 20000;
        final long foregroundTimeLessThanHalfMinute = 20000;
        Bundle bundle = new Bundle(3);
        bundle.putLong(
                AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME, backgroundTimeLessThanHalfMinute);
        bundle.putLong(
                AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME, foregroundTimeLessThanHalfMinute);
        bundle.putString(AdvancedPowerUsageDetail.EXTRA_SLOT_TIME, SLOT_TIME);
        when(mFragment.getArguments()).thenReturn(bundle);

        mFragment.initHeader();

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mEntityHeaderController).setSummary(captor.capture());
        assertThat(captor.getValue().toString())
                .isEqualTo("Total less than a minute for 12 am-2 am");
    }

    @Test
    public void initHeader_TotalAMinBackgroundLessThanAMinWithSlotTime_hasCorrectSummary() {
        final long backgroundTimeZero = 59999;
        final long foregroundTimeTwoMinutes = 1;
        Bundle bundle = new Bundle(3);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME, backgroundTimeZero);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME, foregroundTimeTwoMinutes);
        bundle.putString(AdvancedPowerUsageDetail.EXTRA_SLOT_TIME, SLOT_TIME);
        when(mFragment.getArguments()).thenReturn(bundle);

        mFragment.initHeader();

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mEntityHeaderController).setSummary(captor.capture());
        assertThat(captor.getValue().toString())
                .isEqualTo("1 min total • background less than a minute\nfor 12 am-2 am");
    }

    @Test
    public void initHeader_TotalAMinBackgroundZeroWithSlotTime_hasCorrectSummary() {
        final long backgroundTimeZero = 0;
        final long foregroundTimeAMinutes = 60000;
        Bundle bundle = new Bundle(3);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME, backgroundTimeZero);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME, foregroundTimeAMinutes);
        bundle.putString(AdvancedPowerUsageDetail.EXTRA_SLOT_TIME, SLOT_TIME);
        when(mFragment.getArguments()).thenReturn(bundle);

        mFragment.initHeader();

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mEntityHeaderController).setSummary(captor.capture());
        assertThat(captor.getValue().toString())
                .isEqualTo("1 min total for 12 am-2 am");
    }

    @Test
    public void initHeader_foregroundTwoMinBackgroundFourMinWithSlotTime_hasCorrectSummary() {
        final long backgroundTimeFourMinute = 240000;
        final long foregroundTimeTwoMinutes = 120000;
        Bundle bundle = new Bundle(3);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME, backgroundTimeFourMinute);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME, foregroundTimeTwoMinutes);
        bundle.putString(AdvancedPowerUsageDetail.EXTRA_SLOT_TIME, SLOT_TIME);
        when(mFragment.getArguments()).thenReturn(bundle);

        mFragment.initHeader();

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mEntityHeaderController).setSummary(captor.capture());
        assertThat(captor.getValue().toString())
                .isEqualTo("6 min total • 4 min background\nfor 12 am-2 am");
    }

    @Test
    public void initHeader_systemUidWithChartIsDisabled_nullSummary() {
        Bundle bundle = new Bundle(3);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME, 240000);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME, 120000);
        bundle.putInt(AdvancedPowerUsageDetail.EXTRA_UID, Process.SYSTEM_UID);
        when(mFragment.getArguments()).thenReturn(bundle);
        when(mFeatureFactory.powerUsageFeatureProvider.isChartGraphEnabled(mContext))
                .thenReturn(false);

        mFragment.initHeader();

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mEntityHeaderController).setSummary(captor.capture());
        assertThat(captor.getValue()).isNull();
    }

    @Test
    public void initHeader_systemUidWithChartIsEnabled_notNullSummary() {
        Bundle bundle = new Bundle(3);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME, 240000);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME, 120000);
        bundle.putInt(AdvancedPowerUsageDetail.EXTRA_UID, Process.SYSTEM_UID);
        when(mFragment.getArguments()).thenReturn(bundle);

        mFragment.initHeader();

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mEntityHeaderController).setSummary(captor.capture());
        assertThat(captor.getValue()).isNotNull();
    }

    @Test
    public void startBatteryDetailPage_hasBasicData() {
        AdvancedPowerUsageDetail.startBatteryDetailPage(mActivity, mFragment,
                mBatteryEntry, USAGE_PERCENT, /*isValidToShowSummary=*/ true);

        assertThat(mBundle.getInt(AdvancedPowerUsageDetail.EXTRA_UID)).isEqualTo(UID);
        assertThat(mBundle.getLong(AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME))
                .isEqualTo(BACKGROUND_TIME_MS);
        assertThat(mBundle.getLong(AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME))
                .isEqualTo(FOREGROUND_TIME_MS);
        assertThat(mBundle.getString(AdvancedPowerUsageDetail.EXTRA_POWER_USAGE_PERCENT))
                .isEqualTo(USAGE_PERCENT);
    }

    @Test
    public void startBatteryDetailPage_invalidToShowSummary_noFGBDData() {
        AdvancedPowerUsageDetail.startBatteryDetailPage(mActivity, mFragment,
                mBatteryEntry, USAGE_PERCENT, /*isValidToShowSummary=*/ false);

        assertThat(mBundle.getInt(AdvancedPowerUsageDetail.EXTRA_UID)).isEqualTo(UID);
        assertThat(mBundle.getLong(AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME))
                .isEqualTo(0);
        assertThat(mBundle.getLong(AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME))
                .isEqualTo(0);
        assertThat(mBundle.getString(AdvancedPowerUsageDetail.EXTRA_POWER_USAGE_PERCENT))
                .isEqualTo(USAGE_PERCENT);
    }

    @Test
    public void startBatteryDetailPage_NormalApp() {
        when(mBatteryEntry.getDefaultPackageName()).thenReturn(PACKAGE_NAME[0]);

        AdvancedPowerUsageDetail.startBatteryDetailPage(mActivity, mFragment,
                mBatteryEntry, USAGE_PERCENT, /*isValidToShowSummary=*/ true);

        assertThat(mBundle.getString(AdvancedPowerUsageDetail.EXTRA_PACKAGE_NAME)).isEqualTo(
                PACKAGE_NAME[0]);
    }

    @Test
    public void startBatteryDetailPage_SystemApp() {
        when(mBatteryEntry.getDefaultPackageName()).thenReturn(null);

        AdvancedPowerUsageDetail.startBatteryDetailPage(mActivity, mFragment,
                mBatteryEntry, USAGE_PERCENT, /*isValidToShowSummary=*/ true);

        assertThat(mBundle.getString(AdvancedPowerUsageDetail.EXTRA_LABEL)).isEqualTo(APP_LABEL);
        assertThat(mBundle.getInt(AdvancedPowerUsageDetail.EXTRA_ICON_ID)).isEqualTo(ICON_ID);
        assertThat(mBundle.getString(AdvancedPowerUsageDetail.EXTRA_PACKAGE_NAME)).isNull();
    }

    @Test
    public void startBatteryDetailPage_WorkApp() {
        final int appUid = 1010019;
        doReturn(appUid).when(mBatteryEntry).getUid();

        AdvancedPowerUsageDetail.startBatteryDetailPage(mActivity, mFragment,
                mBatteryEntry, USAGE_PERCENT, /*isValidToShowSummary=*/ true);

        verify(mActivity).startActivityAsUser(any(Intent.class), eq(new UserHandle(10)));
    }

    @Test
    public void startBatteryDetailPage_typeUser_startByCurrentUser() {
        when(mBatteryEntry.isUserEntry()).thenReturn(true);

        final int currentUser = 20;
        ShadowActivityManager.setCurrentUser(currentUser);
        AdvancedPowerUsageDetail.startBatteryDetailPage(mActivity, mFragment,
                mBatteryEntry, USAGE_PERCENT, /*isValidToShowSummary=*/ true);

        verify(mActivity).startActivityAsUser(any(Intent.class), eq(new UserHandle(currentUser)));
    }

    @Test
    public void startBatteryDetailPage_noBatteryUsage_hasBasicData() {
        final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);

        AdvancedPowerUsageDetail.startBatteryDetailPage(
                mActivity, mFragment, PACKAGE_NAME[0], UserHandle.OWNER);

        verify(mActivity).startActivity(captor.capture());

        assertThat(captor.getValue().getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS)
                .getString(AdvancedPowerUsageDetail.EXTRA_PACKAGE_NAME))
                .isEqualTo(PACKAGE_NAME[0]);

        assertThat(captor.getValue().getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS)
                .getString(AdvancedPowerUsageDetail.EXTRA_POWER_USAGE_PERCENT))
                .isEqualTo("0%");
    }

    @Test
    public void startBatteryDetailPage_batteryEntryNotExisted_extractUidFromPackageName() throws
            PackageManager.NameNotFoundException {
        doReturn(UID).when(mPackageManager).getPackageUid(PACKAGE_NAME[0], 0 /* no flag */);

        AdvancedPowerUsageDetail.startBatteryDetailPage(
                mActivity, mFragment, PACKAGE_NAME[0], UserHandle.OWNER);

        assertThat(mBundle.getInt(AdvancedPowerUsageDetail.EXTRA_UID)).isEqualTo(UID);
    }

    @Test
    public void testInitPreference_hasCorrectSummary() {
        Bundle bundle = new Bundle(4);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_BACKGROUND_TIME, BACKGROUND_TIME_MS);
        bundle.putLong(AdvancedPowerUsageDetail.EXTRA_FOREGROUND_TIME, FOREGROUND_TIME_MS);
        bundle.putString(AdvancedPowerUsageDetail.EXTRA_POWER_USAGE_PERCENT, USAGE_PERCENT);
        bundle.putInt(AdvancedPowerUsageDetail.EXTRA_POWER_USAGE_AMOUNT, POWER_MAH);
        when(mFragment.getArguments()).thenReturn(bundle);

        doReturn(mContext.getText(R.string.battery_used_for)).when(mFragment).getText(
                R.string.battery_used_for);
        doReturn(mContext.getText(R.string.battery_active_for)).when(mFragment).getText(
                R.string.battery_active_for);

        mFragment.initPreference(mContext);

        assertThat(mForegroundPreference.getSummary().toString()).isEqualTo("Used for 0 min");
        assertThat(mBackgroundPreference.getSummary().toString()).isEqualTo("Active for 0 min");
    }

    @Test
    public void testInitPreferenceForTriState_isValidPackageName_hasCorrectString() {
        when(mBatteryOptimizeUtils.isValidPackageName()).thenReturn(false);

        mFragment.initPreferenceForTriState(mContext);

        assertThat(mFooterPreference.getTitle().toString())
                .isEqualTo("This app requires optimized battery usage.");
    }

    @Test
    public void initPreferenceForTriState_isSystemOrDefaultApp_hasCorrectString() {
        when(mBatteryOptimizeUtils.isValidPackageName()).thenReturn(true);
        when(mBatteryOptimizeUtils.isSystemOrDefaultApp()).thenReturn(true);

        mFragment.initPreferenceForTriState(mContext);

        assertThat(mFooterPreference.getTitle()
                .toString()).isEqualTo("This app requires unrestricted battery usage.");
    }

    @Test
    public void initPreferenceForTriState_hasCorrectString() {
        when(mBatteryOptimizeUtils.isValidPackageName()).thenReturn(true);
        when(mBatteryOptimizeUtils.isSystemOrDefaultApp()).thenReturn(false);

        mFragment.initPreferenceForTriState(mContext);

        assertThat(mFooterPreference.getTitle().toString())
                .isEqualTo("Changing how an app uses your battery can affect its performance.");
    }

    @Test
    public void onRadioButtonClicked_clickOptimizePref_optimizePreferenceChecked() {
        mOptimizePreference.setKey(KEY_PREF_OPTIMIZED);
        mRestrictedPreference.setKey(KEY_PREF_RESTRICTED);
        mUnrestrictedPreference.setKey(KEY_PREF_UNRESTRICTED);
        mFragment.onRadioButtonClicked(mOptimizePreference);

        assertThat(mOptimizePreference.isChecked()).isTrue();
        assertThat(mRestrictedPreference.isChecked()).isFalse();
        assertThat(mUnrestrictedPreference.isChecked()).isFalse();
    }

    @Test
    public void onPause_optimizationModeChanged_logPreference() {
        final int mode = BatteryOptimizeUtils.MODE_RESTRICTED;
        mFragment.mOptimizationMode = mode;
        when(mBatteryOptimizeUtils.getAppOptimizationMode()).thenReturn(mode);
        mOptimizePreference.setKey(KEY_PREF_OPTIMIZED);

        mFragment.onRadioButtonClicked(mOptimizePreference);
        mFragment.onPause();

        verify(mMetricsFeatureProvider)
                .action(
                        SettingsEnums.OPEN_APP_BATTERY_USAGE,
                        SettingsEnums.ACTION_APP_BATTERY_USAGE_OPTIMIZED,
                        SettingsEnums.OPEN_APP_BATTERY_USAGE,
                        /* package name*/ "none",
                        /* consumed battery */ 0);
    }

    @Test
    public void onPause_optimizationModeIsNotChanged_notInvokeLogging() {
        final int mode = BatteryOptimizeUtils.MODE_OPTIMIZED;
        mFragment.mOptimizationMode = mode;
        when(mBatteryOptimizeUtils.getAppOptimizationMode()).thenReturn(mode);
        mOptimizePreference.setKey(KEY_PREF_OPTIMIZED);

        mFragment.onRadioButtonClicked(mOptimizePreference);
        mFragment.onPause();

        verifyZeroInteractions(mMetricsFeatureProvider);
    }

    @Test
    public void notifyBackupManager_optimizationModeIsNotChanged_notInvokeDataChanged() {
        final int mode = BatteryOptimizeUtils.MODE_RESTRICTED;
        mFragment.mOptimizationMode = mode;
        when(mBatteryOptimizeUtils.getAppOptimizationMode()).thenReturn(mode);

        mFragment.notifyBackupManager();

        verifyZeroInteractions(mBackupManager);
    }

    @Test
    public void notifyBackupManager_optimizationModeIsChanged_invokeDataChanged() {
        mFragment.mOptimizationMode = BatteryOptimizeUtils.MODE_RESTRICTED;
        when(mBatteryOptimizeUtils.getAppOptimizationMode())
                .thenReturn(BatteryOptimizeUtils.MODE_UNRESTRICTED);

        mFragment.notifyBackupManager();

        verify(mBackupManager).dataChanged();
    }

    @Test
    public void notifyBackupManager_triStateIsNotEnabled_notInvokeDataChanged() {
        mFragment.mOptimizationMode = BatteryOptimizeUtils.MODE_RESTRICTED;
        when(mBatteryOptimizeUtils.getAppOptimizationMode())
                .thenReturn(BatteryOptimizeUtils.MODE_UNRESTRICTED);
        mFragment.mEnableTriState = false;

        mFragment.onPause();

        verifyZeroInteractions(mBackupManager);
    }
}
