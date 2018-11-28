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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.SliceAction;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.fuelgauge.BatteryStatsHelperLoader;
import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.fuelgauge.batterytip.BatteryTipLoader;
import com.android.settings.fuelgauge.batterytip.BatteryTipPreferenceController;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settings.slices.SliceBuilderUtils;
import com.android.settingslib.utils.ThreadUtils;

import java.util.List;

public class BatteryFixSlice implements CustomSliceable {

    @VisibleForTesting
    static final String PREFS = "battery_fix_prefs";
    @VisibleForTesting
    static final String KEY_CURRENT_TIPS_TYPE = "current_tip_type";

    private static final String TAG = "BatteryFixSlice";

    private final Context mContext;

    public BatteryFixSlice(Context context) {
        mContext = context;
    }

    @Override
    public Uri getUri() {
        return CustomSliceRegistry.BATTERY_FIX_SLICE_URI;
    }

    @Override
    public Slice getSlice() {
        IconCompat icon;
        SliceAction primaryAction;
        Slice slice = null;

        // TipType.SUMMARY is battery good
        if (readBatteryTipAvailabilityCache(mContext) == BatteryTip.TipType.SUMMARY) {
            return null;
        }

        final List<BatteryTip> batteryTips = SliceBackgroundWorker.getInstance(mContext,
                this).getResults();

        if (batteryTips != null) {
            for (BatteryTip batteryTip : batteryTips) {
                if (batteryTip.getState() != BatteryTip.StateType.INVISIBLE) {
                    icon = IconCompat.createWithResource(mContext, batteryTip.getIconId());
                    primaryAction = new SliceAction(getPrimaryAction(),
                            icon,
                            batteryTip.getTitle(mContext));
                    slice = new ListBuilder(mContext, CustomSliceRegistry.BATTERY_FIX_SLICE_URI,
                            ListBuilder.INFINITY)
                            .setAccentColor(Utils.getColorAccentDefaultColor(mContext))
                            .addRow(new RowBuilder()
                                    .setTitle(batteryTip.getTitle(mContext))
                                    .setSubtitle(batteryTip.getSummary(mContext))
                                    .setPrimaryAction(primaryAction)
                                    .addEndItem(icon, ListBuilder.ICON_IMAGE))
                            .build();
                    break;
                }
            }
        } else {
            icon = IconCompat.createWithResource(mContext,
                    R.drawable.ic_battery_status_good_24dp);
            final String title = mContext.getString(R.string.power_usage_summary_title);
            primaryAction = new SliceAction(getPrimaryAction(), icon, title);
            slice = new ListBuilder(mContext, CustomSliceRegistry.BATTERY_FIX_SLICE_URI,
                    ListBuilder.INFINITY)
                    .setAccentColor(Utils.getColorAccentDefaultColor(mContext))
                    .addRow(new RowBuilder()
                            .setTitle(title)
                            .setPrimaryAction(primaryAction)
                            .addEndItem(icon, ListBuilder.ICON_IMAGE))
                    .build();
        }
        return slice;
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
                MetricsProto.MetricsEvent.SLICE)
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

    // TODO(b/114807643): we should find a better way to get current battery tip type quickly
    // Now we save battery tip type to shared preference when battery level changes
    public static void updateBatteryTipAvailabilityCache(Context context) {
        ThreadUtils.postOnBackgroundThread(() -> {
            refreshBatteryTips(context);
        });
    }

    @VisibleForTesting
    static int readBatteryTipAvailabilityCache(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        return prefs.getInt(KEY_CURRENT_TIPS_TYPE, BatteryTip.TipType.SUMMARY);
    }

    @WorkerThread
    private static List<BatteryTip> refreshBatteryTips(Context context) {
        final BatteryStatsHelperLoader statsLoader = new BatteryStatsHelperLoader(context);
        final BatteryStatsHelper statsHelper = statsLoader.loadInBackground();
        final BatteryTipLoader loader = new BatteryTipLoader(context, statsHelper);
        final List<BatteryTip> batteryTips = loader.loadInBackground();
        for (BatteryTip batteryTip : batteryTips) {
            if (batteryTip.getState() != BatteryTip.StateType.INVISIBLE) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS,
                        MODE_PRIVATE).edit();
                editor.putInt(KEY_CURRENT_TIPS_TYPE, batteryTip.getType());
                editor.apply();
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
