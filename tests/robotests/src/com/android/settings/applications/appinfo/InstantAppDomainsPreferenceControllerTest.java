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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.ArraySet;

import com.android.settings.applications.AppDomainsPreference;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.instantapps.InstantAppDataProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class InstantAppDomainsPreferenceControllerTest {

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ApplicationInfo mAppInfo;
    @Mock
    private AppInfoDashboardFragment mFragment;
    @Mock
    private AppDomainsPreference mPreference;

    private Context mContext;
    private InstantAppDomainsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        final PackageInfo packageInfo = mock(PackageInfo.class);
        packageInfo.applicationInfo = mAppInfo;
        packageInfo.packageName = "Package1";
        when(mFragment.getPackageInfo()).thenReturn(packageInfo);
        mController = new InstantAppDomainsPreferenceController(mContext, "test_key");
        mController.setParentFragment(mFragment);
    }

    @Test
    public void getAvailabilityStatus_notInstantApp_shouldReturnDisabled() {
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
                (InstantAppDataProvider) (i -> false));

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.DISABLED_FOR_USER);
    }

    @Test
    public void getAvailabilityStatus_isInstantApp_shouldReturnAvailable() {
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
                (InstantAppDataProvider) (i -> true));

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void updateState_shouldSetPreferenceTitle() {
        final String[] domain = {"Domain1"};
        final ArraySet<String> domains = new ArraySet<>();
        domains.add(domain[0]);
        final List<IntentFilterVerificationInfo> infoList = new ArrayList<>();
        final IntentFilterVerificationInfo info =
                new IntentFilterVerificationInfo("Package1", domains);
        infoList.add(info);

        when(mPackageManager.getIntentFilterVerifications("Package1")).thenReturn(infoList);

        mController.updateState(mPreference);

        verify(mPreference).setTitles(domain);
    }
}
