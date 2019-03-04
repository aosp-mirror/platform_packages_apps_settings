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

import static com.android.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT;
import static com.android.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.fuelgauge.batterytip.AppInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;

import java.util.ArrayList;
import java.util.List;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class RestrictAppPreferenceControllerTest {
    private static final int ALLOWED_UID = 111;
    private static final String ALLOWED_PACKAGE_NAME = "com.android.allowed.package";
    private static final int RESTRICTED_UID = 222;
    private static final String RESTRICTED_PACKAGE_NAME = "com.android.restricted.package";
    private static final int OTHER_USER_UID = UserHandle.PER_USER_RANGE + RESTRICTED_UID;

    @Mock
    private AppOpsManager mAppOpsManager;
    @Mock
    private InstrumentedPreferenceFragment mFragment;
    @Mock
    private UserManager mUserManager;

    private AppOpsManager.PackageOps mRestrictedPackageOps;
    private AppOpsManager.PackageOps mAllowedPackageOps;
    private AppOpsManager.PackageOps mOtherUserPackageOps;
    private List<AppOpsManager.PackageOps> mPackageOpsList;
    private RestrictAppPreferenceController mRestrictAppPreferenceController;
    private Preference mPreference;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final AppOpsManager.OpEntry allowOpEntry = new AppOpsManager.OpEntry(
            AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, false, AppOpsManager.MODE_ALLOWED,
            null /*accessTimes*/, null /*rejectTimes*/,  null /*durations*/,
            null /* proxyUids */, null /* proxyPackages */);
        final List<AppOpsManager.OpEntry> allowOps = new ArrayList<>();
        allowOps.add(allowOpEntry);
        final AppOpsManager.OpEntry restrictedOpEntry = new AppOpsManager.OpEntry(
            AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, false, AppOpsManager.MODE_IGNORED,
            null /*accessTimes*/, null /*rejectTimes*/,  null /*durations*/,
            null /* proxyUids */, null /* proxyPackages */);
        final List<AppOpsManager.OpEntry> restrictedOps = new ArrayList<>();
        restrictedOps.add(restrictedOpEntry);
        mAllowedPackageOps = new AppOpsManager.PackageOps(
                ALLOWED_PACKAGE_NAME, ALLOWED_UID, allowOps);
        mRestrictedPackageOps = new AppOpsManager.PackageOps(
                RESTRICTED_PACKAGE_NAME, RESTRICTED_UID, restrictedOps);
        mOtherUserPackageOps = new AppOpsManager.PackageOps(
                RESTRICTED_PACKAGE_NAME, OTHER_USER_UID, restrictedOps);

        mContext = spy(Robolectric.setupActivity(Activity.class));
        doReturn(mAppOpsManager).when(mContext).getSystemService(Context.APP_OPS_SERVICE);
        doReturn(mUserManager).when(mContext).getSystemService(UserManager.class);
        doReturn(mContext).when(mFragment).getContext();
        mRestrictAppPreferenceController =
                new RestrictAppPreferenceController(mFragment);
        mPackageOpsList = new ArrayList<>();
        mPreference = new Preference(mContext);
        mPreference.setKey(mRestrictAppPreferenceController.getPreferenceKey());

        final List<UserHandle> userHandles = new ArrayList<>();
        userHandles.add(new UserHandle(0));
        doReturn(userHandles).when(mUserManager).getUserProfiles();
    }

    @Test
    public void testUpdateState_oneApp_showCorrectSummary() {
        mPackageOpsList.add(mRestrictedPackageOps);
        doReturn(mPackageOpsList).when(mAppOpsManager).getPackagesForOps(any(int[].class));

        mRestrictAppPreferenceController.updateState(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo("Limiting battery usage for 1 app");
    }

    @Test
    public void testUpdateState_twoRestrictedAppsForPrimaryUser_visibleAndShowCorrectSummary() {
        mPackageOpsList.add(mRestrictedPackageOps);
        mPackageOpsList.add(mRestrictedPackageOps);
        mPackageOpsList.add(mAllowedPackageOps);
        mPackageOpsList.add(mOtherUserPackageOps);
        doReturn(mPackageOpsList).when(mAppOpsManager).getPackagesForOps(any(int[].class));

        mRestrictAppPreferenceController.updateState(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo("Limiting battery usage for 2 apps");
        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void testUpdateState_oneRestrictedAppForTwoUsers_showSummaryAndContainCorrectApp() {
        // Two packageOps share same package name but different uid.
        mPackageOpsList.add(mRestrictedPackageOps);
        mPackageOpsList.add(mOtherUserPackageOps);
        doReturn(mPackageOpsList).when(mAppOpsManager).getPackagesForOps(any(int[].class));

        mRestrictAppPreferenceController.updateState(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo("Limiting battery usage for 1 app");
        assertThat(mRestrictAppPreferenceController.mAppInfos).containsExactly(
                new AppInfo.Builder()
                        .setUid(RESTRICTED_UID)
                        .setPackageName(RESTRICTED_PACKAGE_NAME)
                        .build());
    }

    @Test
    public void testUpdateState_zeroRestrictApp_inVisible() {
        mPackageOpsList.add(mAllowedPackageOps);
        doReturn(mPackageOpsList).when(mAppOpsManager).getPackagesForOps(any(int[].class));

        mRestrictAppPreferenceController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void testHandlePreferenceTreeClick_startFragment() {
        final ArgumentCaptor<Intent> intent = ArgumentCaptor.forClass(Intent.class);

        mRestrictAppPreferenceController.handlePreferenceTreeClick(mPreference);

        verify(mContext).startActivity(intent.capture());
        assertThat(intent.getValue().getStringExtra(EXTRA_SHOW_FRAGMENT))
                .isEqualTo(RestrictedAppDetails.class.getName());
        assertThat(intent.getValue().getIntExtra(EXTRA_SHOW_FRAGMENT_TITLE_RESID, -1))
                .isEqualTo(R.string.restricted_app_title);
    }
}
