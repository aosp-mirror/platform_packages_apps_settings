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
import com.android.settings.accessibility.AccessibilityUtil.UserShortcutType;

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
    private static final String DUMMY_CLASS_NAME2 = DUMMY_PACKAGE_NAME + ".dummy_a11y_service2";
    private static final ComponentName DUMMY_COMPONENT_NAME = new ComponentName(DUMMY_PACKAGE_NAME,
            DUMMY_CLASS_NAME);
    private static final ComponentName DUMMY_COMPONENT_NAME2 = new ComponentName(DUMMY_PACKAGE_NAME,
            DUMMY_CLASS_NAME2);
    private static final String SOFTWARE_SHORTCUT_KEY =
            Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS;
    private static final String HARDWARE_SHORTCUT_KEY =
            Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE;

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void capitalize_shouldReturnCapitalizedString() {
        assertThat(AccessibilityUtil.capitalize(null)).isNull();
        assertThat(AccessibilityUtil.capitalize("")).isEmpty();
        assertThat(AccessibilityUtil.capitalize("Hans")).isEqualTo("Hans");
        assertThat(AccessibilityUtil.capitalize("hans")).isEqualTo("Hans");
        assertThat(AccessibilityUtil.capitalize(",hans")).isEqualTo(",hans");
        assertThat(AccessibilityUtil.capitalize("Hans, Hans")).isEqualTo("Hans, hans");
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
    public void getAccessibilityServiceFragmentType_targetSdkQ_volumeShortcutType() {
        final AccessibilityServiceInfo info = getMockAccessibilityServiceInfo();

        info.getResolveInfo().serviceInfo.applicationInfo.targetSdkVersion = Build.VERSION_CODES.Q;
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON;

        assertThat(AccessibilityUtil.getAccessibilityServiceFragmentType(info)).isEqualTo(
                AccessibilityUtil.AccessibilityServiceFragmentType.VOLUME_SHORTCUT_TOGGLE);
    }

    @Test
    public void getAccessibilityServiceFragmentType_targetSdkR_HaveA11yButton_invisibleType() {
        final AccessibilityServiceInfo info = getMockAccessibilityServiceInfo();

        info.getResolveInfo().serviceInfo.applicationInfo.targetSdkVersion = Build.VERSION_CODES.R;
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON;

        assertThat(AccessibilityUtil.getAccessibilityServiceFragmentType(info)).isEqualTo(
                AccessibilityUtil.AccessibilityServiceFragmentType.INVISIBLE_TOGGLE);
    }

    @Test
    public void getAccessibilityServiceFragmentType_targetSdkR_NoA11yButton_toggleType() {
        final AccessibilityServiceInfo info = getMockAccessibilityServiceInfo();

        info.getResolveInfo().serviceInfo.applicationInfo.targetSdkVersion = Build.VERSION_CODES.R;
        info.flags |= ~AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON;

        assertThat(AccessibilityUtil.getAccessibilityServiceFragmentType(info)).isEqualTo(
                AccessibilityUtil.AccessibilityServiceFragmentType.TOGGLE);
    }

    @Test
    public void hasValueInSettings_putValue_hasValue() {
        putStringIntoSettings(SOFTWARE_SHORTCUT_KEY, DUMMY_COMPONENT_NAME.flattenToString());

        assertThat(AccessibilityUtil.hasValueInSettings(mContext, UserShortcutType.SOFTWARE,
                DUMMY_COMPONENT_NAME)).isTrue();
    }

    @Test
    public void getUserShortcutTypeFromSettings_putOneValue_hasValue() {
        putStringIntoSettings(SOFTWARE_SHORTCUT_KEY, DUMMY_COMPONENT_NAME.flattenToString());

        final int shortcutType = AccessibilityUtil.getUserShortcutTypesFromSettings(mContext,
                DUMMY_COMPONENT_NAME);
        assertThat(
                (shortcutType & UserShortcutType.SOFTWARE) == UserShortcutType.SOFTWARE).isTrue();
    }

    @Test
    public void getUserShortcutTypeFromSettings_putTwoValues_hasValue() {
        putStringIntoSettings(SOFTWARE_SHORTCUT_KEY, DUMMY_COMPONENT_NAME.flattenToString());
        putStringIntoSettings(HARDWARE_SHORTCUT_KEY, DUMMY_COMPONENT_NAME.flattenToString());

        final int shortcutType = AccessibilityUtil.getUserShortcutTypesFromSettings(mContext,
                DUMMY_COMPONENT_NAME);
        assertThat(
                (shortcutType & UserShortcutType.SOFTWARE) == UserShortcutType.SOFTWARE).isTrue();
        assertThat(
                (shortcutType & UserShortcutType.HARDWARE) == UserShortcutType.HARDWARE).isTrue();
    }

    @Test
    public void optInAllValuesToSettings_optInValue_haveMatchString() {
        int shortcutTypes = UserShortcutType.SOFTWARE | UserShortcutType.HARDWARE;

        AccessibilityUtil.optInAllValuesToSettings(mContext, shortcutTypes, DUMMY_COMPONENT_NAME);

        assertThat(getStringFromSettings(SOFTWARE_SHORTCUT_KEY)).isEqualTo(
                DUMMY_COMPONENT_NAME.flattenToString());
        assertThat(getStringFromSettings(HARDWARE_SHORTCUT_KEY)).isEqualTo(
                DUMMY_COMPONENT_NAME.flattenToString());

    }

    @Test
    public void optInValueToSettings_optInValue_haveMatchString() {
        putStringIntoSettings(SOFTWARE_SHORTCUT_KEY, DUMMY_COMPONENT_NAME.flattenToString());
        AccessibilityUtil.optInValueToSettings(mContext, UserShortcutType.SOFTWARE,
                DUMMY_COMPONENT_NAME2);

        assertThat(getStringFromSettings(SOFTWARE_SHORTCUT_KEY)).isEqualTo(
                DUMMY_COMPONENT_NAME.flattenToString() + ":"
                        + DUMMY_COMPONENT_NAME2.flattenToString());
    }

    @Test
    public void optInValueToSettings_optInTwoValues_haveMatchString() {
        putStringIntoSettings(SOFTWARE_SHORTCUT_KEY, DUMMY_COMPONENT_NAME.flattenToString());
        AccessibilityUtil.optInValueToSettings(mContext, UserShortcutType.SOFTWARE,
                DUMMY_COMPONENT_NAME2);
        AccessibilityUtil.optInValueToSettings(mContext, UserShortcutType.SOFTWARE,
                DUMMY_COMPONENT_NAME2);

        assertThat(getStringFromSettings(SOFTWARE_SHORTCUT_KEY)).isEqualTo(
                DUMMY_COMPONENT_NAME.flattenToString() + ":"
                        + DUMMY_COMPONENT_NAME2.flattenToString());
    }

    @Test
    public void optOutAllValuesToSettings_optOutValue_emptyString() {
        putStringIntoSettings(SOFTWARE_SHORTCUT_KEY, DUMMY_COMPONENT_NAME.flattenToString());
        putStringIntoSettings(HARDWARE_SHORTCUT_KEY, DUMMY_COMPONENT_NAME.flattenToString());
        int shortcutTypes =
                UserShortcutType.SOFTWARE | UserShortcutType.HARDWARE | UserShortcutType.TRIPLETAP;

        AccessibilityUtil.optOutAllValuesFromSettings(mContext, shortcutTypes,
                DUMMY_COMPONENT_NAME);

        assertThat(getStringFromSettings(SOFTWARE_SHORTCUT_KEY)).isEmpty();
        assertThat(getStringFromSettings(HARDWARE_SHORTCUT_KEY)).isEmpty();
    }

    @Test
    public void optOutValueFromSettings_optOutValue_emptyString() {
        putStringIntoSettings(SOFTWARE_SHORTCUT_KEY, DUMMY_COMPONENT_NAME.flattenToString());
        AccessibilityUtil.optOutValueFromSettings(mContext, UserShortcutType.SOFTWARE,
                DUMMY_COMPONENT_NAME);

        assertThat(getStringFromSettings(SOFTWARE_SHORTCUT_KEY)).isEmpty();
    }

    @Test
    public void optOutValueFromSettings_optOutValue_haveMatchString() {
        putStringIntoSettings(SOFTWARE_SHORTCUT_KEY, DUMMY_COMPONENT_NAME.flattenToString() + ":"
                + DUMMY_COMPONENT_NAME2.flattenToString());
        AccessibilityUtil.optOutValueFromSettings(mContext, UserShortcutType.SOFTWARE,
                DUMMY_COMPONENT_NAME2);

        assertThat(getStringFromSettings(SOFTWARE_SHORTCUT_KEY)).isEqualTo(
                DUMMY_COMPONENT_NAME.flattenToString());
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
            info.setComponentName(DUMMY_COMPONENT_NAME);
            return info;
        } catch (XmlPullParserException | IOException e) {
            // Do nothing
        }

        return null;
    }

    private void putStringIntoSettings(String key, String componentName) {
        Settings.Secure.putString(mContext.getContentResolver(), key, componentName);
    }

    private String getStringFromSettings(String key) {
        return Settings.Secure.getString(mContext.getContentResolver(), key);
    }
}
