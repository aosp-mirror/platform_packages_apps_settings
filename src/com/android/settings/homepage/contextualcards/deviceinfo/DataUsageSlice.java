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

package com.android.settings.homepage.contextualcards.deviceinfo;

import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.style.TextAppearanceSpan;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.datausage.DataUsageSummary;
import com.android.settings.datausage.DataUsageUtils;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBuilderUtils;
import com.android.settingslib.net.DataUsageController;

import java.util.concurrent.TimeUnit;

public class DataUsageSlice implements CustomSliceable {
    private static final String TAG = "DataUsageSlice";
    private static final long MILLIS_IN_A_DAY = TimeUnit.DAYS.toMillis(1);


    private final Context mContext;

    public DataUsageSlice(Context context) {
        mContext = context;
    }

    @Override
    public Uri getUri() {
        return CustomSliceRegistry.DATA_USAGE_SLICE_URI;
    }

    @Override
    public Slice getSlice() {
        final IconCompat icon = IconCompat.createWithResource(mContext,
                R.drawable.ic_settings_data_usage);
        final String title = mContext.getString(R.string.data_usage_summary_title);
        final SliceAction primaryAction = SliceAction.createDeeplink(getPrimaryAction(), icon,
                ListBuilder.ICON_IMAGE, title);
        final DataUsageController dataUsageController = new DataUsageController(mContext);
        final DataUsageController.DataUsageInfo info = dataUsageController.getDataUsageInfo();
        final ListBuilder listBuilder =
                new ListBuilder(mContext, CustomSliceRegistry.DATA_USAGE_SLICE_URI,
                        ListBuilder.INFINITY)
                        .setAccentColor(Utils.getColorAccentDefaultColor(mContext))
                        .setHeader(new ListBuilder.HeaderBuilder().setTitle(title));
        if (DataUsageUtils.hasSim(mContext)) {
            listBuilder.addRow(new ListBuilder.RowBuilder()
                    .setTitle(getDataUsageText(info))
                    .setSubtitle(getCycleTime(info))
                    .setPrimaryAction(primaryAction));
        } else {
            listBuilder.addRow(new ListBuilder.RowBuilder()
                    .setTitle(mContext.getText(R.string.no_sim_card))
                    .setPrimaryAction(primaryAction));
        }
        return listBuilder.build();
    }

    @Override
    public Intent getIntent() {
        final String screenTitle = mContext.getText(R.string.data_usage_wifi_title).toString();
        return SliceBuilderUtils.buildSearchResultPageIntent(mContext,
                DataUsageSummary.class.getName(), "" /* key */, screenTitle,
                SettingsEnums.SLICE)
                .setClassName(mContext.getPackageName(), SubSettings.class.getName())
                .setData(CustomSliceRegistry.DATA_USAGE_SLICE_URI);
    }

    private PendingIntent getPrimaryAction() {
        final Intent intent = getIntent();
        return PendingIntent.getActivity(mContext, 0  /* requestCode */, intent, 0  /* flags */);
    }

    @VisibleForTesting
    CharSequence getDataUsageText(DataUsageController.DataUsageInfo info) {
        final Formatter.BytesResult usedResult = Formatter.formatBytes(mContext.getResources(),
                info.usageLevel, Formatter.FLAG_CALCULATE_ROUNDED | Formatter.FLAG_IEC_UNITS);
        final SpannableString usageNumberText = new SpannableString(usedResult.value);
        usageNumberText.setSpan(
                new TextAppearanceSpan(mContext, android.R.style.TextAppearance_Large), 0,
                usageNumberText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return TextUtils.expandTemplate(mContext.getText(R.string.data_used_formatted),
                usageNumberText, usedResult.units);
    }

    @VisibleForTesting
    CharSequence getCycleTime(DataUsageController.DataUsageInfo info) {
        final long millisLeft = info.cycleEnd - System.currentTimeMillis();
        if (millisLeft <= 0) {
            return mContext.getString(R.string.billing_cycle_none_left);
        } else {
            final int daysLeft = (int) (millisLeft / MILLIS_IN_A_DAY);
            return daysLeft < 1 ? mContext.getString(R.string.billing_cycle_less_than_one_day_left)
                    : mContext.getResources().getQuantityString(R.plurals.billing_cycle_days_left,
                            daysLeft, daysLeft);
        }
    }

    @Override
    public void onNotifyChange(Intent intent) {

    }
}
