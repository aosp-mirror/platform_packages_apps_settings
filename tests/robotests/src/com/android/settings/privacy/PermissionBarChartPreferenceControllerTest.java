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
        when(mScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn((BarChartPreference) mPreference);
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
}
