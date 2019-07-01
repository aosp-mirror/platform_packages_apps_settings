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

import static com.google.common.truth.Truth.assertThat;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowAccessibilityManager;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AccessibilityShortcutPreferenceControllerTest {
    private final static String PACKAGE_NAME = "com.foo.bar";
    private final static String CLASS_NAME = PACKAGE_NAME + ".fake_a11y_service";
    private final static String COMPONENT_NAME = PACKAGE_NAME + "/" + CLASS_NAME;
    private final static String SERVICE_NAME = "fake_a11y_service";
    private final static int ON = 1;
    private final static int OFF = 0;

    private Context mContext;
    private AccessibilityShortcutPreferenceController mController;
    private ShadowAccessibilityManager mShadowAccessibilityManager;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new AccessibilityShortcutPreferenceController(mContext, "shortcut_key");
        mShadowAccessibilityManager = Shadow.extract(AccessibilityManager.getInstance(mContext));
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(getMockServiceList());
    }

    @Test
    public void getAvailabilityStatus_hasInstalledA11yServices_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noInstalledServices_shouldReturnDisabledDependentSetting() {
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(new ArrayList<>());

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.DISABLED_DEPENDENT_SETTING);
    }

    @Test
    @Config(shadows = {ShadowAccessibilityShortcutPreferenceFragment.class})
    public void getSummary_enabledAndSelectedA11yServices_shouldReturnSelectedServiceName() {
        ShadowAccessibilityShortcutPreferenceFragment.setServiceName(SERVICE_NAME);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SHORTCUT_ENABLED, ON);

        assertThat(mController.getSummary()).isEqualTo(SERVICE_NAME);
    }

    @Test
    public void getSummary_enabledAndNoA11yServices_shouldReturnNoServiceInstalled() {
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(new ArrayList<>());
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SHORTCUT_ENABLED, ON);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getString(R.string.accessibility_no_services_installed));
    }

    @Test
    public void getSummary_disabledShortcut_shouldReturnOffSummary() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SHORTCUT_ENABLED, OFF);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getString(R.string.accessibility_feature_state_off));
    }

    @Implements(AccessibilityShortcutPreferenceFragment.class)
    private static class ShadowAccessibilityShortcutPreferenceFragment {
        private static String sSelectedServiceName;

        public static void setServiceName(String selectedServiceName) {
            sSelectedServiceName = selectedServiceName;
        }

        @Implementation
        protected static CharSequence getServiceName(Context context) {
            return sSelectedServiceName;
        }
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
}
