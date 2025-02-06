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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.service.quicksettings.TileService;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPackageManager;

/** Tests for {@link LaunchAccessibilityActivityPreferenceFragment} */
@RunWith(RobolectricTestRunner.class)
public class LaunchAccessibilityActivityPreferenceFragmentTest {

    private static final String PLACEHOLDER_PACKAGE_NAME = "com.placeholder.example";
    private static final String PLACEHOLDER_TILE_CLASS_NAME =
            PLACEHOLDER_PACKAGE_NAME + "tile.placeholder";
    private static final String PLACEHOLDER_TILE_CLASS_NAME2 =
            PLACEHOLDER_PACKAGE_NAME + "tile.placeholder2";
    private static final ComponentName PLACEHOLDER_TILE_COMPONENT_NAME = new ComponentName(
            PLACEHOLDER_PACKAGE_NAME, PLACEHOLDER_TILE_CLASS_NAME);
    private static final String PLACEHOLDER_TILE_NAME =
            PLACEHOLDER_PACKAGE_NAME + "tile.placeholder";
    private static final String PLACEHOLDER_TILE_NAME2 =
            PLACEHOLDER_PACKAGE_NAME + "tile.placeholder2";

    private TestLaunchAccessibilityActivityPreferenceFragment mFragment;
    private PreferenceScreen mScreen;
    private Context mContext = ApplicationProvider.getApplicationContext();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceManager mPreferenceManager;

    @Before
    public void setUpTestFragment() {
        MockitoAnnotations.initMocks(this);

        mFragment = spy(new TestLaunchAccessibilityActivityPreferenceFragment());
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mFragment.getPreferenceManager().getContext()).thenReturn(mContext);
        when(mFragment.getContext()).thenReturn(mContext);
        mScreen = spy(new PreferenceScreen(mContext, /* attrs= */ null));
        when(mScreen.getPreferenceManager()).thenReturn(mPreferenceManager);
        doReturn(mScreen).when(mFragment).getPreferenceScreen();
    }

    private void setupTileService(String packageName, String name, String tileName) {
        final Intent tileProbe = new Intent(TileService.ACTION_QS_TILE);
        final ResolveInfo info = new ResolveInfo();
        info.serviceInfo = new FakeServiceInfo(packageName, name, tileName);
        final ShadowPackageManager shadowPackageManager =
                Shadows.shadowOf(mContext.getPackageManager());
        shadowPackageManager.addResolveInfoForIntent(tileProbe, info);
    }

    private static class FakeServiceInfo extends ServiceInfo {
        private String mTileName;

        FakeServiceInfo(String packageName, String name, String tileName) {
            this.packageName = packageName;
            this.name = name;
            mTileName = tileName;
        }

        public String loadLabel(PackageManager mgr) {
            return mTileName;
        }
    }

    private static class TestLaunchAccessibilityActivityPreferenceFragment
            extends LaunchAccessibilityActivityPreferenceFragment {

        @Override
        protected ComponentName getTileComponentName() {
            return PLACEHOLDER_TILE_COMPONENT_NAME;
        }
    }
}
