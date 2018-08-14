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
 *
 */

package com.android.settings.search.indexing;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.SearchIndexableResource;

import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class PreIndexDataCollectorTest {

    private static final String AUTHORITY_ONE = "authority";
    private static final String PACKAGE_ONE = "com.android.settings";

    @Mock
    private ContentResolver mResolver;

    private Context mContext;

    private PreIndexDataCollector mDataCollector;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mResolver).when(mContext).getContentResolver();

        mDataCollector = spy(new PreIndexDataCollector(mContext));
    }

    @Test
    public void testCollectIndexableData_addsResourceData() {
        final List<ResolveInfo> providerInfo = getDummyResolveInfo();
        doReturn(true).when(mDataCollector).isWellKnownProvider(any(ResolveInfo.class));

        List<SearchIndexableResource> resources = getFakeResource();
        doReturn(resources).when(mDataCollector).getIndexablesForXmlResourceUri(
                any(Context.class), anyString(), any(Uri.class), any(String[].class));

        PreIndexData data =
            mDataCollector.collectIndexableData(providerInfo, true /* isFullIndex */);

        assertThat(data.dataToUpdate).containsAllIn(resources);
    }

    @Test
    public void testCollectIndexableData_addsRawData() {
        final List<ResolveInfo> providerInfo = getDummyResolveInfo();
        doReturn(true).when(mDataCollector).isWellKnownProvider(any(ResolveInfo.class));

        List<SearchIndexableRaw> rawData = getFakeRaw();
        doReturn(rawData).when(mDataCollector).getIndexablesForRawDataUri(any(Context.class),
                anyString(), any(Uri.class), any(String[].class));


        PreIndexData data =
            mDataCollector.collectIndexableData(providerInfo, true /* isFullIndex */);

        assertThat(data.dataToUpdate).containsAllIn(rawData);
    }

    @Test
    public void testCollectIndexableData_addsNonIndexables() {
        final List<ResolveInfo> providerInfo = getDummyResolveInfo();
        doReturn(true).when(mDataCollector).isWellKnownProvider(any(ResolveInfo.class));

        List<String> niks = getFakeNonIndexables();

        doReturn(niks).when(mDataCollector)
            .getNonIndexablesKeysFromRemoteProvider(anyString(), anyString());

        PreIndexData data = mDataCollector.collectIndexableData(providerInfo,
                true /* isFullIndex */);

        assertThat(data.nonIndexableKeys.get(AUTHORITY_ONE)).containsAllIn(niks);
    }

    private List<ResolveInfo> getDummyResolveInfo() {
        List<ResolveInfo> infoList = new ArrayList<>();
        ResolveInfo info = new ResolveInfo();
        info.providerInfo = new ProviderInfo();
        info.providerInfo.exported = true;
        info.providerInfo.authority = AUTHORITY_ONE;
        info.providerInfo.packageName = PACKAGE_ONE;
        info.providerInfo.applicationInfo = new ApplicationInfo();
        infoList.add(info);

        return infoList;
    }

    private List<SearchIndexableResource> getFakeResource() {
        List<SearchIndexableResource> resources = new ArrayList<>();
        final String BLANK = "";

        SearchIndexableResource sir = new SearchIndexableResource(mContext);
        sir.rank = 0;
        sir.xmlResId = 0;
        sir.className = BLANK;
        sir.packageName = BLANK;
        sir.iconResId = 0;
        sir.intentAction = BLANK;
        sir.intentTargetPackage = BLANK;
        sir.intentTargetClass = BLANK;
        sir.enabled = true;
        resources.add(sir);

        return resources;
    }

    private List<SearchIndexableRaw> getFakeRaw() {
        List<SearchIndexableRaw> rawData = new ArrayList<>();

        SearchIndexableRaw data = new SearchIndexableRaw(mContext);
        data.title = "bront";
        data.key = "brint";
        rawData.add(data);

        return rawData;
    }

    private List<String> getFakeNonIndexables() {
        List<String> niks = new ArrayList<>();
        niks.add("they're");
        niks.add("good");
        niks.add("dogs");
        niks.add("brent");
        return niks;
    }
}
