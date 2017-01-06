/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.Context;
import android.content.IContentProvider;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;
import com.android.settingslib.drawer.TileUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SecurityFeatureProviderImplTest {

    private static final String MOCK_KEY = "key";
    private static final String MOCK_SUMMARY = "summary";
    private static final String URI_GET_SUMMARY = "content://package/text/summary";
    private static final String URI_GET_ICON = "content://package/icon/my_icon";

    @Mock
    private Drawable mMockDrawable;
    @Mock
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private Resources mResources;

    private SecurityFeatureProviderImpl mImpl;

    @Implements(com.android.settingslib.drawer.TileUtils.class)
    public static class TileUtilsMock {
        @Implementation
        public static int getIconFromUri(Context context, String uriString,
                Map<String, IContentProvider> providerMap) {
            return 161803;
        }

        @Implementation
        public static String getTextFromUri(Context context, String uriString,
                Map<String, IContentProvider> providerMap, String key) {
            return MOCK_SUMMARY;
        }
    }

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mImpl = new SecurityFeatureProviderImpl();
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.getResourcesForApplication(anyString())).thenReturn(mResources);
        when(mResources.getDrawable(anyInt(), any())).thenReturn(mMockDrawable);
    }

    @Test
    public void updateTilesData_shouldNotProcessEmptyScreenOrTiles() {
        mImpl.updatePreferences(mContext, null, null);
        mImpl.updatePreferences(mContext, new PreferenceScreen(mContext, null), null);
        verifyNoMoreInteractions(mPackageManager);
    }

    @Test
    public void updateTilesData_shouldNotProcessNonMatchingPreference() {
        DashboardCategory dashboardCategory = new DashboardCategory();
        dashboardCategory.addTile(new Tile());
        mImpl.updatePreferences(mContext, getPreferenceScreen(), dashboardCategory);
        verifyNoMoreInteractions(mPackageManager);
    }

    @Test
    public void updateTilesData_shouldNotProcessMatchingPreferenceWithNoData() {
        mImpl.updatePreferences(mContext, getPreferenceScreen(), getDashboardCategory());
        verifyNoMoreInteractions(mPackageManager);
    }

    @Test
    @Config(shadows = {
            TileUtilsMock.class,
    })
    public void updateTilesData_shouldUpdateMatchingPreference() {
        Bundle bundle = new Bundle();
        bundle.putString(TileUtils.META_DATA_PREFERENCE_ICON_URI, URI_GET_ICON);
        bundle.putString(TileUtils.META_DATA_PREFERENCE_SUMMARY_URI, URI_GET_SUMMARY);

        PreferenceScreen screen = getPreferenceScreen();
        DashboardCategory dashboardCategory = getDashboardCategory();
        dashboardCategory.getTile(0).intent = new Intent().setPackage("package");
        dashboardCategory.getTile(0).metaData = bundle;

        mImpl.updatePreferences(mContext, screen, dashboardCategory);
        verify(screen.findPreference(MOCK_KEY)).setIcon(mMockDrawable);
        verify(screen.findPreference(MOCK_KEY)).setSummary(MOCK_SUMMARY);
    }

    @Test
    @Config(shadows = {
            TileUtilsMock.class,
    })
    public void updateTilesData_shouldNotUpdateAlreadyUpdatedPreference() {
        Bundle bundle = new Bundle();
        bundle.putString(TileUtils.META_DATA_PREFERENCE_ICON_URI, URI_GET_ICON);
        bundle.putString(TileUtils.META_DATA_PREFERENCE_SUMMARY_URI, URI_GET_SUMMARY);

        PreferenceScreen screen = getPreferenceScreen();
        when(screen.findPreference(MOCK_KEY).getSummary()).thenReturn(MOCK_SUMMARY);
        when(screen.findPreference(MOCK_KEY).getIcon()).thenReturn(mMockDrawable);

        DashboardCategory dashboardCategory = getDashboardCategory();
        dashboardCategory.getTile(0).intent = new Intent().setPackage("package");
        dashboardCategory.getTile(0).metaData = bundle;

        mImpl.updatePreferences(mContext, screen, dashboardCategory);
        verify(screen.findPreference(MOCK_KEY), never()).setSummary(anyString());
    }

    private PreferenceScreen getPreferenceScreen() {
        final PreferenceScreen screen = mock(PreferenceScreen.class);
        final Preference pref = mock(Preference.class);
        when(screen.findPreference(MOCK_KEY)).thenReturn(pref);
        when(pref.getKey()).thenReturn(MOCK_KEY);
        return screen;
    }

    private static DashboardCategory getDashboardCategory() {
        DashboardCategory dashboardCategory = new DashboardCategory();
        Tile tile = new Tile();
        tile.key = MOCK_KEY;
        dashboardCategory.addTile(tile);
        return dashboardCategory;
    }
}
