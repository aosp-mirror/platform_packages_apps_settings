/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.settings.homepage.contextualcards.slices;

import static androidx.slice.builders.ListBuilder.ICON_IMAGE;

import android.annotation.ColorInt;
import android.app.PendingIntent;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.CustomSliceable;

public class DarkThemeSlice implements CustomSliceable {
    private static final String TAG = "DarkThemeSlice";
    private static final int BATTERY_LEVEL_THRESHOLD = 50;
    private static final int DELAY_TIME_EXECUTING_DARK_THEME = 200;

    // Keep the slice even Dark theme mode changed when it is on HomePage
    @VisibleForTesting
    static boolean sKeepSliceShow;
    @VisibleForTesting
    static long sActiveUiSession = -1000;

    private final Context mContext;
    private final UiModeManager mUiModeManager;

    public DarkThemeSlice(Context context) {
        mContext = context;
        mUiModeManager = context.getSystemService(UiModeManager.class);
    }

    @Override
    public Slice getSlice() {
        final long currentUiSession = FeatureFactory.getFactory(mContext)
                .getSlicesFeatureProvider().getUiSessionToken();
        if (currentUiSession != sActiveUiSession) {
            sActiveUiSession = currentUiSession;
            sKeepSliceShow = false;
        }
        if (!sKeepSliceShow && !isAvailable(mContext)) {
            return null;
        }
        sKeepSliceShow = true;
        final PendingIntent toggleAction = getBroadcastIntent(mContext);
        @ColorInt final int color = Utils.getColorAccentDefaultColor(mContext);
        final IconCompat icon =
                IconCompat.createWithResource(mContext, R.drawable.dark_theme);
        final boolean isChecked = mUiModeManager.getNightMode() == UiModeManager.MODE_NIGHT_YES;
        return new ListBuilder(mContext, CustomSliceRegistry.DARK_THEME_SLICE_URI,
                ListBuilder.INFINITY)
                .setAccentColor(color)
                .addRow(new ListBuilder.RowBuilder()
                        .setTitle(mContext.getText(R.string.dark_theme_slice_title))
                        .setTitleItem(icon, ICON_IMAGE)
                        .setSubtitle(mContext.getText(R.string.dark_theme_slice_subtitle))
                        .setPrimaryAction(
                                SliceAction.createToggle(toggleAction, null /* actionTitle */,
                                        isChecked)))
                .build();
    }

    @Override
    public Uri getUri() {
        return CustomSliceRegistry.DARK_THEME_SLICE_URI;
    }

    @Override
    public void onNotifyChange(Intent intent) {
        final boolean isChecked = intent.getBooleanExtra(android.app.slice.Slice.EXTRA_TOGGLE_STATE,
                false);
        // make toggle transition more smooth before dark theme takes effect
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            mUiModeManager.setNightMode(
                isChecked ? UiModeManager.MODE_NIGHT_YES : UiModeManager.MODE_NIGHT_NO);
        }, DELAY_TIME_EXECUTING_DARK_THEME);
    }

    @Override
    public Intent getIntent() {
        return null;
    }

    @VisibleForTesting
    boolean isAvailable(Context context) {
        // checking dark theme mode.
        if (mUiModeManager.getNightMode() == UiModeManager.MODE_NIGHT_YES) {
            return false;
        }

        // checking the current battery level
        final BatteryManager batteryManager = context.getSystemService(BatteryManager.class);
        final int level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        Log.d(TAG, "battery level=" + level);

        return level <= BATTERY_LEVEL_THRESHOLD;
    }
}
