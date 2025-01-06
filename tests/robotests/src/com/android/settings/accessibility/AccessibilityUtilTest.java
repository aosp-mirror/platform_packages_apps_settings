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

import static android.provider.Settings.Secure.ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU;
import static android.provider.Settings.Secure.ACCESSIBILITY_BUTTON_MODE_GESTURE;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL;

import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.QUICK_SETTINGS;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.SOFTWARE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.TRIPLETAP;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.TWOFINGER_DOUBLETAP;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

@RunWith(RobolectricTestRunner.class)
public final class AccessibilityUtilTest {
    private static final String SECURE_TEST_KEY = "secure_test_key";
    private static final String MOCK_PACKAGE_NAME = "com.mock.example";
    private static final String MOCK_CLASS_NAME = MOCK_PACKAGE_NAME + ".mock_a11y_service";
    private static final String MOCK_CLASS_NAME2 = MOCK_PACKAGE_NAME + ".mock_a11y_service2";
    private static final ComponentName MOCK_COMPONENT_NAME = new ComponentName(MOCK_PACKAGE_NAME,
            MOCK_CLASS_NAME);
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
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
        setSettingsFeatureEnabled(SECURE_TEST_KEY, true);

        final CharSequence result = AccessibilityUtil.getSummary(mContext, SECURE_TEST_KEY,
                R.string.switch_on_text, R.string.switch_off_text);

        assertThat(result)
                .isEqualTo(mContext.getText(R.string.switch_on_text));
    }

    @Test
    public void getSummary_hasValueAndEqualsToZero_shouldReturnOffString() {
        setSettingsFeatureEnabled(SECURE_TEST_KEY, false);

        final CharSequence result = AccessibilityUtil.getSummary(mContext, SECURE_TEST_KEY,
                R.string.switch_on_text, R.string.switch_off_text);

        assertThat(result)
                .isEqualTo(mContext.getText(R.string.switch_off_text));
    }

    @Test
    public void getSummary_noValue_shouldReturnOffString() {
        final CharSequence result = AccessibilityUtil.getSummary(mContext, SECURE_TEST_KEY,
                R.string.switch_on_text, R.string.switch_off_text);

        assertThat(result)
                .isEqualTo(mContext.getText(R.string.switch_off_text));
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
    public void convertKeyFromSettings_shortcutTypeSoftware() {
        assertThat(AccessibilityUtil.convertKeyFromSettings(SOFTWARE))
                .isEqualTo(Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS);
    }

    @Test
    public void convertKeyFromSettings_shortcutTypeHardware() {
        assertThat(AccessibilityUtil.convertKeyFromSettings(HARDWARE))
                .isEqualTo(Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE);
    }

    @Test
    public void convertKeyFromSettings_shortcutTypeTripleTap() {
        assertThat(AccessibilityUtil.convertKeyFromSettings(TRIPLETAP))
                .isEqualTo(Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED);
    }

    @Test
    public void convertKeyFromSettings_shortcutTypeMultiFingersMultiTap() {
        assertThat(AccessibilityUtil.convertKeyFromSettings(TWOFINGER_DOUBLETAP))
                .isEqualTo(
                        Settings.Secure.ACCESSIBILITY_MAGNIFICATION_TWO_FINGER_TRIPLE_TAP_ENABLED);
    }

    @Test
    public void convertKeyFromSettings_shortcutTypeQuickSettings() {
        assertThat(AccessibilityUtil.convertKeyFromSettings(QUICK_SETTINGS))
                .isEqualTo(Settings.Secure.ACCESSIBILITY_QS_TARGETS);
    }

    @Test
    @EnableFlags(android.provider.Flags.FLAG_A11Y_STANDALONE_GESTURE_ENABLED)
    public void getSoftwareShortcutSummary_returnsSoftwareSummary() {
        assertThat(AccessibilityUtil.getSoftwareShortcutSummary(mContext)).isEqualTo(
                mContext.getText(R.string.accessibility_shortcut_edit_summary_software));
    }

    @Test
    @DisableFlags(android.provider.Flags.FLAG_A11Y_STANDALONE_GESTURE_ENABLED)
    public void getSoftwareShortcutSummary_gestureMode_floatingButton_returnsSoftwareSummary() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.NAVIGATION_MODE, NAV_BAR_MODE_GESTURAL);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_BUTTON_MODE,
                ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU);

        assertThat(AccessibilityUtil.getSoftwareShortcutSummary(mContext)).isEqualTo(
                mContext.getText(R.string.accessibility_shortcut_edit_summary_software));
    }

    @Test
    @DisableFlags(android.provider.Flags.FLAG_A11Y_STANDALONE_GESTURE_ENABLED)
    public void getSoftwareShortcutSummary_gestureMode_gesture_returnsGestureSummary() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.NAVIGATION_MODE, NAV_BAR_MODE_GESTURAL);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_BUTTON_MODE,
                ACCESSIBILITY_BUTTON_MODE_GESTURE);

        assertThat(AccessibilityUtil.getSoftwareShortcutSummary(mContext)).isEqualTo(
                mContext.getText(R.string.accessibility_shortcut_edit_summary_software_gesture));
    }

    @Test
    @DisableFlags(android.provider.Flags.FLAG_A11Y_STANDALONE_GESTURE_ENABLED)
    public void getSoftwareShortcutSummary_navBarMode_returnsSoftwareSummary() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.NAVIGATION_MODE, NAV_BAR_MODE_3BUTTON);

        assertThat(AccessibilityUtil.getSoftwareShortcutSummary(mContext)).isEqualTo(
                mContext.getText(R.string.accessibility_shortcut_edit_summary_software));
    }

    private AccessibilityServiceInfo getMockAccessibilityServiceInfo() {
        final ApplicationInfo applicationInfo = new ApplicationInfo();
        final ServiceInfo serviceInfo = new ServiceInfo();
        applicationInfo.packageName = MOCK_PACKAGE_NAME;
        serviceInfo.packageName = MOCK_PACKAGE_NAME;
        serviceInfo.name = MOCK_CLASS_NAME;
        serviceInfo.applicationInfo = applicationInfo;

        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = serviceInfo;

        try {
            final AccessibilityServiceInfo info = new AccessibilityServiceInfo(resolveInfo,
                    mContext);
            info.setComponentName(MOCK_COMPONENT_NAME);
            return info;
        } catch (XmlPullParserException | IOException e) {
            // Do nothing
        }

        return null;
    }

    private void setSettingsFeatureEnabled(String settingsKey, boolean enabled) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                settingsKey,
                enabled ? AccessibilityUtil.State.ON : AccessibilityUtil.State.OFF);
    }
}
