/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.SliceAction;
import androidx.slice.core.SliceHints;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settings.slices.SliceBroadcastReceiver;

/**
 * {@link CustomSliceable} for turning on Wi-Fi, used by generic clients.
 */
public class TurnOnWifiSlice implements CustomSliceable {

    private static final String TAG = "TurnOnWifiSlice";

    private final Context mContext;
    private final WifiManager mWifiManager;

    public TurnOnWifiSlice(Context context) {
        mContext = context;
        mWifiManager = mContext.getSystemService(WifiManager.class);
    }

    private static void logd(String s) {
        Log.d(TAG, s);
    }

    @Override
    public Slice getSlice() {
        if (mWifiManager.isWifiEnabled()) {
            return null;
        }
        final String title = mContext.getText(R.string.turn_on_wifi).toString();
        final SliceAction primaryAction = SliceAction.create(getBroadcastIntent(mContext),
                getEndIcon(), ListBuilder.ICON_IMAGE, title);
        final ListBuilder listBuilder = new ListBuilder(mContext, getUri(), ListBuilder.INFINITY)
                .addRow(new RowBuilder()
                        .setTitle(title)
                        .addEndItem(getEndIcon(), SliceHints.ICON_IMAGE)
                        .setPrimaryAction(primaryAction));
        return listBuilder.build();
    }

    @Override
    public Uri getUri() {
        return CustomSliceRegistry.TURN_ON_WIFI_SLICE_URI;
    }

    @Override
    public void onNotifyChange(Intent intent) {
        logd("Action: turn on Wi-Fi networks");
        mWifiManager.setWifiEnabled(true);
    }

    @Override
    public Intent getIntent() {
        return new Intent(getUri().toString())
                .setData(getUri())
                .setClass(mContext, SliceBroadcastReceiver.class);
    }

    private IconCompat getEndIcon() {
        final Drawable drawable = mContext.getDrawable(R.drawable.ic_settings_wireless);
        if (drawable == null) {
            return Utils.createIconWithDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        drawable.setTintList(Utils.getColorAttr(mContext, android.R.attr.colorAccent));
        return Utils.createIconWithDrawable(drawable);
    }

    @Override
    public Class getBackgroundWorkerClass() {
        return TurnOnWifiWorker.class;
    }

    /**
     * The Slice background worker {@link SliceBackgroundWorker} is used to listen the Wi-Fi
     * status change, and then notifies the Slice {@link Uri} to update.
     */
    public static class TurnOnWifiWorker extends SliceBackgroundWorker {

        private final IntentFilter mIntentFilter;
        private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                    notifySliceChange();
                }
            }
        };

        public TurnOnWifiWorker(Context context, Uri uri) {
            super(context, uri);
            mIntentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        }

        @Override
        protected void onSlicePinned() {
            getContext().registerReceiver(mBroadcastReceiver, mIntentFilter);
        }

        @Override
        protected void onSliceUnpinned() {
            getContext().unregisterReceiver(mBroadcastReceiver);
        }

        @Override
        public void close() {
            // Do nothing.
        }
    }
}
