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
 * limitations under the License
 */

package com.android.settings.accessibility;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import com.android.settingslib.accessibility.AccessibilityUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowAccessibilityManager;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AccessibilitySlicePreferenceControllerTest {

    private final String PACKAGE_NAME = "com.android.settings.fake";
    private final String CLASS_NAME = "com.android.settings.fake.classname";
    private final String SERVICE_NAME = PACKAGE_NAME + "/" + CLASS_NAME;

    private Context mContext;

    private AccessibilitySlicePreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;

        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Secure.putInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1 /* on */);
        Settings.Secure.putString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                SERVICE_NAME);

        // Register the fake a11y Service
        ShadowAccessibilityManager shadowAccessibilityManager = Shadow.extract(
                RuntimeEnvironment.application.getSystemService(AccessibilityManager.class));
        shadowAccessibilityManager.setInstalledAccessibilityServiceList(getFakeServiceList());

        mController = new AccessibilitySlicePreferenceController(mContext, SERVICE_NAME);
    }

    @Test
    public void getAvailability_availableService_returnsAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailability_unknownService_returnsUnsupported() {
        AccessibilitySlicePreferenceController controller =
                new AccessibilitySlicePreferenceController(mContext, "fake_service/name");

        assertThat(controller.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void setChecked_availableService_serviceIsEnabled() {
        mController.setChecked(true);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void setNotChecked_availableService_serviceIsDisabled() {
        mController.setChecked(false);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_serviceEnabled_returnsTrue() {
        AccessibilityUtils.setAccessibilityServiceState(mContext,
                ComponentName.unflattenFromString(mController.getPreferenceKey()), true);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_serviceNotEnabled_returnsFalse() {
        AccessibilitySlicePreferenceController controller =
                new AccessibilitySlicePreferenceController(mContext, "fake_service/name");

        assertThat(controller.isChecked()).isFalse();
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalServiceName_exceptionThrown() {
        new AccessibilitySlicePreferenceController(mContext, "not_split_by_slash");
    }

    @Test
    public void isSliceable_returnTrue() {
        assertThat(mController.isSliceable()).isTrue();
    }

    @Test
    public void isPublicSlice_returnTrue() {
        assertThat(mController.isPublicSlice()).isTrue();
    }

    private List<AccessibilityServiceInfo> getFakeServiceList() {
        final List<AccessibilityServiceInfo> infoList = new ArrayList<>();

        final ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.packageName = PACKAGE_NAME;
        serviceInfo.name = CLASS_NAME;

        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = serviceInfo;

        try {
            final AccessibilityServiceInfo info = new AccessibilityServiceInfo(resolveInfo,
                    mContext);
            ComponentName componentName = new ComponentName(PACKAGE_NAME, CLASS_NAME);
            info.setComponentName(componentName);
            infoList.add(info);
        } catch (XmlPullParserException | IOException e) {

        }

        return infoList;
    }
}
