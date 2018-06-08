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

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import com.android.settings.SubSettings;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(SettingsRobolectricTestRunner.class)
public class WallpaperSuggestionActivityTest {

    @Mock
    private Context mContext;
    @Mock
    private Resources mResources;

    private ActivityController<WallpaperSuggestionActivity> mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = Robolectric.buildActivity(WallpaperSuggestionActivity.class);
    }

    @After
    public void tearDown() {
        ShadowWallpaperManager.reset();
    }

    @Test
    public void launch_primarySuggestionActivityDoesNotExist_shouldFallback() {
        ShadowPackageManager packageManager =
                shadowOf(RuntimeEnvironment.application.getPackageManager());
        packageManager.removePackage("com.android.settings");

        ShadowActivity activity = shadowOf(mController.setup().get());
        final Intent intent = activity.getNextStartedActivity();

        assertThat(intent.getComponent().getClassName()).isEqualTo(SubSettings.class.getName());
        assertThat(intent.getFlags()).isEqualTo(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        assertThat(activity.isFinishing()).isTrue();
    }

    @Test
    public void wallpaperServiceEnabled_no_shouldReturnTrue() {
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getBoolean(com.android.internal.R.bool.config_enableWallpaperService))
                .thenReturn(false);

        assertThat(WallpaperSuggestionActivity.isSuggestionComplete(mContext)).isTrue();
    }

    @Test
    @Config(shadows = ShadowWallpaperManager.class)
    public void hasWallpaperSet_no_shouldReturnFalse() {
        ShadowWallpaperManager.setWallpaperId(0);

        assertThat(WallpaperSuggestionActivity.isSuggestionComplete(RuntimeEnvironment.application))
                .isFalse();
    }

    @Test
    @Config(shadows = ShadowWallpaperManager.class)
    public void hasWallpaperSet_yes_shouldReturnTrue() {
        ShadowWallpaperManager.setWallpaperId(100);

        assertThat(WallpaperSuggestionActivity.isSuggestionComplete(RuntimeEnvironment.application))
                .isTrue();
    }

    @Implements(WallpaperManager.class)
    public static class ShadowWallpaperManager {

        private static int sWallpaperId;

        private static void setWallpaperId(int id) {
            sWallpaperId = id;
        }

        @Resetter
        public static void reset() {
            sWallpaperId = 0;
        }

        @Implementation
        public boolean isWallpaperServiceEnabled() {
            return true;
        }

        @Implementation
        public int getWallpaperId(int which) {
            return sWallpaperId;
        }
    }
}
