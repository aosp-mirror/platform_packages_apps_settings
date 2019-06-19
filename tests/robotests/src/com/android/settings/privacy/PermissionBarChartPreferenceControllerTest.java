/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.privacy;

import static android.Manifest.permission_group.CALENDAR;
import static android.Manifest.permission_group.CAMERA;
import static android.Manifest.permission_group.CONTACTS;
import static android.Manifest.permission_group.LOCATION;
import static android.Manifest.permission_group.MICROPHONE;
import static android.Manifest.permission_group.PHONE;
import static android.Manifest.permission_group.SMS;

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserManager;
import android.permission.RuntimePermissionUsageInfo;
import android.provider.DeviceConfig;
import android.view.accessibility.AccessibilityManager;

import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.Utils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowDeviceConfig;
import com.android.settings.testutils.shadow.ShadowPermissionControllerManager;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.widget.BarChartInfo;
import com.android.settingslib.widget.BarChartPreference;
import com.android.settingslib.widget.BarViewInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowAccessibilityManager;
import org.robolectric.shadows.androidx.fragment.FragmentController;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowDeviceConfig.class, ShadowUserManager.class,
        ShadowPermissionControllerManager.class})
public class PermissionBarChartPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private LockPatternUtils mLockPatternUtils;

    private PermissionBarChartPreferenceController mController;
    private BarChartPreference mPreference;
    private PrivacyDashboardFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final Context context = RuntimeEnvironment.application;
        final UserManager userManager = context.getSystemService(UserManager.class);
        final ShadowUserManager shadowUserManager = Shadow.extract(userManager);
        final ShadowAccessibilityManager accessibilityManager = Shadow.extract(
                AccessibilityManager.getInstance(context));
        accessibilityManager.setEnabledAccessibilityServiceList(new ArrayList<>());
        shadowUserManager.addProfile(new UserInfo(123, null, 0));
        when(FakeFeatureFactory.setupForTest().securityFeatureProvider.getLockPatternUtils(
                any(Context.class))).thenReturn(mLockPatternUtils);

        mController = spy(new PermissionBarChartPreferenceController(context, "test_key"));
        mFragment = spy(FragmentController.of(new PrivacyDashboardFragment())
                .create().start().get());
        mController.setFragment(mFragment);
        mPreference = spy(new BarChartPreference(context));
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @After
    public void tearDown() {
        ShadowDeviceConfig.reset();
    }

    @Test
    public void getAvailabilityStatus_permissionHubNotSet_shouldReturnUnsupported() {
        // We have not yet set the property to show the Permissions Hub.
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_permissionHubEnabled_shouldReturnAvailableUnsearchable() {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_PRIVACY,
                Utils.PROPERTY_PERMISSIONS_HUB_ENABLED,
                "true", true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void displayPreference_shouldInitializeBarChart() {
        mController.displayPreference(mScreen);

        verify(mPreference).initializeBarChart(any(BarChartInfo.class));
    }

    @Test
    public void displayPreference_usageInfosSet_shouldSetBarViewInfos() {
        final RuntimePermissionUsageInfo info1 =
                new RuntimePermissionUsageInfo("permission 1", 10);
        mController.mOldUsageInfos.add(info1);

        mController.displayPreference(mScreen);

        verify(mPreference).setBarViewInfos(any(BarViewInfo[].class));
        verify(mPreference).initializeBarChart(any(BarChartInfo.class));
    }

    @Test
    public void onPermissionUsageResult_differentPermissionResultSet_shouldSetBarViewInfos() {
        final List<RuntimePermissionUsageInfo> infos1 = new ArrayList<>();
        final RuntimePermissionUsageInfo info1 =
                new RuntimePermissionUsageInfo("permission 1", 10);
        infos1.add(info1);
        mController.displayPreference(mScreen);
        mController.onPermissionUsageResult(infos1);

        verify(mPreference).setBarViewInfos(any(BarViewInfo[].class));

        final List<RuntimePermissionUsageInfo> infos2 = new ArrayList<>();
        final RuntimePermissionUsageInfo info2 =
                new RuntimePermissionUsageInfo("permission 2", 20);
        infos2.add(info2);
        mController.onPermissionUsageResult(infos2);

        verify(mPreference, times(2)).setBarViewInfos(any(BarViewInfo[].class));
    }

    @Test
    public void onPermissionUsageResult_samePermissionResultSet_shouldNotSetBarViewInfos() {
        final List<RuntimePermissionUsageInfo> mInfos = new ArrayList<>();
        final RuntimePermissionUsageInfo info1 =
                new RuntimePermissionUsageInfo("permission 1", 10);
        mInfos.add(info1);
        mController.displayPreference(mScreen);
        mController.onPermissionUsageResult(mInfos);

        mController.onPermissionUsageResult(mInfos);

        verify(mPreference, times(1)).setBarViewInfos(any(BarViewInfo[].class));
    }

    @Test
    public void onStart_usageInfosNotSetAndPermissionHubEnabled_shouldShowProgressBar() {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_PRIVACY,
                Utils.PROPERTY_PERMISSIONS_HUB_ENABLED,
                "true", true);
        mController.displayPreference(mScreen);

        mController.onStart();

        assertThat(mFragment.getActivity().getActionBar().getElevation()).isZero();
        verify(mFragment).showPinnedHeader(true);
        verify(mPreference).updateLoadingState(true /* isLoading */);
    }

    @Test
    public void onStart_usageInfosSetAndPermissionHubEnabled_shouldNotUpdatePrefLoadingState() {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_PRIVACY,
                Utils.PROPERTY_PERMISSIONS_HUB_ENABLED,
                "true", true);
        final RuntimePermissionUsageInfo info1 =
                new RuntimePermissionUsageInfo("permission 1", 10);
        mController.mOldUsageInfos.add(info1);
        mController.displayPreference(mScreen);

        mController.onStart();

        assertThat(mFragment.getActivity().getActionBar().getElevation()).isZero();
        verify(mFragment).showPinnedHeader(true);
        verify(mPreference).updateLoadingState(false /* isLoading */);
    }

    @Test
    public void onStart_permissionHubDisabled_shouldNotShowProgressBar() {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_PRIVACY,
                Utils.PROPERTY_PERMISSIONS_HUB_ENABLED,
                "false", true);

        mController.onStart();

        assertThat(mFragment.getActivity().getActionBar().getElevation()).isNonZero();
        verify(mFragment, never()).showPinnedHeader(true);
        verify(mPreference, never()).updateLoadingState(true /* isLoading */);
    }

    @Test
    public void onPermissionUsageResult_shouldHideProgressBar() {
        final List<RuntimePermissionUsageInfo> infos1 = new ArrayList<>();
        final RuntimePermissionUsageInfo info1 =
                new RuntimePermissionUsageInfo("permission 1", 10);
        infos1.add(info1);
        mController.displayPreference(mScreen);

        mController.onPermissionUsageResult(infos1);

        verify(mFragment).showPinnedHeader(false);
        verify(mPreference).updateLoadingState(false /* isLoading */);
    }

    @Test
    public void onPermissionUsageResult_shouldBeSorted() {
        final List<RuntimePermissionUsageInfo> infos = new ArrayList<>();
        infos.add(new RuntimePermissionUsageInfo(PHONE, 10));
        infos.add(new RuntimePermissionUsageInfo(LOCATION, 10));
        infos.add(new RuntimePermissionUsageInfo(CAMERA, 10));
        infos.add(new RuntimePermissionUsageInfo(SMS, 1));
        infos.add(new RuntimePermissionUsageInfo(MICROPHONE, 10));
        infos.add(new RuntimePermissionUsageInfo(CONTACTS, 42));
        infos.add(new RuntimePermissionUsageInfo(CALENDAR, 10));
        mController.displayPreference(mScreen);

        mController.onPermissionUsageResult(infos);

        assertThat(infos.get(0).getName()).isEqualTo(CONTACTS);
        assertThat(infos.get(1).getName()).isEqualTo(LOCATION);
        assertThat(infos.get(2).getName()).isEqualTo(MICROPHONE);
        assertThat(infos.get(3).getName()).isEqualTo(CAMERA);
        assertThat(infos.get(4).getName()).isEqualTo(CALENDAR);
        assertThat(infos.get(5).getName()).isEqualTo(PHONE);
        assertThat(infos.get(6).getName()).isEqualTo(SMS);
    }
}
