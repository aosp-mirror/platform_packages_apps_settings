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

package com.android.settings.wallpaper;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.android.settings.SubSettings;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.wrapper.WallpaperManagerWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowActivity;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {
                WallpaperSuggestionActivityTest.ShadowWallpaperManagerWrapper.class
        })
public class WallpaperSuggestionActivityTest {

    @Mock
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;
    private ActivityController<WallpaperSuggestionActivity> mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        mController = Robolectric.buildActivity(WallpaperSuggestionActivity.class);
    }

    @Test
    public void launch_primarySuggestionActivityDoesNotExist_shouldFallback() {
        ShadowActivity activity = shadowOf(mController.setup().get());
        final Intent intent = activity.getNextStartedActivity();

        assertThat(intent.getComponent().getClassName()).isEqualTo(SubSettings.class.getName());
    }

    @Test
    public void hasWallpaperSet_no_shouldReturnFalse() {
        ShadowWallpaperManagerWrapper.setWallpaperId(0);

        assertThat(WallpaperSuggestionActivity.isSuggestionComplete(mContext))
                .isFalse();
    }

    @Test
    public void hasWallpaperSet_yes_shouldReturnTrue() {
        ShadowWallpaperManagerWrapper.setWallpaperId(100);

        assertThat(WallpaperSuggestionActivity.isSuggestionComplete(mContext))
                .isTrue();
    }

    @Implements(WallpaperManagerWrapper.class)
    public static class ShadowWallpaperManagerWrapper {

        private static int sWallpaperId;

        public static void setWallpaperId(int id) {
            sWallpaperId = id;
        }

        public static void reset() {
            sWallpaperId = 0;
        }

        @Implementation
        public int getWallpaperId(int which) {
            return sWallpaperId;
        }
    }
}
