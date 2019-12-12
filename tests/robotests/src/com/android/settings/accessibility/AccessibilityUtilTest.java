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
import android.os.Build;
import android.provider.Settings;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

@RunWith(RobolectricTestRunner.class)
public final class AccessibilityUtilTest {
    private static final int ON = 1;
    private static final int OFF = 0;
    private static final String SECURE_TEST_KEY = "secure_test_key";
    private static final String DUMMY_PACKAGE_NAME = "com.dummy.example";
    private static final String DUMMY_CLASS_NAME = DUMMY_PACKAGE_NAME + ".dummy_a11y_service";
    private static final String DUMMY_COMPONENT_NAME = DUMMY_PACKAGE_NAME + "/" + DUMMY_CLASS_NAME;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void getSummary_hasValueAndEqualsToOne_shouldReturnOnString() {
        Settings.Secure.putInt(mContext.getContentResolver(), SECURE_TEST_KEY, ON);

        final CharSequence result = AccessibilityUtil.getSummary(mContext, SECURE_TEST_KEY);

        assertThat(result)
                .isEqualTo(mContext.getText(R.string.accessibility_feature_state_on));
    }

    @Test
    public void getSummary_hasValueAndEqualsToZero_shouldReturnOffString() {
        Settings.Secure.putInt(mContext.getContentResolver(), SECURE_TEST_KEY, OFF);

        final CharSequence result = AccessibilityUtil.getSummary(mContext, SECURE_TEST_KEY);

        assertThat(result)
                .isEqualTo(mContext.getText(R.string.accessibility_feature_state_off));
    }

    @Test
    public void getSummary_noValue_shouldReturnOffString() {
        final CharSequence result = AccessibilityUtil.getSummary(mContext, SECURE_TEST_KEY);

        assertThat(result)
                .isEqualTo(mContext.getText(R.string.accessibility_feature_state_off));
    }

    @Test
    public void getAccessibilityServiceFragmentType_targetSdkQ_legacyType() {
        final AccessibilityServiceInfo info = getMockAccessibilityServiceInfo();

        info.getResolveInfo().serviceInfo.applicationInfo.targetSdkVersion = Build.VERSION_CODES.Q;
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON;

        assertThat(AccessibilityUtil.getAccessibilityServiceFragmentType(info)).isEqualTo(
                AccessibilityUtil.AccessibilityServiceFragmentType.LEGACY);

    }

    @Test
    public void getAccessibilityServiceFragmentType_targetSdkR_HaveA11yButton_headlessType() {
        final AccessibilityServiceInfo info = getMockAccessibilityServiceInfo();

        info.getResolveInfo().serviceInfo.applicationInfo.targetSdkVersion = Build.VERSION_CODES.R;
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON;

        assertThat(AccessibilityUtil.getAccessibilityServiceFragmentType(info)).isEqualTo(
                AccessibilityUtil.AccessibilityServiceFragmentType.HEADLESS);

    }

    @Test
    public void getAccessibilityServiceFragmentType_targetSdkR_NoA11yButton_intuitiveType() {
        final AccessibilityServiceInfo info = getMockAccessibilityServiceInfo();

        info.getResolveInfo().serviceInfo.applicationInfo.targetSdkVersion = Build.VERSION_CODES.R;
        info.flags |= ~AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON;

        assertThat(AccessibilityUtil.getAccessibilityServiceFragmentType(info)).isEqualTo(
                AccessibilityUtil.AccessibilityServiceFragmentType.INTUITIVE);

    }


    private AccessibilityServiceInfo getMockAccessibilityServiceInfo() {
        final ApplicationInfo applicationInfo = new ApplicationInfo();
        final ServiceInfo serviceInfo = new ServiceInfo();
        applicationInfo.packageName = DUMMY_PACKAGE_NAME;
        serviceInfo.packageName = DUMMY_PACKAGE_NAME;
        serviceInfo.name = DUMMY_CLASS_NAME;
        serviceInfo.applicationInfo = applicationInfo;

        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = serviceInfo;

        try {
            final AccessibilityServiceInfo info = new AccessibilityServiceInfo(resolveInfo,
                    mContext);
            final ComponentName componentName = ComponentName.unflattenFromString(
                    DUMMY_COMPONENT_NAME);
            info.setComponentName(componentName);
            return info;
        } catch (XmlPullParserException | IOException e) {
            // Do nothing
        }

        return null;
    }
}
