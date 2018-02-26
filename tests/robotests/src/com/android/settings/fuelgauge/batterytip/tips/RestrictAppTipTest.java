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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Parcel;

import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class RestrictAppTipTest {

    private static final String PACKAGE_NAME = "com.android.app";
    private static final String DISPLAY_NAME = "app";

    private Context mContext;
    private RestrictAppTip mNewBatteryTip;
    private RestrictAppTip mHandledBatteryTip;
    private RestrictAppTip mInvisibleBatteryTip;
    private List<AppInfo> mUsageAppList;
    @Mock
    private ApplicationInfo mApplicationInfo;
    @Mock
    private PackageManager mPackageManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        doReturn(mPackageManager).when(mContext).getPackageManager();
        doReturn(mApplicationInfo).when(mPackageManager).getApplicationInfo(PACKAGE_NAME,
                PackageManager.MATCH_DISABLED_COMPONENTS | PackageManager.MATCH_ANY_USER);
        doReturn(DISPLAY_NAME).when(mApplicationInfo).loadLabel(mPackageManager);

        mUsageAppList = new ArrayList<>();
        mUsageAppList.add(new AppInfo.Builder().setPackageName(PACKAGE_NAME).build());
        mNewBatteryTip = new RestrictAppTip(BatteryTip.StateType.NEW, mUsageAppList);
        mHandledBatteryTip = new RestrictAppTip(BatteryTip.StateType.HANDLED, mUsageAppList);
        mInvisibleBatteryTip = new RestrictAppTip(BatteryTip.StateType.INVISIBLE, mUsageAppList);
    }

    @Test
    public void testParcelable() {
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
    public void testGetTitle_stateNew_showRestrictTitle() {
        assertThat(mNewBatteryTip.getTitle(mContext)).isEqualTo("Restrict 1 app");
    }

    @Test
    public void testGetTitle_stateHandled_showHandledTitle() {
        assertThat(mHandledBatteryTip.getTitle(mContext)).isEqualTo("1 recently restricted");
    }

    @Test
    public void testGetSummary_stateNew_showRestrictSummary() {
        assertThat(mNewBatteryTip.getSummary(mContext))
            .isEqualTo("app has high battery usage");
    }

    @Test
    public void testGetSummary_stateHandled_showHandledSummary() {
        assertThat(mHandledBatteryTip.getSummary(mContext))
            .isEqualTo("App changes are in progress");
    }

    @Test
    public void testUpdate_anomalyBecomeInvisible_stateHandled() {
        mNewBatteryTip.updateState(mInvisibleBatteryTip);

        assertThat(mNewBatteryTip.getState()).isEqualTo(BatteryTip.StateType.HANDLED);
    }

    @Test
    public void testUpdate_newAnomalyComes_stateNew() {
        mInvisibleBatteryTip.updateState(mNewBatteryTip);
        assertThat(mInvisibleBatteryTip.getState()).isEqualTo(BatteryTip.StateType.NEW);

        mHandledBatteryTip.updateState(mNewBatteryTip);
        assertThat(mHandledBatteryTip.getState()).isEqualTo(BatteryTip.StateType.NEW);
    }
}
