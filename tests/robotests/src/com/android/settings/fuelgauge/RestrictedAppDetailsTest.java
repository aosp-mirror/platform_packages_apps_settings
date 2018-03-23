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
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceManager;
import android.util.IconDrawableFactory;

import com.android.settings.SettingsActivity;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.fuelgauge.batterytip.AppInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
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
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceManager mPreferenceManager;
    @Mock
    private SettingsActivity mSettingsActivity;
    @Mock
    private InstrumentedPreferenceFragment mFragment;
    private RestrictedAppDetails mRestrictedAppDetails;
    private Context mContext;
    private AppInfo mAppInfo;
    private Intent mIntent;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mRestrictedAppDetails = spy(new RestrictedAppDetails());
        mAppInfo = new AppInfo.Builder()
                .setPackageName(PACKAGE_NAME)
                .setUid(UID)
                .build();

        doReturn(mPreferenceManager).when(mRestrictedAppDetails).getPreferenceManager();
        doReturn(mContext).when(mPreferenceManager).getContext();
        mRestrictedAppDetails.mPackageManager = mPackageManager;
        mRestrictedAppDetails.mIconDrawableFactory = mIconDrawableFactory;
        mRestrictedAppDetails.mAppInfos = new ArrayList<>();
        mRestrictedAppDetails.mAppInfos.add(mAppInfo);
        mRestrictedAppDetails.mRestrictedAppListGroup = spy(new PreferenceCategory(mContext));
        mRestrictedAppDetails.mBatteryUtils = new BatteryUtils(mContext);
        doReturn(mPreferenceManager).when(
                mRestrictedAppDetails.mRestrictedAppListGroup).getPreferenceManager();
    }

    @Test
    public void testRefreshUi_displayPreference() throws Exception {
        doReturn(mApplicationInfo).when(mPackageManager)
                .getApplicationInfoAsUser(PACKAGE_NAME, 0, USER_ID);
        doReturn(APP_NAME).when(mPackageManager).getApplicationLabel(mApplicationInfo);

        mRestrictedAppDetails.refreshUi();

        assertThat(mRestrictedAppDetails.mRestrictedAppListGroup.getPreferenceCount()).isEqualTo(1);
        final Preference preference = mRestrictedAppDetails.mRestrictedAppListGroup.getPreference(
                0);
        assertThat(preference.getTitle()).isEqualTo(APP_NAME);
    }

    @Test
    public void testStartRestrictedAppDetails_startWithCorrectData() {
        final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        doAnswer(invocation -> {
            // Get the intent in which it has the app info bundle
            mIntent = captor.getValue();
            return true;
        }).when(mSettingsActivity).startActivity(captor.capture());

        RestrictedAppDetails.startRestrictedAppDetails(mSettingsActivity, mFragment,
                mRestrictedAppDetails.mAppInfos);

        final Bundle bundle = mIntent.getBundleExtra(
                SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        // Verify the bundle has the correct info
        final List<AppInfo> appInfos = bundle.getParcelableArrayList(
                RestrictedAppDetails.EXTRA_APP_INFO_LIST);
        assertThat(appInfos).containsExactly(mAppInfo);
    }
}
