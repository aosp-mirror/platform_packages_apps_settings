/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.security;

import static android.app.settings.SettingsEnums.CONTENT_PROTECTION_PREFERENCE;

import static com.android.internal.R.string.config_defaultContentProtectionService;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.ComponentName;
import android.content.Context;
import android.provider.DeviceConfig;
import android.provider.SearchIndexableResource;
import android.view.contentcapture.ContentCaptureManager;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.ShadowDashboardFragment;
import com.android.settings.testutils.shadow.ShadowDeviceConfig;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowDashboardFragment.class,
            ShadowUtils.class,
            ShadowDeviceConfig.class,
        })
public class ContentProtectionPreferenceFragmentTest {
    private static final String PACKAGE_NAME = "com.test.package";

    private static final ComponentName COMPONENT_NAME =
            new ComponentName(PACKAGE_NAME, "TestClass");

    private String mConfigDefaultContentProtectionService = COMPONENT_NAME.flattenToString();
    private ContentProtectionPreferenceFragment mFragment;
    private Context mContext;
    private PreferenceScreen mScreen;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mFragment = spy(new ContentProtectionPreferenceFragment());
        mScreen = spy(new PreferenceScreen(mContext, /* attrs= */ null));

        doReturn(mContext).when(mFragment).getContext();
        doReturn(mScreen).when(mFragment).getPreferenceScreen();
    }

    @After
    public void tearDown() {
        ShadowDeviceConfig.reset();
    }

    @Test
    public void getMetricsCategory() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(CONTENT_PROTECTION_PREFERENCE);
    }

    @Test
    public void getPreferenceScreenResId() {
        assertThat(mFragment.getPreferenceScreenResId())
                .isEqualTo(R.layout.content_protection_preference_fragment);
    }

    @Test
    public void getNonIndexableKeys_uiEnabled_existInXmlLayout() {
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_CONTENT_CAPTURE,
                ContentCaptureManager.DEVICE_CONFIG_PROPERTY_ENABLE_CONTENT_PROTECTION_RECEIVER,
                "true",
                /* makeDefault= */ false);
        doReturn(mConfigDefaultContentProtectionService)
                .when(mContext)
                .getString(config_defaultContentProtectionService);

        final List<String> nonIndexableKeys =
                ContentProtectionPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(
                        mContext);
        final List<String> allKeys =
                XmlTestUtils.getKeysFromPreferenceXml(
                        mContext, R.layout.content_protection_preference_fragment);
        final List<String> nonIndexableKeysExpected =
                List.of(
                        "content_protection_preference_top_intro",
                        "content_protection_preference_subpage_illustration",
                        "content_protection_preference_user_consent_work_profile_switch");

        assertThat(allKeys).containsAtLeastElementsIn(nonIndexableKeys);
        assertThat(nonIndexableKeys).isEqualTo(nonIndexableKeysExpected);
    }

    @Test
    public void getNonIndexableKeys_uiDisabled_notExisted() {
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_CONTENT_CAPTURE,
                ContentCaptureManager.DEVICE_CONFIG_PROPERTY_ENABLE_CONTENT_PROTECTION_RECEIVER,
                "false",
                /* makeDefault= */ false);

        final List<String> nonIndexableKeys =
                ContentProtectionPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(
                        mContext);
        final List<String> allKeys =
                XmlTestUtils.getKeysFromPreferenceXml(
                        mContext, R.layout.content_protection_preference_fragment);

        assertThat(nonIndexableKeys).isEqualTo(allKeys);
    }

    @Test
    public void searchIndexProvider_shouldIndexResource() {
        final List<SearchIndexableResource> indexRes =
                ContentProtectionPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER
                        .getXmlResourcesToIndex(mContext, /* enabled= */ true);

        assertThat(indexRes).isNotNull();
        assertThat(indexRes).isNotEmpty();
        assertThat(indexRes.get(0).xmlResId).isEqualTo(mFragment.getPreferenceScreenResId());
    }

    @Test
    public void isPageSearchEnabled_uiDisabled_returnsFalse() {
        boolean isSearchEnabled =
                mFragment.SEARCH_INDEX_DATA_PROVIDER.isPageSearchEnabled(mContext);

        assertThat(isSearchEnabled).isFalse();
    }

    @Test
    public void isPageSearchEnabled_uiEnabled_returnsTrue() {
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_CONTENT_CAPTURE,
                ContentCaptureManager.DEVICE_CONFIG_PROPERTY_ENABLE_CONTENT_PROTECTION_RECEIVER,
                "true",
                /* makeDefault= */ false);
        doReturn(mConfigDefaultContentProtectionService)
                .when(mContext)
                .getString(config_defaultContentProtectionService);

        boolean isSearchEnabled =
                mFragment.SEARCH_INDEX_DATA_PROVIDER.isPageSearchEnabled(mContext);

        assertThat(isSearchEnabled).isTrue();
    }
}
