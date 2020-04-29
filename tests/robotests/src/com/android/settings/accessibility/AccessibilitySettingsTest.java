/*
 * Copyright (C) 2017 The Android Open Source Project
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.AccessibilityShortcutInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import com.android.settings.R;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.ShadowDeviceConfig;
import com.android.settingslib.RestrictedPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowAccessibilityManager;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AccessibilitySettingsTest {
    private static final String DUMMY_PACKAGE_NAME = "com.dummy.example";
    private static final String DUMMY_CLASS_NAME = DUMMY_PACKAGE_NAME + ".dummy_a11y_service";
    private static final ComponentName DUMMY_COMPONENT_NAME = new ComponentName(DUMMY_PACKAGE_NAME,
            DUMMY_CLASS_NAME);
    private static final int ON = 1;
    private static final int OFF = 0;
    private static final String EMPTY_STRING = "";
    private static final String DEFAULT_SUMMARY = "default summary";
    private static final String DEFAULT_DESCRIPTION = "default description";
    private static final String DEFAULT_LABEL = "default label";
    private static final Boolean SERVICE_ENABLED = true;
    private static final Boolean SERVICE_DISABLED = false;

    private Context mContext;
    private AccessibilitySettings mSettings;
    private ShadowAccessibilityManager mShadowAccessibilityManager;
    private AccessibilityServiceInfo mServiceInfo;
    @Mock
    private AccessibilityShortcutInfo mShortcutInfo;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mSettings = spy(new AccessibilitySettings());
        mServiceInfo = spy(getMockAccessibilityServiceInfo());
        mShadowAccessibilityManager = Shadow.extract(AccessibilityManager.getInstance(mContext));
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(new ArrayList<>());
        doReturn(mContext).when(mSettings).getContext();
    }

    @Test
    public void getNonIndexableKeys_existInXmlLayout() {
        final List<String> niks = AccessibilitySettings.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);
        final List<String> keys =
                XmlTestUtils.getKeysFromPreferenceXml(mContext, R.xml.accessibility_settings);

        assertThat(keys).containsAllIn(niks);
    }

    @Test
    @Config(shadows = {ShadowDeviceConfig.class})
    public void isRampingRingerEnabled_settingsFlagOn_Enabled() {
        Settings.Global.putInt(
                mContext.getContentResolver(), Settings.Global.APPLY_RAMPING_RINGER, ON);
        assertThat(AccessibilitySettings.isRampingRingerEnabled(mContext)).isTrue();
    }

    @Test
    @Config(shadows = {ShadowDeviceConfig.class})
    public void isRampingRingerEnabled_settingsFlagOff_Disabled() {
        Settings.Global.putInt(
                mContext.getContentResolver(), Settings.Global.APPLY_RAMPING_RINGER, OFF);
        assertThat(AccessibilitySettings.isRampingRingerEnabled(mContext)).isFalse();
    }

    @Test
    public void getServiceSummary_serviceCrash_showsStopped() {
        mServiceInfo.crashed = true;

        final CharSequence summary = AccessibilitySettings.getServiceSummary(mContext,
                mServiceInfo, SERVICE_ENABLED);

        assertThat(summary).isEqualTo(
                mContext.getString(R.string.accessibility_summary_state_stopped));
    }

    @Test
    public void getServiceSummary_invisibleToggle_shortcutDisabled_showsOffSummary() {
        setInvisibleToggleFragmentType(mServiceInfo);
        doReturn(DEFAULT_SUMMARY).when(mServiceInfo).loadSummary(any());

        final CharSequence summary = AccessibilitySettings.getServiceSummary(mContext,
                mServiceInfo, SERVICE_ENABLED);

        assertThat(summary).isEqualTo(
                mContext.getString(R.string.preference_summary_default_combination,
                        mContext.getString(R.string.accessibility_summary_shortcut_disabled),
                        DEFAULT_SUMMARY));
    }

    @Test
    public void getServiceSummary_enableService_showsEnabled() {
        doReturn(EMPTY_STRING).when(mServiceInfo).loadSummary(any());

        final CharSequence summary = AccessibilitySettings.getServiceSummary(mContext,
                mServiceInfo, SERVICE_ENABLED);

        assertThat(summary).isEqualTo(
                mContext.getString(R.string.accessibility_summary_state_enabled));
    }

    @Test
    public void getServiceSummary_disableService_showsDisabled() {
        doReturn(EMPTY_STRING).when(mServiceInfo).loadSummary(any());

        final CharSequence summary = AccessibilitySettings.getServiceSummary(mContext,
                mServiceInfo, SERVICE_DISABLED);

        assertThat(summary).isEqualTo(
                mContext.getString(R.string.accessibility_summary_state_disabled));
    }

    @Test
    public void getServiceSummary_enableServiceAndHasSummary_showsEnabledSummary() {
        final String service_enabled = mContext.getString(
                R.string.accessibility_summary_state_enabled);
        doReturn(DEFAULT_SUMMARY).when(mServiceInfo).loadSummary(any());

        final CharSequence summary = AccessibilitySettings.getServiceSummary(mContext,
                mServiceInfo, SERVICE_ENABLED);

        assertThat(summary).isEqualTo(
                mContext.getString(R.string.preference_summary_default_combination, service_enabled,
                        DEFAULT_SUMMARY));
    }

    @Test
    public void getServiceSummary_disableServiceAndHasSummary_showsCombineDisabledSummary() {
        final String service_disabled = mContext.getString(
                R.string.accessibility_summary_state_disabled);
        doReturn(DEFAULT_SUMMARY).when(mServiceInfo).loadSummary(any());

        final CharSequence summary = AccessibilitySettings.getServiceSummary(mContext,
                mServiceInfo, SERVICE_DISABLED);

        assertThat(summary).isEqualTo(
                mContext.getString(R.string.preference_summary_default_combination,
                        service_disabled, DEFAULT_SUMMARY));
    }

    @Test
    public void getServiceDescription_serviceCrash_showsStopped() {
        mServiceInfo.crashed = true;

        final CharSequence description = AccessibilitySettings.getServiceDescription(mContext,
                mServiceInfo, SERVICE_ENABLED);

        assertThat(description).isEqualTo(
                mContext.getString(R.string.accessibility_description_state_stopped));
    }

    @Test
    public void getServiceDescription_haveDescription_showsDescription() {
        doReturn(DEFAULT_DESCRIPTION).when(mServiceInfo).loadDescription(any());

        final CharSequence description = AccessibilitySettings.getServiceDescription(mContext,
                mServiceInfo, SERVICE_ENABLED);

        assertThat(description).isEqualTo(DEFAULT_DESCRIPTION);
    }

    @Test
    public void createAccessibilityServicePreferenceList_hasOneInfo_containsSameKey() {
        final String key = DUMMY_COMPONENT_NAME.flattenToString();
        final AccessibilitySettings.RestrictedPreferenceHelper helper =
                new AccessibilitySettings.RestrictedPreferenceHelper(mContext);
        final List<AccessibilityServiceInfo> infoList = new ArrayList<>(
                Collections.singletonList(mServiceInfo));

        final List<RestrictedPreference> preferenceList =
                helper.createAccessibilityServicePreferenceList(infoList);
        RestrictedPreference preference = preferenceList.get(0);

        assertThat(preference.getKey()).isEqualTo(key);
    }

    @Test
    public void createAccessibilityActivityPreferenceList_hasOneInfo_containsSameKey() {
        final String key = DUMMY_COMPONENT_NAME.flattenToString();
        final AccessibilitySettings.RestrictedPreferenceHelper helper =
                new AccessibilitySettings.RestrictedPreferenceHelper(mContext);
        setMockAccessibilityShortcutInfo(mShortcutInfo);
        final List<AccessibilityShortcutInfo> infoList = new ArrayList<>(
                Collections.singletonList(mShortcutInfo));

        final List<RestrictedPreference> preferenceList =
                helper.createAccessibilityActivityPreferenceList(infoList);
        RestrictedPreference preference = preferenceList.get(0);

        assertThat(preference.getKey()).isEqualTo(key);
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

    private void setMockAccessibilityShortcutInfo(AccessibilityShortcutInfo mockInfo) {
        final ActivityInfo activityInfo = Mockito.mock(ActivityInfo.class);
        when(mockInfo.getActivityInfo()).thenReturn(activityInfo);
        when(activityInfo.loadLabel(any())).thenReturn(DEFAULT_LABEL);
        when(mockInfo.loadSummary(any())).thenReturn(DEFAULT_SUMMARY);
        when(mockInfo.loadDescription(any())).thenReturn(DEFAULT_DESCRIPTION);
        when(mockInfo.getComponentName()).thenReturn(DUMMY_COMPONENT_NAME);
    }

    private void setInvisibleToggleFragmentType(AccessibilityServiceInfo info) {
        info.getResolveInfo().serviceInfo.applicationInfo.targetSdkVersion = Build.VERSION_CODES.R;
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON;
    }
}
