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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceManager;

import com.android.settings.TestConfig;

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

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class RestrictedAppDetailsTest {
    private static final String PACKAGE_NAME = "com.android.app";
    private static final String APP_NAME = "app";
    private static final int UID = 1234;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ApplicationInfo mApplicationInfo;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceManager mPreferenceManager;
    private RestrictedAppDetails mFragment;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mFragment = spy(new RestrictedAppDetails());

        doReturn(mPreferenceManager).when(mFragment).getPreferenceManager();
        doReturn(mContext).when(mPreferenceManager).getContext();
        mFragment.mPackageManager = mPackageManager;
        mFragment.mPackageOpsList = new ArrayList<>();
        mFragment.mPackageOpsList.add(
                new AppOpsManager.PackageOps(PACKAGE_NAME, UID, null /* entries */));
        mFragment.mRestrictedAppListGroup = spy(new PreferenceCategory(mContext));
        doReturn(mPreferenceManager).when(mFragment.mRestrictedAppListGroup).getPreferenceManager();
    }

    @Test
    public void testRefreshUi_displayPreference() throws Exception {
        doReturn(mApplicationInfo).when(mPackageManager).getApplicationInfo(PACKAGE_NAME, 0);
        doReturn(APP_NAME).when(mPackageManager).getApplicationLabel(mApplicationInfo);

        mFragment.refreshUi();

        assertThat(mFragment.mRestrictedAppListGroup.getPreferenceCount()).isEqualTo(1);
        final Preference preference = mFragment.mRestrictedAppListGroup.getPreference(0);
        assertThat(preference.getKey()).isEqualTo(PACKAGE_NAME);
        assertThat(preference.getTitle()).isEqualTo(APP_NAME);
    }

}
