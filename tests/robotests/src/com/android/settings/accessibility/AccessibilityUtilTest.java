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

import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.QUICK_SETTINGS;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.SOFTWARE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.TRIPLETAP;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.TWOFINGER_DOUBLETAP;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.Flags;

import androidx.test.core.app.ApplicationProvider;

import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType;
import com.android.internal.accessibility.util.ShortcutUtils;
import com.android.settings.R;
import com.android.settings.testutils.AccessibilityTestUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Set;
import java.util.StringJoiner;

@RunWith(RobolectricTestRunner.class)
public final class AccessibilityUtilTest {
    private static final String SECURE_TEST_KEY = "secure_test_key";
    private static final String MOCK_PACKAGE_NAME = "com.mock.example";
    private static final String MOCK_CLASS_NAME = MOCK_PACKAGE_NAME + ".mock_a11y_service";
    private static final String MOCK_CLASS_NAME2 = MOCK_PACKAGE_NAME + ".mock_a11y_service2";
    private static final ComponentName MOCK_COMPONENT_NAME = new ComponentName(MOCK_PACKAGE_NAME,
            MOCK_CLASS_NAME);
    private static final ComponentName MOCK_COMPONENT_NAME2 = new ComponentName(MOCK_PACKAGE_NAME,
            MOCK_CLASS_NAME2);
    private static final String SOFTWARE_SHORTCUT_KEY =
            Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS;
    private static final String HARDWARE_SHORTCUT_KEY =
            Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE;
    private static final String QUICK_SETTINGS_SHORTCUT_KEY =
            Settings.Secure.ACCESSIBILITY_QS_TARGETS;

    private static final String PLACEHOLDER_SETTING_FEATURE = "placeholderSettingFeature";
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
    public void hasValueInSettings_putValue_hasValue() {
        setShortcut(SOFTWARE, MOCK_COMPONENT_NAME.flattenToString());

        assertThat(AccessibilityUtil.hasValueInSettings(mContext, SOFTWARE,
                MOCK_COMPONENT_NAME)).isTrue();
    }

    @Test
    public void getUserShortcutTypeFromSettings_putOneValue_hasValue() {
        setShortcut(SOFTWARE, MOCK_COMPONENT_NAME.flattenToString());

        final int shortcutTypes = AccessibilityUtil.getUserShortcutTypesFromSettings(mContext,
                MOCK_COMPONENT_NAME);

        assertThat(shortcutTypes).isEqualTo(
                SOFTWARE
        );
    }

