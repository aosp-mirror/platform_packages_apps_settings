/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static java.util.Collections.singletonList;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.AccessibilityShortcutInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Test for {@link RestrictedPreferenceHelper}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowRestrictedLockUtilsInternal.class
})
public class RestrictedPreferenceHelperTest {

    private static final String PACKAGE_NAME = "com.android.test";
    private static final String CLASS_NAME = PACKAGE_NAME + ".test_a11y_service";
    private static final ComponentName COMPONENT_NAME = new ComponentName(PACKAGE_NAME, CLASS_NAME);
    private static final String DEFAULT_SUMMARY = "default summary";
    private static final String DEFAULT_DESCRIPTION = "default description";
    private static final String DEFAULT_LABEL = "default label";

    @Rule
    public final MockitoRule mocks = MockitoJUnit.rule();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Spy
    private final AccessibilityServiceInfo mServiceInfo = getMockAccessibilityServiceInfo(
            PACKAGE_NAME, CLASS_NAME);
    @Mock
    private AccessibilityShortcutInfo mShortcutInfo;
    private final RestrictedPreferenceHelper mHelper = new RestrictedPreferenceHelper(mContext);

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Test
    public void createAccessibilityServicePreferenceList_hasOneInfo_containsSameKey() {
        final String key = COMPONENT_NAME.flattenToString();
        final List<AccessibilityServiceInfo> infoList = new ArrayList<>(
                singletonList(mServiceInfo));

        final List<RestrictedPreference> preferenceList =
                mHelper.createAccessibilityServicePreferenceList(infoList);
        final RestrictedPreference preference = preferenceList.get(0);

        assertThat(preference.getKey()).isEqualTo(key);
    }

    @Test
    @RequiresFlagsEnabled(value = {android.security.Flags.FLAG_EXTEND_ECM_TO_ALL_SETTINGS,
            android.permission.flags.Flags.FLAG_ENHANCED_CONFIRMATION_MODE_APIS_ENABLED})
    public void createAccessibilityServicePreferenceList_ecmRestricted_prefIsEcmRestricted() {
        ShadowRestrictedLockUtilsInternal.setEcmRestrictedPkgs(
                mServiceInfo.getResolveInfo().serviceInfo.packageName);
        final List<AccessibilityServiceInfo> infoList = new ArrayList<>(
                singletonList(mServiceInfo));

        final List<RestrictedPreference> preferenceList =
                mHelper.createAccessibilityServicePreferenceList(infoList);
        final RestrictedPreference preference = preferenceList.get(0);

        assertThat(preference.isDisabledByEcm()).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(value = {android.security.Flags.FLAG_EXTEND_ECM_TO_ALL_SETTINGS,
            android.permission.flags.Flags.FLAG_ENHANCED_CONFIRMATION_MODE_APIS_ENABLED})
    public void createAccessibilityServicePreferenceList_ecmNotRestricted_prefIsNotEcmRestricted() {
        ShadowRestrictedLockUtilsInternal.setEcmRestrictedPkgs();
        final List<AccessibilityServiceInfo> infoList = new ArrayList<>(
                singletonList(mServiceInfo));

        final List<RestrictedPreference> preferenceList =
                mHelper.createAccessibilityServicePreferenceList(infoList);
        final RestrictedPreference preference = preferenceList.get(0);

        assertThat(preference.isDisabledByEcm()).isFalse();
    }

    @Test
    public void createAccessibilityActivityPreferenceList_hasOneInfo_containsSameKey() {
        final String key = COMPONENT_NAME.flattenToString();
        setMockAccessibilityShortcutInfo(mShortcutInfo);
        final List<AccessibilityShortcutInfo> infoList = new ArrayList<>(
                singletonList(mShortcutInfo));

        final List<RestrictedPreference> preferenceList =
                mHelper.createAccessibilityActivityPreferenceList(infoList);
        final RestrictedPreference preference = preferenceList.get(0);

        assertThat(preference.getKey()).isEqualTo(key);
    }

    private AccessibilityServiceInfo getMockAccessibilityServiceInfo(String packageName,
            String className) {
        final ApplicationInfo applicationInfo = new ApplicationInfo();
        final ServiceInfo serviceInfo = new ServiceInfo();
        applicationInfo.packageName = packageName;
        serviceInfo.packageName = packageName;
        serviceInfo.name = className;
        serviceInfo.applicationInfo = applicationInfo;

        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = serviceInfo;
        try {
            final AccessibilityServiceInfo info = new AccessibilityServiceInfo(resolveInfo,
                    mContext);
            info.setComponentName(new ComponentName(packageName, className));
            return info;
        } catch (XmlPullParserException | IOException e) {
            // Do nothing
        }
        return null;
    }

    private void setMockAccessibilityShortcutInfo(AccessibilityShortcutInfo mockInfo) {
        final ActivityInfo activityInfo = Mockito.mock(ActivityInfo.class);
        activityInfo.applicationInfo = new ApplicationInfo();
        when(mockInfo.getActivityInfo()).thenReturn(activityInfo);
        when(activityInfo.loadLabel(any())).thenReturn(DEFAULT_LABEL);
        when(mockInfo.loadSummary(any())).thenReturn(DEFAULT_SUMMARY);
        when(mockInfo.loadDescription(any())).thenReturn(DEFAULT_DESCRIPTION);
        when(mockInfo.getComponentName()).thenReturn(COMPONENT_NAME);
    }
}
