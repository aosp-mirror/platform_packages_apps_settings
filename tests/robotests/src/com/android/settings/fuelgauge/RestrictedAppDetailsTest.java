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

package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.IconDrawableFactory;
import android.util.SparseLongArray;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;

import com.android.settings.SettingsActivity;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.fuelgauge.batterytip.AnomalyDatabaseHelper;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.BatteryDatabaseManager;
import com.android.settings.fuelgauge.batterytip.BatteryTipDialogFragment;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.RestrictAppTip;
import com.android.settings.fuelgauge.batterytip.tips.UnrestrictAppTip;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.androidx.fragment.FragmentController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowAlertDialogCompat.class)
public class RestrictedAppDetailsTest {

    private static final String PACKAGE_NAME = "com.android.app";
    private static final int USER_ID = 10;
    private static final int UID = UserHandle.getUid(USER_ID, 234);
    private static final String APP_NAME = "app";


    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ApplicationInfo mApplicationInfo;
    @Mock
    private IconDrawableFactory mIconDrawableFactory;
    @Mock
    private InstrumentedPreferenceFragment mFragment;
    @Mock
    private BatteryDatabaseManager mBatteryDatabaseManager;
    private PreferenceManager mPreferenceManager;
    private RestrictedAppDetails mRestrictedAppDetails;
    private Context mContext;
    private AppInfo mAppInfo;
    private Intent mIntent;
    private CheckBoxPreference mCheckBoxPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mRestrictedAppDetails = spy(new RestrictedAppDetails());
        mAppInfo = new AppInfo.Builder()
                .setPackageName(PACKAGE_NAME)
                .setUid(UID)
                .build();

        mPreferenceManager = new PreferenceManager(mContext);

        doReturn(mPreferenceManager).when(mRestrictedAppDetails).getPreferenceManager();
        doReturn(mContext).when(mFragment).getContext();
        doReturn(mContext).when(mRestrictedAppDetails).getContext();
        mRestrictedAppDetails.mPackageManager = mPackageManager;
        mRestrictedAppDetails.mIconDrawableFactory = mIconDrawableFactory;
        mRestrictedAppDetails.mAppInfos = new ArrayList<>();
        mRestrictedAppDetails.mAppInfos.add(mAppInfo);
        mRestrictedAppDetails.mRestrictedAppListGroup = spy(new PreferenceCategory(mContext));
        mRestrictedAppDetails.mBatteryUtils = spy(new BatteryUtils(mContext));
        mRestrictedAppDetails.mBatteryDatabaseManager = mBatteryDatabaseManager;
        doReturn(mPreferenceManager).when(
                mRestrictedAppDetails.mRestrictedAppListGroup).getPreferenceManager();

        mCheckBoxPreference = new CheckBoxPreference(mContext);
        mCheckBoxPreference.setKey(mRestrictedAppDetails.getKeyFromAppInfo(mAppInfo));
    }

    @Test
    public void refreshUi_displayPreference() throws Exception {
        doReturn(mApplicationInfo).when(mPackageManager)
                .getApplicationInfoAsUser(PACKAGE_NAME, 0, USER_ID);
        doReturn(APP_NAME).when(mPackageManager).getApplicationLabel(mApplicationInfo);
        doReturn(true).when(mRestrictedAppDetails.mBatteryUtils).isForceAppStandbyEnabled(UID,
                PACKAGE_NAME);
        final SparseLongArray timestampArray = new SparseLongArray();
        timestampArray.put(UID, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(5));
        doReturn(timestampArray).when(mBatteryDatabaseManager)
                .queryActionTime(AnomalyDatabaseHelper.ActionType.RESTRICTION);

        mRestrictedAppDetails.refreshUi();

        assertThat(mRestrictedAppDetails.mRestrictedAppListGroup.getPreferenceCount()).isEqualTo(1);
        final CheckBoxPreference preference =
                (CheckBoxPreference) mRestrictedAppDetails.mRestrictedAppListGroup.getPreference(0);
        assertThat(preference.getTitle()).isEqualTo(APP_NAME);
        assertThat(preference.isChecked()).isTrue();
        assertThat(preference.getSummary()).isEqualTo("Restricted 5 hours ago");
    }

    @Test
    public void startRestrictedAppDetails_startWithCorrectData() {
        final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        doAnswer(invocation -> {
            // Get the intent in which it has the app info bundle
            mIntent = captor.getValue();
            return true;
        }).when(mContext).startActivity(captor.capture());

        RestrictedAppDetails.startRestrictedAppDetails(mFragment,
                mRestrictedAppDetails.mAppInfos);

        final Bundle bundle = mIntent.getBundleExtra(
                SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        // Verify the bundle has the correct info
        final List<AppInfo> appInfos = bundle.getParcelableArrayList(
                RestrictedAppDetails.EXTRA_APP_INFO_LIST);
        assertThat(appInfos).containsExactly(mAppInfo);
    }

    @Test
    public void createDialogFragment_toRestrict_createRestrictDialog() {
        final BatteryTipDialogFragment dialogFragment = mRestrictedAppDetails.createDialogFragment(
                mAppInfo, true);

        FragmentController.setupFragment(dialogFragment, FragmentActivity.class,
                0 /* containerViewId */, null /* bundle */);

        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        assertThat(shadowDialog.getTitle()).isEqualTo("Restrict app?");
    }

    @Test
    public void createDialogFragment_toUnrestrict_createUnrestrictDialog() {
        final BatteryTipDialogFragment dialogFragment = mRestrictedAppDetails.createDialogFragment(
                mAppInfo, false);

        FragmentController.setupFragment(dialogFragment, FragmentActivity.class,
                0 /* containerViewId */, null /* bundle */);

        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        assertThat(shadowDialog.getTitle()).isEqualTo("Remove restriction?");
    }

    @Test
    public void onBatteryTipHandled_restrict_setChecked() {
        final RestrictAppTip restrictAppTip = new RestrictAppTip(BatteryTip.StateType.NEW,
                mAppInfo);
        mRestrictedAppDetails.mRestrictedAppListGroup.addPreference(mCheckBoxPreference);

        mRestrictedAppDetails.onBatteryTipHandled(restrictAppTip);

        assertThat(mCheckBoxPreference.isChecked()).isTrue();
    }

    @Test
    public void onBatteryTipHandled_unrestrict_setUnchecked() {
        final UnrestrictAppTip unrestrictAppTip = new UnrestrictAppTip(BatteryTip.StateType.NEW,
                mAppInfo);
        mRestrictedAppDetails.mRestrictedAppListGroup.addPreference(mCheckBoxPreference);

        mRestrictedAppDetails.onBatteryTipHandled(unrestrictAppTip);

        assertThat(mCheckBoxPreference.isChecked()).isFalse();
    }
}
