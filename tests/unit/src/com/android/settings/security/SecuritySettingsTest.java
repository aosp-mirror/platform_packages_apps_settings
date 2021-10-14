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

import static com.android.settings.core.PreferenceXmlParserUtils.METADATA_KEY;
import static com.android.settings.core.PreferenceXmlParserUtils.MetadataFlag.FLAG_INCLUDE_PREF_SCREEN;
import static com.android.settings.core.PreferenceXmlParserUtils.MetadataFlag.FLAG_NEED_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.annotation.XmlRes;
import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.core.PreferenceXmlParserUtils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.security.trustagent.TrustAgentManager;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;


@RunWith(AndroidJUnit4.class)
public class SecuritySettingsTest {

    private Context mContext;
    private SecuritySettingsFeatureProvider mSecuritySettingsFeatureProvider;

    @Mock
    private TrustAgentManager mTrustAgentManager;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        FakeFeatureFactory mFeatureFactory = FakeFeatureFactory.setupForTest();
        mSecuritySettingsFeatureProvider = mFeatureFactory.getSecuritySettingsFeatureProvider();
        SecurityFeatureProvider mSecurityFeatureProvider =
                mFeatureFactory.getSecurityFeatureProvider();

        when(mSecurityFeatureProvider.getTrustAgentManager()).thenReturn(mTrustAgentManager);
    }

    @Test
    public void noAlternativeFragmentAvailable_pageIndexIncluded() throws Exception {
        when(mSecuritySettingsFeatureProvider.hasAlternativeSecuritySettingsFragment())
                .thenReturn(false);
        BaseSearchIndexProvider indexProvider = SecuritySettings.SEARCH_INDEX_DATA_PROVIDER;

        List<String> allXmlKeys = getAllXmlKeys(indexProvider);
        List<String> nonIndexableKeys = indexProvider.getNonIndexableKeys(mContext);
        allXmlKeys.removeAll(nonIndexableKeys);

        assertThat(allXmlKeys).isNotEmpty();
    }

    @Test
    public void alternativeFragmentAvailable_pageIndexExcluded() throws Exception {
        when(mSecuritySettingsFeatureProvider.hasAlternativeSecuritySettingsFragment())
                .thenReturn(true);
        BaseSearchIndexProvider indexProvider = SecuritySettings.SEARCH_INDEX_DATA_PROVIDER;

        List<String> allXmlKeys = getAllXmlKeys(indexProvider);
        List<String> nonIndexableKeys = indexProvider.getNonIndexableKeys(mContext);
        allXmlKeys.removeAll(nonIndexableKeys);

        assertThat(allXmlKeys).isEmpty();
    }

    private List<String> getAllXmlKeys(BaseSearchIndexProvider indexProvider) throws Exception {
        final List<SearchIndexableResource> resources = indexProvider.getXmlResourcesToIndex(
                mContext, true /* not used*/);
        if (resources == null || resources.isEmpty()) {
            return new ArrayList<>();
        }
        final List<String> keys = new ArrayList<>();
        for (SearchIndexableResource res : resources) {
            keys.addAll(getKeysFromXml(res.xmlResId));
        }
        return keys;
    }

    private List<String> getKeysFromXml(@XmlRes int xmlResId) throws Exception {
        final List<String> keys = new ArrayList<>();
        final List<Bundle> metadata = PreferenceXmlParserUtils
                .extractMetadata(mContext, xmlResId, FLAG_NEED_KEY | FLAG_INCLUDE_PREF_SCREEN);
        for (Bundle bundle : metadata) {
            keys.add(bundle.getString(METADATA_KEY));
        }
        return keys;
    }
}
