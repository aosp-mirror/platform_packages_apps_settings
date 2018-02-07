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

package com.android.settings.wrapper;

import android.app.WallpaperManager;
import android.content.Context;

public class WallpaperManagerWrapper {

    private final WallpaperManager mWallpaperManager;
    private final boolean mWallpaperServiceEnabled;

    public WallpaperManagerWrapper(Context context) {
        mWallpaperServiceEnabled = context.getResources().getBoolean(
                com.android.internal.R.bool.config_enableWallpaperService);
        mWallpaperManager = mWallpaperServiceEnabled ? (WallpaperManager) context.getSystemService(
                Context.WALLPAPER_SERVICE) : null;
    }

    public boolean isWallpaperServiceEnabled() {
        return mWallpaperServiceEnabled;
    }

    public int getWallpaperId(int which) {
        if (!mWallpaperServiceEnabled) {
            throw new RuntimeException("This device does not have wallpaper service enabled.");
        }
        return mWallpaperManager.getWallpaperId(which);
    }
}
