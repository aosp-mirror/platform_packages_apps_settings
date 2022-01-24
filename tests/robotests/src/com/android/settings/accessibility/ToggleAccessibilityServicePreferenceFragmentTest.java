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

import static com.google.common.truth.Truth.assertThat;

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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.Arrays;

/** Tests for {@link ToggleAccessibilityServicePreferenceFragment} */
@RunWith(RobolectricTestRunner.class)
public class ToggleAccessibilityServicePreferenceFragmentTest {

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

    private TestToggleAccessibilityServicePreferenceFragment mFragment;
    private PreferenceScreen mScreen;
    private Context mContext = ApplicationProvider.getApplicationContext();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceManager mPreferenceManager;

    @Before
    public void setUpTestFragment() {
        MockitoAnnotations.initMocks(this);

        mFragment = spy(new TestToggleAccessibilityServicePreferenceFragment());
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mFragment.getPreferenceManager().getContext()).thenReturn(mContext);
        when(mFragment.getContext()).thenReturn(mContext);
        mScreen = spy(new PreferenceScreen(mContext, /* attrs= */ null));
        when(mScreen.getPreferenceManager()).thenReturn(mPreferenceManager);
        doReturn(mScreen).when(mFragment).getPreferenceScreen();
    }

    @Test
    public void getTileName_noTileServiceAssigned_noMatchString() {
        final CharSequence tileName = mFragment.getTileName();
        assertThat(tileName.toString()).isEqualTo("");
    }

    @Test
    public void getTileName_hasOneTileService_haveMatchString() {
        final Intent tileProbe = new Intent(TileService.ACTION_QS_TILE);
        final ResolveInfo info = new ResolveInfo();
        info.serviceInfo = new FakeServiceInfo();
        info.serviceInfo.packageName = PLACEHOLDER_PACKAGE_NAME;
        info.serviceInfo.name = PLACEHOLDER_TILE_CLASS_NAME;
        final ShadowPackageManager shadowPackageManager =
                Shadows.shadowOf(mContext.getPackageManager());
        shadowPackageManager.setResolveInfosForIntent(tileProbe, Arrays.asList(info));

        final CharSequence tileName = mFragment.getTileName();
        assertThat(tileName.toString()).isEqualTo(PLACEHOLDER_TILE_NAME);
    }

    @Test
    public void getTileName_hasTwoTileServices_haveMatchString() {
        final Intent tileProbe = new Intent(TileService.ACTION_QS_TILE);
        final ResolveInfo info = new ResolveInfo();
        info.serviceInfo = new FakeServiceInfo();
        info.serviceInfo.packageName = PLACEHOLDER_PACKAGE_NAME;
        info.serviceInfo.name = PLACEHOLDER_TILE_CLASS_NAME;
        final ResolveInfo info2 = new ResolveInfo();
        info2.serviceInfo = new FakeServiceInfo2();
        info2.serviceInfo.packageName = PLACEHOLDER_PACKAGE_NAME;
        info2.serviceInfo.name = PLACEHOLDER_TILE_CLASS_NAME2;
        final ShadowPackageManager shadowPackageManager =
                Shadows.shadowOf(mContext.getPackageManager());
        shadowPackageManager.setResolveInfosForIntent(tileProbe, Arrays.asList(info, info2));

        final CharSequence tileName = mFragment.getTileName();
        assertThat(tileName.toString()).isEqualTo(PLACEHOLDER_TILE_NAME);
    }

    private static class FakeServiceInfo extends ServiceInfo {
        public String loadLabel(PackageManager mgr) {
            return PLACEHOLDER_TILE_NAME;
        }
    }

    private static class FakeServiceInfo2 extends ServiceInfo {
        public String loadLabel(PackageManager mgr) {
            return PLACEHOLDER_TILE_NAME2;
        }
    }

    private static class TestToggleAccessibilityServicePreferenceFragment
            extends ToggleAccessibilityServicePreferenceFragment {

        @Override
        protected ComponentName getTileComponentName() {
            return PLACEHOLDER_TILE_COMPONENT_NAME;
        }
    }
}
