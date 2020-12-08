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
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.AirplaneModeEnabler;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settings.slices.SliceBroadcastReceiver;
import com.android.settingslib.WirelessUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * {@link CustomSliceable} for airplane-safe networks, used by generic clients.
 */
// TODO(b/173413889): Need to update the slice to Button style.
public class AirplaneSafeNetworksSlice implements CustomSliceable,
        AirplaneModeEnabler.OnAirplaneModeChangedListener {

    private static final String TAG = "AirplaneSafeNetworksSlice";

    public static final String ACTION_INTENT_EXTRA = "action";

    /**
     * Annotation for different action of the slice.
     *
     * {@code VIEW_AIRPLANE_SAFE_NETWORKS} for action of turning on Wi-Fi.
     * {@code TURN_OFF_AIRPLANE_MODE} for action of turning off Airplane Mode.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            Action.VIEW_AIRPLANE_SAFE_NETWORKS,
            Action.TURN_OFF_AIRPLANE_MODE,
    })
    public @interface Action {
        int VIEW_AIRPLANE_SAFE_NETWORKS = 1;
        int TURN_OFF_AIRPLANE_MODE = 2;
    }

    private final Context mContext;
    private final AirplaneModeEnabler mAirplaneModeEnabler;
    private final WifiManager mWifiManager;

    public AirplaneSafeNetworksSlice(Context context) {
        mContext = context;
        mAirplaneModeEnabler = new AirplaneModeEnabler(context, this);
        mWifiManager = mContext.getSystemService(WifiManager.class);
    }

    private static void logd(String s) {
        Log.d(TAG, s);
    }

    @Override
    public Slice getSlice() {
        if (!WirelessUtils.isAirplaneModeOn(mContext)) {
            return null;
        }

        return new ListBuilder(mContext, getUri(), ListBuilder.INFINITY)
                .addRow(new RowBuilder()
                        .setTitle(getTitle())
                        .setPrimaryAction(getSliceAction()))
                .build();
    }

    @Override
    public Uri getUri() {
        return CustomSliceRegistry.AIRPLANE_SAFE_NETWORKS_SLICE_URI;
    }

    @Override
    public void onNotifyChange(Intent intent) {
        final int action = intent.getIntExtra(ACTION_INTENT_EXTRA, 0);
        if (action == Action.VIEW_AIRPLANE_SAFE_NETWORKS) {
            if (!mWifiManager.isWifiEnabled()) {
                logd("Action: turn on WiFi");
                mWifiManager.setWifiEnabled(true);
            }
        } else if (action == Action.TURN_OFF_AIRPLANE_MODE) {
            if (WirelessUtils.isAirplaneModeOn(mContext)) {
                logd("Action: turn off Airplane mode");
                mAirplaneModeEnabler.setAirplaneMode(false);
            }
        }
    }

    @Override
    public void onAirplaneModeChanged(boolean isAirplaneModeOn) {
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
                ? Action.TURN_OFF_AIRPLANE_MODE
                : Action.VIEW_AIRPLANE_SAFE_NETWORKS;
    }

    private String getTitle() {
        return mContext.getText(
                (getAction() == Action.VIEW_AIRPLANE_SAFE_NETWORKS)
                        ? R.string.view_airplane_safe_networks
                        : R.string.turn_off_airplane_mode).toString();
    }

    private SliceAction getSliceAction() {
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext,
                0 /* requestCode */, getIntent(),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        final IconCompat icon = Utils.createIconWithDrawable(new ColorDrawable(Color.TRANSPARENT));
        return SliceAction.createDeeplink(pendingIntent, icon, ListBuilder.ACTION_WITH_LABEL,
                getTitle());
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
