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

import static android.provider.Settings.Global.LOW_POWER_MODE;

import static androidx.slice.builders.ListBuilder.ICON_IMAGE;

import android.annotation.ColorInt;
import android.app.PendingIntent;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
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
import com.android.settings.slices.SliceBackgroundWorker;

public class DarkThemeSlice implements CustomSliceable {
    private static final String TAG = "DarkThemeSlice";
    private static final boolean DEBUG = Build.IS_DEBUGGABLE;
    private static final int BATTERY_LEVEL_THRESHOLD = 50;
    private static final int DELAY_TIME_EXECUTING_DARK_THEME = 200;

    // Keep the slice even Dark theme mode changed when it is on HomePage
    @VisibleForTesting
    static boolean sKeepSliceShow;
    @VisibleForTesting
    static long sActiveUiSession = -1000;
    @VisibleForTesting
    static boolean sSliceClicked = false;
    static boolean sPreChecked = false;

    private final Context mContext;
    private final UiModeManager mUiModeManager;
    private final PowerManager mPowerManager;

    public DarkThemeSlice(Context context) {
        mContext = context;
        mUiModeManager = context.getSystemService(UiModeManager.class);
        mPowerManager = context.getSystemService(PowerManager.class);
    }

    @Override
    public Slice getSlice() {
        final long currentUiSession = FeatureFactory.getFeatureFactory()
                .getSlicesFeatureProvider().getUiSessionToken();
        if (currentUiSession != sActiveUiSession) {
            sActiveUiSession = currentUiSession;
            sKeepSliceShow = false;
        }

        // 1. Dark theme slice will disappear when battery saver is ON.
        // 2. If the slice is shown and the user doesn't toggle it directly, but instead turns on
        // Dark theme from Quick settings or display page, the card should no longer persist.
        // This card will persist when user clicks its toggle directly.
        // 3. If the slice is shown and the user toggles it on (switch to Dark theme) directly,
        // then user returns to home (launcher), no matter by the Back key or Home gesture.
        // Next time the Settings displays on screen again this card should no longer persist.
        if (DEBUG) {
            Log.d(TAG,
                    "sKeepSliceShow = " + sKeepSliceShow + ", sSliceClicked = " + sSliceClicked
                            + ", isAvailable = " + isAvailable(mContext));
        }
        if (mPowerManager.isPowerSaveMode()
                || ((!sKeepSliceShow || !sSliceClicked) && !isAvailable(mContext))) {
            return new ListBuilder(mContext, CustomSliceRegistry.DARK_THEME_SLICE_URI,
                    ListBuilder.INFINITY)
                    .setIsError(true)
                    .build();
        }
        sKeepSliceShow = true;
        final PendingIntent toggleAction = getBroadcastIntent(mContext);
        @ColorInt final int color = Utils.getColorAccentDefaultColor(mContext);
        final IconCompat icon =
                IconCompat.createWithResource(mContext, R.drawable.dark_theme);

        final boolean isChecked = Utils.isNightMode(mContext);
        if (sPreChecked != isChecked) {
            // Dark(Night) mode changed and reset the sSliceClicked.
            resetValue(isChecked, false);
        }
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
        // Dark(Night) mode changed by user clicked the toggle in the Dark theme slice.
        if (isChecked) {
            resetValue(isChecked, true);
        }
        // make toggle transition more smooth before dark theme takes effect
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            mUiModeManager.setNightModeActivated(isChecked);
        }, DELAY_TIME_EXECUTING_DARK_THEME);
    }

    @Override
    public Intent getIntent() {
        return null;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

    @Override
    public Class getBackgroundWorkerClass() {
        return DarkThemeWorker.class;
    }

    @VisibleForTesting
    boolean isAvailable(Context context) {
        // check if dark theme mode is enabled or if dark theme scheduling is on.
        if (Utils.isNightMode(context) || isNightModeScheduled()) {
            return false;
        }
        // checking the current battery level
        final BatteryManager batteryManager = context.getSystemService(BatteryManager.class);
        final int level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        Log.d(TAG, "battery level = " + level);
        return level <= BATTERY_LEVEL_THRESHOLD;
    }

    private void resetValue(boolean preChecked, boolean clicked) {
        sPreChecked = preChecked;
        sSliceClicked = clicked;
    }

    private boolean isNightModeScheduled() {
        final int mode = mUiModeManager.getNightMode();
        if (DEBUG) {
            Log.d(TAG, "night mode = " + mode);
        }
        // Turn on from sunset to sunrise or turn on at custom time
        if (mode == UiModeManager.MODE_NIGHT_AUTO || mode == UiModeManager.MODE_NIGHT_CUSTOM) {
            return true;
        }
        return false;
    }

    public static class DarkThemeWorker extends SliceBackgroundWorker<Void> {
        private final Context mContext;
        private final ContentObserver mContentObserver =
                new ContentObserver(new Handler(Looper.getMainLooper())) {
                    @Override
                    public void onChange(boolean bChanged) {
                        if (mContext.getSystemService(PowerManager.class).isPowerSaveMode()) {
                            notifySliceChange();
                        }
                    }
                };

        public DarkThemeWorker(Context context, Uri uri) {
            super(context, uri);
            mContext = context;
        }

        @Override
        protected void onSlicePinned() {
            mContext.getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(LOW_POWER_MODE), false /* notifyForDescendants */,
                    mContentObserver);
        }

        @Override
        protected void onSliceUnpinned() {
            mContext.getContentResolver().unregisterContentObserver(mContentObserver);
        }

        @Override
        public void close() {
        }
    }
}
