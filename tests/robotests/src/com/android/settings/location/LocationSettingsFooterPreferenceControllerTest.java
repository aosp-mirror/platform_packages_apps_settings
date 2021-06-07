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
package com.android.settings.location;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
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
import android.text.Html;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.FooterPreference;

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
public class LocationSettingsFooterPreferenceControllerTest {

    private static final int TEST_RES_ID = 1234;
    private static final String TEST_TEXT = "text";
    private static final String PREFERENCE_KEY = "location_footer";

    private Context mContext;
    private LocationSettingsFooterPreferenceController mController;
    private Lifecycle mLifecycle;

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private FooterPreference mFooterPreference;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private Resources mResources;

    @Before
    public void setUp() throws NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);

        LifecycleOwner lifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(lifecycleOwner);
        LocationSettings locationSettings = spy(new LocationSettings());
        when(locationSettings.getSettingsLifecycle()).thenReturn(mLifecycle);

        mController = spy(new LocationSettingsFooterPreferenceController(mContext, PREFERENCE_KEY));
        mController.init(locationSettings);

        when(mPreferenceScreen.findPreference(PREFERENCE_KEY)).thenReturn(mFooterPreference);
        when(mPackageManager.getResourcesForApplication(any(ApplicationInfo.class)))
                .thenReturn(mResources);
        when(mResources.getString(TEST_RES_ID)).thenReturn(TEST_TEXT);
        mController.displayPreference(mPreferenceScreen);
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

    /**
     * Display the footer even without the injected string.
     */
    @Test
    public void isAvailable_noSystemApp_returnsTrue() {
        final List<ResolveInfo> testResolveInfos = new ArrayList<>();
        testResolveInfos.add(
                getTestResolveInfo(/*isSystemApp*/ false, /*hasRequiredMetadata*/ true));
        when(mPackageManager.queryBroadcastReceivers(any(Intent.class), anyInt()))
                .thenReturn(testResolveInfos);
        assertThat(mController.isAvailable()).isTrue();
    }

    /**
     * Display the footer even without the injected string.
     */
    @Test
    public void isAvailable_noRequiredMetadata_returnsTrue() {
        final List<ResolveInfo> testResolveInfos = new ArrayList<>();
        testResolveInfos.add(
                getTestResolveInfo(/*isSystemApp*/ true, /*hasRequiredMetadata*/ false));
        when(mPackageManager.queryBroadcastReceivers(any(Intent.class), anyInt()))
                .thenReturn(testResolveInfos);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_setTitle() {
        final List<ResolveInfo> testResolveInfos = new ArrayList<>();
        testResolveInfos.add(
                getTestResolveInfo(/*isSystemApp*/ true, /*hasRequiredMetadata*/ true));
        when(mPackageManager.queryBroadcastReceivers(any(Intent.class), anyInt()))
                .thenReturn(testResolveInfos);
        mController.updateState(mFooterPreference);
        ArgumentCaptor<CharSequence> title = ArgumentCaptor.forClass(CharSequence.class);
        verify(mFooterPreference).setTitle(title.capture());
        assertThat(title.getValue()).isNotNull();
    }

    @Test
    public void onLocationModeChanged_off_setTitle() {
        final List<ResolveInfo> testResolveInfos = new ArrayList<>();
        testResolveInfos.add(
                getTestResolveInfo(/*isSystemApp*/ true, /*hasRequiredMetadata*/ true));
        when(mPackageManager.queryBroadcastReceivers(any(Intent.class), anyInt()))
                .thenReturn(testResolveInfos);
        mController.updateState(mFooterPreference);
        verify(mFooterPreference).setTitle(any());
        mController.onLocationModeChanged(/* mode= */ 0, /* restricted= */ false);
        ArgumentCaptor<CharSequence> title = ArgumentCaptor.forClass(CharSequence.class);
        verify(mFooterPreference, times(2)).setTitle(title.capture());

        assertThat(title.getValue().toString()).contains(
                Html.fromHtml(mContext.getString(
                        R.string.location_settings_footer_location_off)).toString());
    }

    @Test
    public void onLocationModeChanged_on_setTitle() {
        final List<ResolveInfo> testResolveInfos = new ArrayList<>();
        testResolveInfos.add(
                getTestResolveInfo(/*isSystemApp*/ true, /*hasRequiredMetadata*/ true));
        when(mPackageManager.queryBroadcastReceivers(any(Intent.class), anyInt()))
                .thenReturn(testResolveInfos);
        mController.updateState(mFooterPreference);
        verify(mFooterPreference).setTitle(any());
        mController.onLocationModeChanged(/* mode= */ 1, /* restricted= */ false);
        ArgumentCaptor<CharSequence> title = ArgumentCaptor.forClass(CharSequence.class);
        verify(mFooterPreference, times(2)).setTitle(title.capture());
        assertThat(title.getValue().toString()).doesNotContain(
                Html.fromHtml(mContext.getString(
                        R.string.location_settings_footer_location_off)).toString());
    }

    @Test
    public void updateState_notSystemApp_ignore() {
        final List<ResolveInfo> testResolveInfos = new ArrayList<>();
        testResolveInfos.add(
                getTestResolveInfo(/*isSystemApp*/ false, /*hasRequiredMetadata*/ true));
        when(mPackageManager.queryBroadcastReceivers(any(Intent.class), anyInt()))
                .thenReturn(testResolveInfos);
        mController.updateState(mFooterPreference);
        verify(mFooterPreference, never()).setTitle(anyChar());
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
