/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.settings.location;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class LocationFooterPreferenceControllerTest {

    @Mock
    private PreferenceCategory mPreferenceCategory;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private Resources mResources;
    private LocationFooterPreferenceController mController;
    private static final int TEST_RES_ID = 1234;
    private static final String TEST_TEXT = "text";

    @Before
    public void setUp() throws NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        Context context = spy(RuntimeEnvironment.application);
        when(context.getPackageManager()).thenReturn(mPackageManager);
        when(mPreferenceCategory.getContext()).thenReturn(context);
        mController = spy(new LocationFooterPreferenceController(context, "key"));
        when(mPackageManager.getResourcesForApplication(any(ApplicationInfo.class)))
                .thenReturn(mResources);
        when(mResources.getString(TEST_RES_ID)).thenReturn(TEST_TEXT);
        doNothing().when(mPreferenceCategory).removeAll();
    }

    @Test
    public void isAvailable_hasValidFooter_returnsTrue() {
        final List<ResolveInfo> testResolveInfos = new ArrayList<>();
        testResolveInfos.add(
                getTestResolveInfo(/*isSystemApp*/ true, /*hasRequiredMetadata*/ true));
        when(mPackageManager.queryBroadcastReceivers(any(Intent.class), anyInt()))
                .thenReturn(testResolveInfos);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_noSystemApp_returnsFalse() {
        final List<ResolveInfo> testResolveInfos = new ArrayList<>();
        testResolveInfos.add(
                getTestResolveInfo(/*isSystemApp*/ false, /*hasRequiredMetadata*/ true));
        when(mPackageManager.queryBroadcastReceivers(any(Intent.class), anyInt()))
                .thenReturn(testResolveInfos);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_noRequiredMetadata_returnsFalse() {
        final List<ResolveInfo> testResolveInfos = new ArrayList<>();
        testResolveInfos.add(
                getTestResolveInfo(/*isSystemApp*/ true, /*hasRequiredMetadata*/ false));
        when(mPackageManager.queryBroadcastReceivers(any(Intent.class), anyInt()))
                .thenReturn(testResolveInfos);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void updateState_addPreferences() {
        final List<ResolveInfo> testResolveInfos = new ArrayList<>();
        testResolveInfos.add(
                getTestResolveInfo(/*isSystemApp*/ true, /*hasRequiredMetadata*/ true));
        when(mPackageManager.queryBroadcastReceivers(any(Intent.class), anyInt()))
                .thenReturn(testResolveInfos);
        mController.updateState(mPreferenceCategory);
        ArgumentCaptor<Preference> pref = ArgumentCaptor.forClass(Preference.class);
        verify(mPreferenceCategory).addPreference(pref.capture());
        assertThat(pref.getValue().getTitle()).isEqualTo(TEST_TEXT);
    }

    @Test
    public void updateState_notSystemApp_ignore() {
        final List<ResolveInfo> testResolveInfos = new ArrayList<>();
        testResolveInfos.add(
                getTestResolveInfo(/*isSystemApp*/ false, /*hasRequiredMetadata*/ true));
        when(mPackageManager.queryBroadcastReceivers(any(Intent.class), anyInt()))
                .thenReturn(testResolveInfos);
        mController.updateState(mPreferenceCategory);
        verify(mPreferenceCategory, never()).addPreference(any(Preference.class));
    }

    /**
     * Returns a ResolveInfo object for testing
     * @param isSystemApp If true, the application is a system app.
     * @param hasRequiredMetaData If true, the broadcast receiver has a valid value for
     *                            {@link LocationManager#METADATA_SETTINGS_FOOTER_STRING}
     */
    private ResolveInfo getTestResolveInfo(boolean isSystemApp, boolean hasRequiredMetaData) {
        ResolveInfo testResolveInfo = new ResolveInfo();
        ApplicationInfo testAppInfo = new ApplicationInfo();
        if (isSystemApp) {
            testAppInfo.flags |= ApplicationInfo.FLAG_SYSTEM;
        }
        ActivityInfo testActivityInfo = new ActivityInfo();
        testActivityInfo.name = "TestActivityName";
        testActivityInfo.packageName = "TestPackageName";
        testActivityInfo.applicationInfo = testAppInfo;
        if (hasRequiredMetaData) {
            testActivityInfo.metaData = new Bundle();
            testActivityInfo.metaData.putInt(
                    LocationManager.METADATA_SETTINGS_FOOTER_STRING, TEST_RES_ID);
        }
        testResolveInfo.activityInfo = testActivityInfo;
        return testResolveInfo;
    }
}
