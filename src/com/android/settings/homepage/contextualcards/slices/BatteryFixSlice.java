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

package com.android.settings.homepage.contextualcards.slices;

import static android.content.Context.MODE_PRIVATE;

import static com.android.settings.slices.CustomSliceRegistry.BATTERY_FIX_SLICE_URI;

import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.ArrayMap;
import android.view.View;

import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.SliceAction;

import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.fuelgauge.BatteryStatsHelperLoader;
import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.fuelgauge.batterytip.BatteryTipLoader;
import com.android.settings.fuelgauge.batterytip.BatteryTipPreferenceController;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settings.slices.SliceBuilderUtils;
import com.android.settingslib.utils.ThreadUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BatteryFixSlice implements CustomSliceable {

    @VisibleForTesting
    static final String PREFS = "battery_fix_prefs";
    @VisibleForTesting
    static final String KEY_CURRENT_TIPS_TYPE = "current_tip_type";
    static final String KEY_CURRENT_TIPS_STATE = "current_tip_state";

    // A map tracking which BatteryTip and which state of that tip is not important.
    private static final Map<Integer, List<Integer>> UNIMPORTANT_BATTERY_TIPS;

    static {
        UNIMPORTANT_BATTERY_TIPS = new ArrayMap<>();
        UNIMPORTANT_BATTERY_TIPS.put(BatteryTip.TipType.SUMMARY,
                Arrays.asList(BatteryTip.StateType.NEW, BatteryTip.StateType.HANDLED));
        UNIMPORTANT_BATTERY_TIPS.put(BatteryTip.TipType.HIGH_DEVICE_USAGE,
                Arrays.asList(BatteryTip.StateType.NEW, BatteryTip.StateType.HANDLED));
        UNIMPORTANT_BATTERY_TIPS.put(BatteryTip.TipType.BATTERY_SAVER,
                Arrays.asList(BatteryTip.StateType.HANDLED));
    }

    private static final String TAG = "BatteryFixSlice";

    private final Context mContext;

    public BatteryFixSlice(Context context) {
        mContext = context;
    }

    @Override
    public Uri getUri() {
        return BATTERY_FIX_SLICE_URI;
    }

    @Override
    public Slice getSlice() {
        final ListBuilder sliceBuilder =
                new ListBuilder(mContext, BATTERY_FIX_SLICE_URI, ListBuilder.INFINITY)
                        .setAccentColor(COLOR_NOT_TINTED);

        if (!isBatteryTipAvailableFromCache(mContext)) {
            return buildBatteryGoodSlice(sliceBuilder, true /* isError */);
        }

        final SliceBackgroundWorker worker = SliceBackgroundWorker.getInstance(getUri());
        final List<BatteryTip> batteryTips = worker != null ? worker.getResults() : null;

        if (batteryTips == null) {
            // Because we need wait slice background worker return data
            return buildBatteryGoodSlice(sliceBuilder, false /* isError */);
        }

        for (BatteryTip batteryTip : batteryTips) {
            if (batteryTip.getState() == BatteryTip.StateType.INVISIBLE) {
                continue;
            }
            final Drawable drawable = mContext.getDrawable(batteryTip.getIconId());
            final int iconTintColorId = batteryTip.getIconTintColorId();
            if (iconTintColorId != View.NO_ID) {
                drawable.setColorFilter(new PorterDuffColorFilter(
                        mContext.getResources().getColor(iconTintColorId),
                        PorterDuff.Mode.SRC_IN));
            }

            final IconCompat icon = Utils.createIconWithDrawable(drawable);
            final SliceAction primaryAction = SliceAction.createDeeplink(getPrimaryAction(),
                    icon,
                    ListBuilder.ICON_IMAGE,
                    batteryTip.getTitle(mContext));
            sliceBuilder.addRow(new RowBuilder()
                    .setTitleItem(icon, ListBuilder.ICON_IMAGE)
                    .setTitle(batteryTip.getTitle(mContext))
                    .setSubtitle(batteryTip.getSummary(mContext))
                    .setPrimaryAction(primaryAction));
            break;
        }
        return sliceBuilder.build();
    }

    @Override
    public Intent getIntent() {
        final String screenTitle = mContext.getText(R.string.power_usage_summary_title)
                .toString();
        final Uri contentUri = new Uri.Builder()
                .appendPath(BatteryTipPreferenceController.PREF_NAME).build();

        return SliceBuilderUtils.buildSearchResultPageIntent(mContext,
                PowerUsageSummary.class.getName(), BatteryTipPreferenceController.PREF_NAME,
                screenTitle,
                SettingsEnums.SLICE)
                .setClassName(mContext.getPackageName(), SubSettings.class.getName())
                .setData(contentUri);
    }

    @Override
    public void onNotifyChange(Intent intent) {
    }

    @Override
    public Class getBackgroundWorkerClass() {
        return BatteryTipWorker.class;
    }

    private PendingIntent getPrimaryAction() {
        final Intent intent = getIntent();
        return PendingIntent.getActivity(mContext, 0  /* requestCode */, intent, 0  /* flags */);
    }

    private Slice buildBatteryGoodSlice(ListBuilder sliceBuilder, boolean isError) {
        final IconCompat icon = IconCompat.createWithResource(mContext,
                R.drawable.ic_battery_status_good_24dp);
        final String title = mContext.getString(R.string.power_usage_summary_title);
        final SliceAction primaryAction = SliceAction.createDeeplink(getPrimaryAction(), icon,
                ListBuilder.ICON_IMAGE, title);
        sliceBuilder.addRow(new RowBuilder()
                .setTitleItem(icon, ListBuilder.ICON_IMAGE)
                .setTitle(title)
                .setPrimaryAction(primaryAction))
                .setIsError(isError);
        return sliceBuilder.build();
    }

    // TODO(b/114807643): we should find a better way to get current battery tip type quickly
    // Now we save battery tip type to shared preference when battery level changes
    public static void updateBatteryTipAvailabilityCache(Context context) {
        ThreadUtils.postOnBackgroundThread(() -> refreshBatteryTips(context));
    }


    @VisibleForTesting
    static boolean isBatteryTipAvailableFromCache(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(PREFS, MODE_PRIVATE);

        final int type = prefs.getInt(KEY_CURRENT_TIPS_TYPE, BatteryTip.TipType.SUMMARY);
        final int state = prefs.getInt(KEY_CURRENT_TIPS_STATE, BatteryTip.StateType.INVISIBLE);
        if (state == BatteryTip.StateType.INVISIBLE) {
            // State is INVISIBLE, We should not show anything.
            return false;
        }
        final boolean unimportant = UNIMPORTANT_BATTERY_TIPS.containsKey(type)
                && UNIMPORTANT_BATTERY_TIPS.get(type).contains(state);
        return !unimportant;
    }

    @WorkerThread
    @VisibleForTesting
    static List<BatteryTip> refreshBatteryTips(Context context) {
        final BatteryStatsHelperLoader statsLoader = new BatteryStatsHelperLoader(context);
        final BatteryStatsHelper statsHelper = statsLoader.loadInBackground();
        final BatteryTipLoader loader = new BatteryTipLoader(context, statsHelper);
        final List<BatteryTip> batteryTips = loader.loadInBackground();
        for (BatteryTip batteryTip : batteryTips) {
            if (batteryTip.getState() != BatteryTip.StateType.INVISIBLE) {
                context.getSharedPreferences(PREFS, MODE_PRIVATE)
                        .edit()
                        .putInt(KEY_CURRENT_TIPS_TYPE, batteryTip.getType())
                        .putInt(KEY_CURRENT_TIPS_STATE, batteryTip.getState())
                        .apply();
                break;
            }
        }
        return batteryTips;
    }

    public static class BatteryTipWorker extends SliceBackgroundWorker<BatteryTip> {

        private final Context mContext;

        public BatteryTipWorker(Context context, Uri uri) {
            super(context, uri);
            mContext = context;
        }

        @Override
        protected void onSlicePinned() {
            ThreadUtils.postOnBackgroundThread(() -> {
                final List<BatteryTip> batteryTips = refreshBatteryTips(mContext);
                updateResults(batteryTips);
            });
        }

        @Override
        protected void onSliceUnpinned() {
        }

        @Override
        public void close() {
        }
    }
}
