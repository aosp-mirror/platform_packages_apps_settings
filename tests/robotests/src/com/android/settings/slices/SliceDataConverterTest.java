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

package com.android.settings.slices;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.accessibility.AccessibilitySlicePreferenceController;
import com.android.settings.search.SearchFeatureProvider;
import com.android.settings.search.SearchFeatureProviderImpl;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.FakeIndexProvider;
import com.android.settingslib.search.SearchIndexableData;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SliceDataConverterTest {

    private static final String FAKE_KEY = "key";
    private static final String FAKE_TITLE = "title";
    private static final String FAKE_SUMMARY = "summary";
    private static final String FAKE_SCREEN_TITLE = "screen_title";
    private static final String FAKE_FRAGMENT_CLASSNAME = FakeIndexProvider.class.getName();
    private static final String FAKE_CONTROLLER_NAME = FakePreferenceController.class.getName();
    private static final String ACCESSIBILITY_FRAGMENT = AccessibilitySettings.class.getName();
    private static final String A11Y_CONTROLLER_NAME =
            AccessibilitySlicePreferenceController.class.getName();
    private static final String FAKE_SERVICE_NAME = "fake_service";
    private static final String FAKE_ACCESSIBILITY_PACKAGE = "fake_package";
    private static final String FAKE_A11Y_SERVICE_NAME =
            FAKE_ACCESSIBILITY_PACKAGE + "/" + FAKE_SERVICE_NAME;
    private static final int FAKE_ICON = 1234;

    private Context mContext;

    private SliceDataConverter mSliceDataConverter;
    private SearchFeatureProvider mSearchFeatureProvider;
    private FakeFeatureFactory mFakeFeatureFactory;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mSliceDataConverter = spy(new SliceDataConverter(RuntimeEnvironment.application));
        mSearchFeatureProvider = new SearchFeatureProviderImpl();
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mFakeFeatureFactory.searchFeatureProvider = mSearchFeatureProvider;
    }

    @After
    public void cleanUp() {
        mFakeFeatureFactory.searchFeatureProvider = mock(SearchFeatureProvider.class);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void testFakeProvider_convertsFakeData() {
        mSearchFeatureProvider.getSearchIndexableResources().getProviderValues().clear();
        mSearchFeatureProvider.getSearchIndexableResources().getProviderValues()
                .add(new SearchIndexableData(FakeIndexProvider.class,
                        FakeIndexProvider.SEARCH_INDEX_DATA_PROVIDER));

        doReturn(getFakeService()).when(mSliceDataConverter).getAccessibilityServiceInfoList();

        List<SliceData> sliceDataList = mSliceDataConverter.getSliceData();

        assertThat(sliceDataList).hasSize(2);
        SliceData fakeSlice0 = sliceDataList.get(0);
        SliceData fakeSlice1 = sliceDataList.get(1);

        // Should not assume the order of the data list.
        if (TextUtils.equals(fakeSlice0.getKey(), FAKE_KEY)) {
            assertFakeSlice(fakeSlice0);
            assertFakeA11ySlice(fakeSlice1);
        } else {
            assertFakeSlice(fakeSlice1);
            assertFakeA11ySlice(fakeSlice0);
        }
    }

    private void assertFakeSlice(SliceData fakeSlice) {
        assertThat(fakeSlice.getKey()).isEqualTo(FAKE_KEY);
        assertThat(fakeSlice.getTitle()).isEqualTo(FAKE_TITLE);
        assertThat(fakeSlice.getSummary()).isEqualTo(FAKE_SUMMARY);
        assertThat(fakeSlice.getScreenTitle()).isEqualTo(FAKE_SCREEN_TITLE);
        assertThat(fakeSlice.getKeywords()).isNull();
        assertThat(fakeSlice.getIconResource()).isNotNull();
        assertThat(fakeSlice.getUri().toString())
                .isEqualTo("content://com.android.settings.slices/action/key");
        assertThat(fakeSlice.getFragmentClassName()).isEqualTo(FAKE_FRAGMENT_CLASSNAME);
        assertThat(fakeSlice.getPreferenceController()).isEqualTo(FAKE_CONTROLLER_NAME);
        assertThat(fakeSlice.getSliceType()).isEqualTo(SliceData.SliceType.SLIDER);
        assertThat(fakeSlice.getUnavailableSliceSubtitle()).isEqualTo(
                "subtitleOfUnavailableSlice"); // from XML
        assertThat(fakeSlice.isPublicSlice()).isTrue();
    }

    private void assertFakeA11ySlice(SliceData fakeSlice) {
        assertThat(fakeSlice.getKey()).isEqualTo(FAKE_A11Y_SERVICE_NAME);
        assertThat(fakeSlice.getTitle()).isEqualTo(FAKE_TITLE);
        assertThat(fakeSlice.getSummary()).isNull();
        assertThat(fakeSlice.getScreenTitle()).isEqualTo(
                mContext.getString(R.string.accessibility_settings));
        assertThat(fakeSlice.getIconResource()).isEqualTo(FAKE_ICON);
        assertThat(fakeSlice.getUri()).isNotNull();
        assertThat(fakeSlice.getFragmentClassName()).isEqualTo(ACCESSIBILITY_FRAGMENT);
        assertThat(fakeSlice.getPreferenceController()).isEqualTo(A11Y_CONTROLLER_NAME);
    }

    // This is fragile. Should be replaced by a proper fake Service if possible.
    private List<AccessibilityServiceInfo> getFakeService() {
        List<AccessibilityServiceInfo> serviceInfoList = new ArrayList<>();
        AccessibilityServiceInfo serviceInfo = spy(new AccessibilityServiceInfo());

        ResolveInfo resolveInfo = spy(new ResolveInfo());
        resolveInfo.serviceInfo = new ServiceInfo();
        resolveInfo.serviceInfo.name = FAKE_SERVICE_NAME;
        resolveInfo.serviceInfo.packageName = FAKE_ACCESSIBILITY_PACKAGE;
        doReturn(FAKE_TITLE).when(resolveInfo).loadLabel(any(PackageManager.class));
        doReturn(FAKE_ICON).when(resolveInfo).getIconResource();

        doReturn(resolveInfo).when(serviceInfo).getResolveInfo();
        serviceInfoList.add(serviceInfo);

        return serviceInfoList;
    }
}
