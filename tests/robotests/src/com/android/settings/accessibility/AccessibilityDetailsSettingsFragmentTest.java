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

package com.android.settings.accessibility;

import static com.android.internal.accessibility.AccessibilityShortcutController.ACCESSIBILITY_BUTTON_COMPONENT_NAME;
import static com.android.internal.accessibility.AccessibilityShortcutController.ACCESSIBILITY_HEARING_AIDS_COMPONENT_NAME;
import static com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_COMPONENT_NAME;

import static com.google.common.truth.Truth.assertThat;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.view.accessibility.AccessibilityManager;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.SettingsActivity;
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowAccessibilityManager;
import org.robolectric.shadows.androidx.fragment.FragmentController;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Tests for {@link AccessibilityDetailsSettingsFragment}. */
@Config(shadows = ShadowDevicePolicyManager.class)
@RunWith(RobolectricTestRunner.class)
public class AccessibilityDetailsSettingsFragmentTest {
    private static final String PACKAGE_NAME = "com.foo.bar";
    private static final String CLASS_NAME = PACKAGE_NAME + ".fake_a11y_service";
    private static final String COMPONENT_NAME = PACKAGE_NAME + "/" + CLASS_NAME;

    private Context mContext;

    private FragmentController<AccessibilityDetailsSettingsFragment> mFragmentController;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        ShadowAccessibilityManager shadowAccessibilityManager = Shadow.extract(
                AccessibilityManager.getInstance(mContext));
        shadowAccessibilityManager.setInstalledAccessibilityServiceList(getMockServiceList());
    }

    @Test
    public void onCreate_afterSuccessfullyLaunch_shouldBeFinished() {
        final Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_COMPONENT_NAME, COMPONENT_NAME);

        AccessibilityDetailsSettingsFragment fragment = startFragment(intent);

        assertThat(fragment.getActivity().isFinishing()).isTrue();
    }

    @Test
    public void onCreate_hasValidExtraComponentName_launchExpectedFragment() {
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_COMPONENT_NAME, COMPONENT_NAME);

        AccessibilityDetailsSettingsFragment fragment = startFragment(intent);

        assertStartActivityWithExpectedFragment(fragment.getActivity(),
                ToggleAccessibilityServicePreferenceFragment.class.getName());
    }

    @Test
    public void onCreate_hasInvalidExtraComponentName_launchAccessibilitySettings() {
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_COMPONENT_NAME, PACKAGE_NAME + "/.service");

        AccessibilityDetailsSettingsFragment fragment = startFragment(intent);

        assertStartActivityWithExpectedFragment(fragment.getActivity(),
                AccessibilitySettings.class.getName());
    }

    @Test
    public void onCreate_hasNoExtraComponentName_launchAccessibilitySettings() {
        AccessibilityDetailsSettingsFragment fragment = startFragment(/* intent= */ null);

        assertStartActivityWithExpectedFragment(fragment.getActivity(),
                AccessibilitySettings.class.getName());
    }

    @Test
    public void onCreate_extraComponentNameIsDisallowed_launchAccessibilitySettings() {
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_COMPONENT_NAME, COMPONENT_NAME);
        DevicePolicyManager dpm = mContext.getSystemService(
                DevicePolicyManager.class);
        ((ShadowDevicePolicyManager) Shadows.shadowOf(dpm)).setPermittedAccessibilityServices(
                ImmutableList.of());

        AccessibilityDetailsSettingsFragment fragment = startFragment(intent);

        assertStartActivityWithExpectedFragment(fragment.getActivity(),
                AccessibilitySettings.class.getName());
    }

    @Test
    public void onCreate_magnificationComponentName_launchMagnificationFragment() {
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_COMPONENT_NAME,
                MAGNIFICATION_COMPONENT_NAME.flattenToString());

        AccessibilityDetailsSettingsFragment fragment = startFragment(intent);

        assertStartActivityWithExpectedFragment(fragment.getActivity(),
                ToggleScreenMagnificationPreferenceFragment.class.getName());
    }

    @Test
    public void onCreate_accessibilityButton_launchAccessibilityButtonFragment() {
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_COMPONENT_NAME,
                ACCESSIBILITY_BUTTON_COMPONENT_NAME.flattenToString());

        AccessibilityDetailsSettingsFragment fragment = startFragment(intent);

        assertStartActivityWithExpectedFragment(fragment.getActivity(),
                AccessibilityButtonFragment.class.getName());
    }

    @Test
    public void onCreate_hearingAidsComponentName_launchAccessibilityHearingAidsFragment() {
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_COMPONENT_NAME,
                ACCESSIBILITY_HEARING_AIDS_COMPONENT_NAME.flattenToString());

        AccessibilityDetailsSettingsFragment fragment = startFragment(intent);

        assertStartActivityWithExpectedFragment(fragment.getActivity(),
                AccessibilityHearingAidsFragment.class.getName());
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {

        AccessibilityDetailsSettingsFragment fragment = startFragment(/* intent= */ null);

        assertThat(fragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.ACCESSIBILITY_DETAILS_SETTINGS);
    }

    private AccessibilityDetailsSettingsFragment startFragment(Intent intent) {
        mFragmentController = FragmentController.of(
                new AccessibilityDetailsSettingsFragment(), intent)
                .create()
                .visible();

        return mFragmentController.get();
    }

    private AccessibilityServiceInfo getMockAccessibilityServiceInfo() {
        final ApplicationInfo applicationInfo = new ApplicationInfo();
        final ServiceInfo serviceInfo = new ServiceInfo();
        applicationInfo.packageName = PACKAGE_NAME;
        serviceInfo.packageName = PACKAGE_NAME;
        serviceInfo.name = CLASS_NAME;
        serviceInfo.applicationInfo = applicationInfo;

        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = serviceInfo;

        try {
            final AccessibilityServiceInfo info = new AccessibilityServiceInfo(resolveInfo,
                    mContext);
            ComponentName componentName = ComponentName.unflattenFromString(COMPONENT_NAME);
            info.setComponentName(componentName);
            return info;
        } catch (XmlPullParserException | IOException e) {
            // Do nothing
        }

        return null;
    }

    private List<AccessibilityServiceInfo> getMockServiceList() {
        final List<AccessibilityServiceInfo> infoList = new ArrayList<>();
        infoList.add(getMockAccessibilityServiceInfo());
        return infoList;
    }

    private void assertStartActivityWithExpectedFragment(Activity activity, String fragmentName) {
        Intent intent = Shadows.shadowOf(activity).getNextStartedActivity();
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT)).isEqualTo(
                fragmentName);
    }
}
