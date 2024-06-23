/*
 * Copyright (C) 2021 The Android Open Source Project
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


import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;

import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.TestUtils;
import com.android.settings.safetycenter.SafetyCenterManagerWrapper;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.ResourcesUtils;
import com.android.settingslib.drawer.CategoryKey;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class SecurityAdvancedSettingsTest {
    private static final String SCREEN_XML_RESOURCE_NAME = "security_advanced_settings";
    private static final String ALTERNATIVE_CATEGORY_KEY = "alternative_category_key";
    private static final String LEGACY_CATEGORY_KEY =
            "com.android.settings.category.ia.legacy_advanced_security";

    private Context mContext;
    private SecurityAdvancedSettings mSecurityAdvancedSettings;

    @Mock
    private SafetyCenterManagerWrapper mSafetyCenterManagerWrapper;

    @Before
    @UiThreadTest
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        SafetyCenterManagerWrapper.sInstance = mSafetyCenterManagerWrapper;
        mContext = ApplicationProvider.getApplicationContext();

        mSecurityAdvancedSettings = spy(new SecurityAdvancedSettings());
        when(mSecurityAdvancedSettings.getContext()).thenReturn(mContext);
    }

    @Test
    public void getPreferenceXml_returnsAdvancedSettings() {
        assertThat(mSecurityAdvancedSettings.getPreferenceScreenResId())
                .isEqualTo(getXmlResId(SCREEN_XML_RESOURCE_NAME));
    }

    @Test
    public void getCategoryKey_whenSafetyCenterIsEnabled_returnsSecurity() {
        when(mSafetyCenterManagerWrapper.isEnabled(any())).thenReturn(true);

        assertThat(mSecurityAdvancedSettings.getCategoryKey())
                .isEqualTo(CategoryKey.CATEGORY_SECURITY_ADVANCED_SETTINGS);
    }

    @Test
    public void getCategoryKey_whenAlternativeFragmentPresented_returnsAlternative() {
        when(mSafetyCenterManagerWrapper.isEnabled(any(Context.class))).thenReturn(false);
        setupAlternativeFragment(true, ALTERNATIVE_CATEGORY_KEY);

        assertThat(mSecurityAdvancedSettings.getCategoryKey())
                .isEqualTo(ALTERNATIVE_CATEGORY_KEY);
    }

    @Test
    public void getCategoryKey_whenNoAlternativeFragmentPresented_returnsLegacy() {
        when(mSafetyCenterManagerWrapper.isEnabled(any(Context.class))).thenReturn(false);
        setupAlternativeFragment(false, null);

        assertThat(mSecurityAdvancedSettings.getCategoryKey())
                .isEqualTo(LEGACY_CATEGORY_KEY);
    }

    @Test
    public void whenSafetyCenterIsEnabled_pageIndexExcluded() throws Exception {
        when(mSafetyCenterManagerWrapper.isEnabled(any())).thenReturn(true);
        BaseSearchIndexProvider indexProvider = SecurityAdvancedSettings.SEARCH_INDEX_DATA_PROVIDER;

        List<String> allXmlKeys = TestUtils.getAllXmlKeys(mContext, indexProvider);
        List<String> nonIndexableKeys = indexProvider.getNonIndexableKeys(mContext);
        allXmlKeys.removeAll(nonIndexableKeys);

        assertThat(allXmlKeys).isEmpty();
    }

    private int getXmlResId(String resName) {
        return ResourcesUtils.getResourcesId(mContext, "xml", resName);
    }

    private void setupAlternativeFragment(boolean hasAlternativeFragment,
            String alternativeCategoryKey) {
        final FakeFeatureFactory fakeFeatureFactory = FakeFeatureFactory.setupForTest();
        when(fakeFeatureFactory.securitySettingsFeatureProvider
                .hasAlternativeSecuritySettingsFragment())
                .thenReturn(hasAlternativeFragment);
        when(fakeFeatureFactory.securitySettingsFeatureProvider
                .getAlternativeAdvancedSettingsCategoryKey())
                .thenReturn(alternativeCategoryKey);
    }
}
