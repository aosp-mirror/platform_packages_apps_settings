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
package com.android.settings.fuelgauge.batterytip.tips;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Parcel;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.testutils.BatteryTestUtils;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class RestrictAppTipTest {
    private static final String PACKAGE_NAME = "com.android.app";
    private static final String UNINSTALL_PACKAGE_NAME = "com.android.app.unintall";
    private static final String DISPLAY_NAME = "app";
    private static final int ANOMALY_WAKEUP = 0;
    private static final int ANOMALY_WAKELOCK = 1;

    private Context mContext;
    private RestrictAppTip mNewBatteryTip;
    private RestrictAppTip mHandledBatteryTip;
    private RestrictAppTip mInvisibleBatteryTip;
    private List<AppInfo> mUsageAppList;
    private AppInfo mAppInfo;
    private AppInfo mUninstallAppInfo;
    @Mock
    private ApplicationInfo mApplicationInfo;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private MetricsFeatureProvider mMetricsFeatureProvider;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        doReturn(mContext).when(mContext).getApplicationContext();
        doReturn(mPackageManager).when(mContext).getPackageManager();
        doReturn(mApplicationInfo).when(mPackageManager).getApplicationInfo(PACKAGE_NAME,
                PackageManager.MATCH_DISABLED_COMPONENTS | PackageManager.MATCH_ANY_USER);
        doThrow(new PackageManager.NameNotFoundException()).when(mPackageManager)
                .getApplicationInfo(UNINSTALL_PACKAGE_NAME,
                        PackageManager.MATCH_DISABLED_COMPONENTS | PackageManager.MATCH_ANY_USER);
        doReturn(DISPLAY_NAME).when(mApplicationInfo).loadLabel(mPackageManager);

        mAppInfo = new AppInfo.Builder()
                .setPackageName(PACKAGE_NAME)
                .addAnomalyType(ANOMALY_WAKEUP)
                .addAnomalyType(ANOMALY_WAKELOCK)
                .build();
        mUninstallAppInfo = new AppInfo.Builder()
                .setPackageName(UNINSTALL_PACKAGE_NAME)
                .addAnomalyType(ANOMALY_WAKEUP)
                .build();
        mUsageAppList = new ArrayList<>();
        mUsageAppList.add(mAppInfo);
        mNewBatteryTip = new RestrictAppTip(BatteryTip.StateType.NEW, mUsageAppList);
        mHandledBatteryTip = new RestrictAppTip(BatteryTip.StateType.HANDLED, mUsageAppList);
        mInvisibleBatteryTip = new RestrictAppTip(BatteryTip.StateType.INVISIBLE,
                new ArrayList<>());
    }

    @After
    public void tearDown() {
        ReflectionHelpers.setStaticField(AppLabelPredicate.class, "sInstance", null);
        ReflectionHelpers.setStaticField(AppRestrictionPredicate.class, "sInstance", null);
    }

    @Test
    public void parcelable() {
        Parcel parcel = Parcel.obtain();
        mNewBatteryTip.writeToParcel(parcel, mNewBatteryTip.describeContents());
        parcel.setDataPosition(0);

        final RestrictAppTip parcelTip = new RestrictAppTip(parcel);

        assertThat(parcelTip.getType()).isEqualTo(BatteryTip.TipType.APP_RESTRICTION);
        assertThat(parcelTip.getState()).isEqualTo(BatteryTip.StateType.NEW);
        final AppInfo app = parcelTip.getRestrictAppList().get(0);
        assertThat(app.packageName).isEqualTo(PACKAGE_NAME);
    }

    @Test
    public void getTitle_stateNew_showRestrictTitle() {
        assertThat(mNewBatteryTip.getTitle(mContext)).isEqualTo("Restrict 1 app");
    }

    @Test
    public void getTitle_oneAppHandled_showHandledTitle() {
        assertThat(mHandledBatteryTip.getTitle(mContext)).isEqualTo("app recently restricted");
    }

    @Test
    public void getTitle_moreAppsHandled_showHandledTitle() {
        mUsageAppList.add(new AppInfo.Builder().build());
        mHandledBatteryTip = new RestrictAppTip(BatteryTip.StateType.HANDLED, mUsageAppList);
        assertThat(mHandledBatteryTip.getTitle(mContext)).isEqualTo("2 apps recently restricted");
    }

    @Test
    public void getSummary_stateNew_showRestrictSummary() {
        assertThat(mNewBatteryTip.getSummary(mContext))
                .isEqualTo("app has high background battery usage");
    }

    @Test
    public void getSummary_oneAppHandled_showHandledSummary() {
        assertThat(mHandledBatteryTip.getSummary(mContext).toString())
                .isEqualTo(mContext.getResources().getQuantityString(
                        R.plurals.battery_tip_restrict_handled_summary, 1));
    }

    @Test
    public void getSummary_moreAppsHandled_showHandledSummary() {
        mUsageAppList.add(new AppInfo.Builder().build());
        mHandledBatteryTip = new RestrictAppTip(BatteryTip.StateType.HANDLED, mUsageAppList);
        assertThat(mHandledBatteryTip.getSummary(mContext))
                .isEqualTo(mContext.getResources().getQuantityString(
                        R.plurals.battery_tip_restrict_handled_summary, 2));
    }

    @Test
    public void update_anomalyBecomeInvisible_stateHandled() {
        mNewBatteryTip.updateState(mInvisibleBatteryTip);

        assertThat(mNewBatteryTip.getState()).isEqualTo(BatteryTip.StateType.HANDLED);
    }

    @Test
    public void update_handledAnomlayBecomeInvisible_stateInvisible() {
        mHandledBatteryTip.updateState(mInvisibleBatteryTip);

        assertThat(mHandledBatteryTip.getState()).isEqualTo(BatteryTip.StateType.INVISIBLE);
    }

    @Test
    public void update_newAnomalyComes_stateNew() {
        mInvisibleBatteryTip.updateState(mNewBatteryTip);
        assertThat(mInvisibleBatteryTip.getState()).isEqualTo(BatteryTip.StateType.NEW);

        mHandledBatteryTip.updateState(mNewBatteryTip);
        assertThat(mHandledBatteryTip.getState()).isEqualTo(BatteryTip.StateType.NEW);
    }

    @Test
    public void update_newHandledAnomalyComes_containHandledAnomaly() {
        mInvisibleBatteryTip.updateState(mHandledBatteryTip);
        assertThat(mInvisibleBatteryTip.getState()).isEqualTo(BatteryTip.StateType.HANDLED);
        assertThat(mInvisibleBatteryTip.getRestrictAppList()).containsExactly(mAppInfo);
    }

    @Test
    public void sanityCheck_appUninstalled_stateInvisible() {
        final List<AppInfo> appInfos = new ArrayList<>();
        appInfos.add(mUninstallAppInfo);
        final BatteryTip batteryTip = new RestrictAppTip(BatteryTip.StateType.NEW, appInfos);

        batteryTip.sanityCheck(mContext);

        assertThat(batteryTip.getState()).isEqualTo(BatteryTip.StateType.INVISIBLE);
    }

    @Test
    public void sanityCheck_twoRestrictedAppsWhileUninstallOne_stateVisible() {
        final List<AppInfo> appInfos = new ArrayList<>();
        appInfos.add(mAppInfo);
        appInfos.add(mUninstallAppInfo);
        final BatteryTip batteryTip = new RestrictAppTip(BatteryTip.StateType.NEW, appInfos);

        batteryTip.sanityCheck(mContext);

        assertThat(batteryTip.getState()).isEqualTo(BatteryTip.StateType.NEW);
    }

    @Test
    public void toString_containsAppData() {
        assertThat(mNewBatteryTip.toString()).isEqualTo(
                "type=1 state=0 { packageName=com.android.app,anomalyTypes={0, 1},screenTime=0 }");
    }

    @Test
    public void testLog_stateNew_logAppInfo() {
        mNewBatteryTip.log(mContext, mMetricsFeatureProvider);

        verify(mMetricsFeatureProvider).action(mContext,
                MetricsProto.MetricsEvent.ACTION_APP_RESTRICTION_TIP, BatteryTip.StateType.NEW);
        verify(mMetricsFeatureProvider).action(SettingsEnums.PAGE_UNKNOWN,
                MetricsProto.MetricsEvent.ACTION_APP_RESTRICTION_TIP_LIST,
                SettingsEnums.PAGE_UNKNOWN,
                PACKAGE_NAME,
                ANOMALY_WAKEUP);
        verify(mMetricsFeatureProvider).action(SettingsEnums.PAGE_UNKNOWN,
                MetricsProto.MetricsEvent.ACTION_APP_RESTRICTION_TIP_LIST,
                SettingsEnums.PAGE_UNKNOWN,
                PACKAGE_NAME,
                ANOMALY_WAKELOCK);
    }

    @Test
    public void testLog_stateHandled_doNotLogAppInfo() {
        mHandledBatteryTip.log(mContext, mMetricsFeatureProvider);

        verify(mMetricsFeatureProvider).action(mContext,
                MetricsProto.MetricsEvent.ACTION_APP_RESTRICTION_TIP, BatteryTip.StateType.HANDLED);
        verify(mMetricsFeatureProvider, never()).action(
                anyInt(), anyInt(), anyInt(), anyString(), anyInt());
    }
}
