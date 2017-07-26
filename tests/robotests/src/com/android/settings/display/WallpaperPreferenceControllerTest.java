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

package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class WallpaperPreferenceControllerTest {

    private static final String WALLPAPER_PACKAGE = "TestPkg";
    private static final String WALLPAPER_CLASS = "TestCls";

    @Mock
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;

    private WallpaperPreferenceController mController;

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        when(mContext.getString(R.string.config_wallpaper_picker_package))
                .thenReturn(WALLPAPER_PACKAGE);
        when(mContext.getString(R.string.config_wallpaper_picker_class))
                .thenReturn(WALLPAPER_CLASS);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);

        mController = new WallpaperPreferenceController(mContext);
    }

    @Test
    public void isAvailable_wallpaerPickerEnabled_shouldReturnTrue() {
        final List<ResolveInfo> resolveInfos = new ArrayList<>();
        resolveInfos.add(mock(ResolveInfo.class));
        when(mPackageManager.queryIntentActivities(any(Intent.class), anyInt()))
                .thenReturn(resolveInfos);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_wallpaerPickerDisbled_shouldReturnFalseAndNoCrash() {
        when(mPackageManager.queryIntentActivities(any(Intent.class), anyInt())).thenReturn(null);

        assertThat(mController.isAvailable()).isFalse();

        final List<ResolveInfo> resolveInfos = new ArrayList<>();
        when(mPackageManager.queryIntentActivities(any(Intent.class), anyInt()))
                .thenReturn(resolveInfos);

        assertThat(mController.isAvailable()).isFalse();
        // should not crash
    }
}
