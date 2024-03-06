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

package com.android.settings.wallpaper;

import static com.google.common.truth.Truth.assertThat;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.Application;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.app.WallpaperManager.OnColorsChangedListener;
import android.content.Context;
import android.os.Handler;

import com.android.settings.FallbackHome;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;

import java.util.ArrayList;
import java.util.List;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class FallbackHomeActivityTest {

    private ActivityController<FallbackHome> mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        final Application application = RuntimeEnvironment.application;
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(application);
        ShadowApplication shadowApplication = Shadows.shadowOf(application);
        shadowApplication.setSystemService(Context.WALLPAPER_SERVICE, wallpaperManager);

        mController = Robolectric.buildActivity(FallbackHome.class);
    }

    @Ignore("b/315124270")
    @Test
    @Config(shadows = ShadowWallpaperManager.class)
    public void wallpaperColorsChangedListener_ensured_removed() {
        // onCreate adds the first color listener by WallpaperManager returning null colors
        ActivityController controller = mController.setup();
        ShadowWallpaperManager shadowManager = Shadow.extract(RuntimeEnvironment.application
                .getSystemService(WallpaperManager.class));
        assertThat(shadowManager.size()).isEqualTo(1);

        // Assert onDestroy will remove the original listener
        controller.destroy();
        assertThat(shadowManager.size()).isEqualTo(0);
    }

    @Implements(WallpaperManager.class)
    public static class ShadowWallpaperManager extends
        org.robolectric.shadows.ShadowWallpaperManager {

        private final List<OnColorsChangedListener> mListener = new ArrayList<>();

        public int size() {
            return mListener.size();
        }

        @Implementation
        protected boolean isWallpaperServiceEnabled() {
            return true;
        }

        @Implementation
        protected @Nullable WallpaperColors getWallpaperColors(int which) {
            return null;
        }

        @Implementation
        protected void addOnColorsChangedListener(@NonNull OnColorsChangedListener listener,
                @NonNull Handler handler) {
            mListener.add(listener);
        }

        @Implementation
        protected void removeOnColorsChangedListener(@NonNull OnColorsChangedListener listener) {
            mListener.remove(listener);
        }
    }
}