    @Test
    public void getUserShortcutTypeFromSettings_putTwoValues_hasValue() {
        setShortcut(SOFTWARE, MOCK_COMPONENT_NAME.flattenToString());
        setShortcut(HARDWARE, MOCK_COMPONENT_NAME.flattenToString());

        final int shortcutTypes = AccessibilityUtil.getUserShortcutTypesFromSettings(mContext,
                MOCK_COMPONENT_NAME);

        assertThat(shortcutTypes).isEqualTo(
                SOFTWARE
                        | HARDWARE
        );
    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void getUserShortcutTypeFromSettings_threeShortcutTypesChosen() {
        setShortcut(SOFTWARE, MOCK_COMPONENT_NAME.flattenToString());
        setShortcut(HARDWARE, MOCK_COMPONENT_NAME.flattenToString());
        setShortcut(QUICK_SETTINGS, MOCK_COMPONENT_NAME.flattenToString());

        final int shortcutTypes = AccessibilityUtil.getUserShortcutTypesFromSettings(mContext,
                MOCK_COMPONENT_NAME);

        assertThat(shortcutTypes).isEqualTo(
                SOFTWARE
                        | HARDWARE
                        | QUICK_SETTINGS
        );
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void optInAllValuesToSettings_optInValue_haveMatchString() {
        clearShortcuts();
        int shortcutTypes = SOFTWARE | HARDWARE;

        AccessibilityUtil.optInAllValuesToSettings(mContext, shortcutTypes, MOCK_COMPONENT_NAME);

        assertThat(getStringFromSettings(SOFTWARE_SHORTCUT_KEY)).isEqualTo(
                MOCK_COMPONENT_NAME.flattenToString());
        assertThat(getStringFromSettings(HARDWARE_SHORTCUT_KEY)).isEqualTo(
                MOCK_COMPONENT_NAME.flattenToString());

    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void optInAllValuesToSettings_optInValue_callsA11yManager() {
        AccessibilityManager a11yManager =
                AccessibilityTestUtils.setupMockAccessibilityManager(mContext);
        Set<String> shortcutTargets = Set.of(MOCK_COMPONENT_NAME.flattenToString());
        int shortcutTypes = SOFTWARE | HARDWARE
                | QUICK_SETTINGS;

        AccessibilityUtil.optInAllValuesToSettings(mContext, shortcutTypes, MOCK_COMPONENT_NAME);

        verify(a11yManager).enableShortcutsForTargets(
                /* enable= */ true, shortcutTypes,
                shortcutTargets, UserHandle.myUserId());
        verifyNoMoreInteractions(a11yManager);
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void optInValueToSettings_optInValue_haveMatchString() {
        setShortcut(SOFTWARE, MOCK_COMPONENT_NAME.flattenToString());

        AccessibilityUtil.optInValueToSettings(mContext, SOFTWARE,
                MOCK_COMPONENT_NAME2);

        assertThat(getStringFromSettings(SOFTWARE_SHORTCUT_KEY)).isEqualTo(
                MOCK_COMPONENT_NAME.flattenToString() + ":"
                        + MOCK_COMPONENT_NAME2.flattenToString());
    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void optInValueToSettings_optInValue_callsA11yManager() {
        AccessibilityManager a11yManager =
                AccessibilityTestUtils.setupMockAccessibilityManager(mContext);
        Set<String> shortcutTargets = Set.of(MOCK_COMPONENT_NAME2.flattenToString());

        AccessibilityUtil.optInValueToSettings(
                mContext, HARDWARE, MOCK_COMPONENT_NAME2);

        verify(a11yManager).enableShortcutsForTargets(
                /* enable= */ true, HARDWARE,
                shortcutTargets, UserHandle.myUserId());
        verifyNoMoreInteractions(a11yManager);
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void optInValueToSettings_optInTwoValues_haveMatchString() {
        setShortcut(SOFTWARE, MOCK_COMPONENT_NAME.flattenToString());

        AccessibilityUtil.optInValueToSettings(mContext, SOFTWARE,
                MOCK_COMPONENT_NAME2);
        AccessibilityUtil.optInValueToSettings(mContext, SOFTWARE,
                MOCK_COMPONENT_NAME2);

        assertThat(getStringFromSettings(SOFTWARE_SHORTCUT_KEY)).isEqualTo(
                MOCK_COMPONENT_NAME.flattenToString() + ":"
                        + MOCK_COMPONENT_NAME2.flattenToString());
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void optOutAllValuesToSettings_optOutValue_emptyString() {
        setShortcut(SOFTWARE, MOCK_COMPONENT_NAME.flattenToString());
        setShortcut(HARDWARE, MOCK_COMPONENT_NAME.flattenToString());
        int shortcutTypes =
                SOFTWARE | HARDWARE | TRIPLETAP;

        AccessibilityUtil.optOutAllValuesFromSettings(mContext, shortcutTypes,
                MOCK_COMPONENT_NAME);

        assertThat(getStringFromSettings(SOFTWARE_SHORTCUT_KEY)).isEmpty();
        assertThat(getStringFromSettings(HARDWARE_SHORTCUT_KEY)).isEmpty();
    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void optOutAllValuesToSettings_optOutValue_callsA1yManager() {
        AccessibilityManager a11yManager =
                AccessibilityTestUtils.setupMockAccessibilityManager(mContext);
        int shortcutTypes =
                SOFTWARE | HARDWARE
                        | QUICK_SETTINGS;
        Set<String> shortcutTargets = Set.of(MOCK_COMPONENT_NAME.flattenToString());

        AccessibilityUtil.optOutAllValuesFromSettings(mContext, shortcutTypes,
                MOCK_COMPONENT_NAME);

        verify(a11yManager).enableShortcutsForTargets(
                /* enable= */ false,
                shortcutTypes,
                shortcutTargets, UserHandle.myUserId());
        verifyNoMoreInteractions(a11yManager);
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void optOutValueFromSettings_optOutValue_emptyString() {
        setShortcut(SOFTWARE, MOCK_COMPONENT_NAME.flattenToString());

        AccessibilityUtil.optOutValueFromSettings(mContext, SOFTWARE,
                MOCK_COMPONENT_NAME);

        assertThat(getStringFromSettings(SOFTWARE_SHORTCUT_KEY)).isEmpty();
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void optOutValueFromSettings_optOutValue_haveMatchString() {
        setShortcut(SOFTWARE, MOCK_COMPONENT_NAME.flattenToString(),
                MOCK_COMPONENT_NAME2.flattenToString());

        AccessibilityUtil.optOutValueFromSettings(mContext, SOFTWARE,
                MOCK_COMPONENT_NAME2);

        assertThat(getStringFromSettings(SOFTWARE_SHORTCUT_KEY)).isEqualTo(
                MOCK_COMPONENT_NAME.flattenToString());
    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void optOutValueFromSettings_optOutValue_callsA11yManager() {
        AccessibilityManager a11yManager =
                AccessibilityTestUtils.setupMockAccessibilityManager(mContext);
        Set<String> shortcutTargets = Set.of(MOCK_COMPONENT_NAME.flattenToString());

        AccessibilityUtil.optOutValueFromSettings(
                mContext, QUICK_SETTINGS, MOCK_COMPONENT_NAME);

        verify(a11yManager).enableShortcutsForTargets(
                /* enable= */ false, QUICK_SETTINGS,
                shortcutTargets, UserHandle.myUserId());
        verifyNoMoreInteractions(a11yManager);
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
    @EnableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void convertKeyFromSettings_shortcutTypeMultiFingersMultiTap() {
        assertThat(AccessibilityUtil.convertKeyFromSettings(TWOFINGER_DOUBLETAP))
                .isEqualTo(
                        Settings.Secure.ACCESSIBILITY_MAGNIFICATION_TWO_FINGER_TRIPLE_TAP_ENABLED);
    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void convertKeyFromSettings_shortcutTypeQuickSettings() {
        assertThat(AccessibilityUtil.convertKeyFromSettings(QUICK_SETTINGS))
                .isEqualTo(Settings.Secure.ACCESSIBILITY_QS_TARGETS);
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

    private String getStringFromSettings(String key) {
        return Settings.Secure.getString(mContext.getContentResolver(), key);
    }

    private void setSettingsFeatureEnabled(String settingsKey, boolean enabled) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                settingsKey,
                enabled ? AccessibilityUtil.State.ON : AccessibilityUtil.State.OFF);
    }

    private void setShortcut(@UserShortcutType int shortcutType, String... componentNames) {
        StringJoiner shortcutComponents = new StringJoiner(":");
        for (String componentName : componentNames) {
            shortcutComponents.add(componentName);
        }
        Settings.Secure.putString(mContext.getContentResolver(),
                ShortcutUtils.convertToKey(shortcutType), shortcutComponents.toString());
    }

    private void clearShortcuts() {
        Settings.Secure.putString(mContext.getContentResolver(), SOFTWARE_SHORTCUT_KEY, "");
        Settings.Secure.putString(mContext.getContentResolver(), HARDWARE_SHORTCUT_KEY, "");
        Settings.Secure.putString(mContext.getContentResolver(), QUICK_SETTINGS_SHORTCUT_KEY, "");
    }
}
