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

package com.android.settings.development.qstile;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.service.quicksettings.TileService;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DevelopmentTilePreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    private Context mContext;
    private DevelopmentTilePreferenceController mController;
    private ShadowPackageManager mShadowPackageManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mShadowPackageManager = Shadows.shadowOf(mContext.getPackageManager());
        when(mScreen.getContext()).thenReturn(mContext);

        mController = new DevelopmentTilePreferenceController(mContext);
        assertThat(mController.getPreferenceKey()).isNull();
    }

    @Test
    public void isAlwaysAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void display_hasTileService_shouldDisplay() {
        final Intent tileProbe = new Intent(TileService.ACTION_QS_TILE)
                .setPackage(mContext.getPackageName());
        final ResolveInfo info = new ResolveInfo();
        info.serviceInfo = new FakeServiceInfo();
        info.serviceInfo.name = "abc";
        info.serviceInfo.icon = R.drawable.ic_settings_24dp;
        info.serviceInfo.packageName = mContext.getPackageName();
        mShadowPackageManager.addResolveInfoForIntent(tileProbe, info);

        mController.displayPreference(mScreen);

        verify(mScreen).addPreference(any(Preference.class));
    }

    public static class FakeServiceInfo extends ServiceInfo {

        public String loadLabel(PackageManager mgr) {
            return "hi";
        }
    }

}
