/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.app.PendingIntent;
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

import androidx.annotation.IntDef;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.SliceAction;
import androidx.slice.core.SliceHints;

import com.android.settings.AirplaneModeEnabler;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settings.slices.SliceBroadcastReceiver;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * {@link CustomSliceable} for airplane-safe networks, used by generic clients.
 */
public class AirplaneSafeNetworksSlice implements CustomSliceable,
        AirplaneModeEnabler.OnAirplaneModeChangedListener {

    private static final String TAG = "AirplaneSafeNetworksSlice";

    public static final String ACTION_INTENT_EXTRA = "action";

    /**
     * Annotation for different action of the slice.
     *
     * {@code TURN_ON_NETWORKS} for action of turning on Wi-Fi networks.
     * {@code TURN_OFF_NETWORKS} for action of turning off Wi-Fi networks.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            Action.TURN_ON_NETWORKS,
            Action.TURN_OFF_NETWORKS,
    })
    public @interface Action {
        int TURN_ON_NETWORKS = 1;
        int TURN_OFF_NETWORKS = 2;
    }

    private final Context mContext;
    private final AirplaneModeEnabler mAirplaneModeEnabler;
    private final WifiManager mWifiManager;

    private boolean mIsAirplaneModeOn;

    public AirplaneSafeNetworksSlice(Context context) {
        mContext = context;
        mAirplaneModeEnabler = new AirplaneModeEnabler(context, this);
        mIsAirplaneModeOn = mAirplaneModeEnabler.isAirplaneModeOn();
        mWifiManager = mContext.getSystemService(WifiManager.class);
    }

    private static void logd(String s) {
        Log.d(TAG, s);
    }

    @Override
    public Slice getSlice() {
        final ListBuilder listBuilder = new ListBuilder(mContext, getUri(), ListBuilder.INFINITY);
        if (mIsAirplaneModeOn) {
            listBuilder.addRow(new RowBuilder()
                    .setTitle(getTitle())
                    .addEndItem(getEndIcon(), SliceHints.ICON_IMAGE)
                    .setPrimaryAction(getSliceAction()));
        }
        return listBuilder.build();
    }

    @Override
    public Uri getUri() {
        return CustomSliceRegistry.AIRPLANE_SAFE_NETWORKS_SLICE_URI;
    }

    @Override
    public void onNotifyChange(Intent intent) {
        final int action = intent.getIntExtra(ACTION_INTENT_EXTRA, 0);
        if (action == Action.TURN_ON_NETWORKS) {
            if (!mWifiManager.isWifiEnabled()) {
                logd("Action: turn on Wi-Fi networks");
                mWifiManager.setWifiEnabled(true);
            }
        } else if (action == Action.TURN_OFF_NETWORKS) {
            if (mWifiManager.isWifiEnabled()) {
                logd("Action: turn off Wi-Fi networks");
                mWifiManager.setWifiEnabled(false);
            }
        }
    }

    @Override
    public void onAirplaneModeChanged(boolean isAirplaneModeOn) {
        mIsAirplaneModeOn = isAirplaneModeOn;
        final AirplaneSafeNetworksWorker worker = SliceBackgroundWorker.getInstance(getUri());
        if (worker != null) {
            worker.updateSlice();
        }
    }

    @Override
    public Intent getIntent() {
        return new Intent(getUri().toString())
                .setData(getUri())
                .setClass(mContext, SliceBroadcastReceiver.class)
                .putExtra(ACTION_INTENT_EXTRA, getAction());
    }

    @Action
    private int getAction() {
        return mWifiManager.isWifiEnabled()
                ? Action.TURN_OFF_NETWORKS
                : Action.TURN_ON_NETWORKS;
    }

    private String getTitle() {
        return mContext.getText(
                (getAction() == Action.TURN_ON_NETWORKS)
                        ? R.string.turn_on_networks
                        : R.string.turn_off_networks).toString();
    }

    private IconCompat getEndIcon() {
        final Drawable drawable = mContext.getDrawable(
                (getAction() == Action.TURN_ON_NETWORKS) ? R.drawable.ic_airplane_safe_networks_24dp
                        : R.drawable.ic_airplanemode_active);
        if (drawable == null) {
            return Utils.createIconWithDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        drawable.setTintList(Utils.getColorAttr(mContext, android.R.attr.colorAccent));
        return Utils.createIconWithDrawable(drawable);
    }

    private SliceAction getSliceAction() {
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext,
                0 /* requestCode */, getIntent(),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        final IconCompat icon = Utils.createIconWithDrawable(new ColorDrawable(Color.TRANSPARENT));
        return SliceAction.create(pendingIntent, icon, ListBuilder.ACTION_WITH_LABEL, getTitle());
    }

    @Override
    public Class getBackgroundWorkerClass() {
        return AirplaneSafeNetworksWorker.class;
    }

    public static class AirplaneSafeNetworksWorker extends SliceBackgroundWorker {

        private final IntentFilter mIntentFilter;
        private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                    notifySliceChange();
                }
            }
        };

        public AirplaneSafeNetworksWorker(Context context, Uri uri) {
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

        public void updateSlice() {
            notifySliceChange();
        }
    }
}
