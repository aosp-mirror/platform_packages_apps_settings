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
import android.net.wifi.WifiManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceScreen;

import com.android.settings.AirplaneModeEnabler;
import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.widget.LayoutPreference;

/**
 * This controls the airplane mode message and click button of the "View airplane mode networks"
 * item on the Network & internet page.
 */
public class ViewAirplaneModeNetworksLayoutPreferenceController extends AbstractPreferenceController
        implements LifecycleObserver, AirplaneModeEnabler.OnAirplaneModeChangedListener,
        View.OnClickListener {

    public static final String KEY = "view_airplane_mode_netwokrs_button";

    private LayoutPreference mPreference;
    @VisibleForTesting
    TextView mTextView;
    @VisibleForTesting
    Button mButton;

    private AirplaneModeEnabler mAirplaneModeEnabler;
    private final WifiManager mWifiManager;
    private final IntentFilter mIntentFilter;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                refreshLayout();
            }
        }
    };

    public ViewAirplaneModeNetworksLayoutPreferenceController(Context context,
            Lifecycle lifecycle) {
        super(context);
        if (lifecycle == null) {
            throw new IllegalArgumentException("Lifecycle must be set");
        }
        mAirplaneModeEnabler = new AirplaneModeEnabler(context, this);
        mWifiManager = context.getSystemService(WifiManager.class);
        mIntentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        lifecycle.addObserver(this);
    }

    @Override
    public boolean isAvailable() {
        return mAirplaneModeEnabler.isAirplaneModeOn();
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (isAvailable()) {
            generateLayout();
        }
    }

    /** Lifecycle.Event.ON_START */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        mAirplaneModeEnabler.start();
    }

    /** Lifecycle.Event.ON_STOP */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        mAirplaneModeEnabler.stop();
    }

    /** Lifecycle.Event.ON_RESUME */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        mContext.registerReceiver(mBroadcastReceiver, mIntentFilter);
    }

    /** Lifecycle.Event.ON_PAUSE */
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        mContext.unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onAirplaneModeChanged(boolean isAirplaneModeOn) {
        if (mPreference != null) {
            mPreference.setVisible(isAirplaneModeOn);
        }
    }

    @Override
    public void onClick(View v) {
        mWifiManager.setWifiEnabled(true);
    }

    private void generateLayout() {
        if (mPreference == null) {
            return;
        }
        if (mTextView == null) {
            mTextView = mPreference.findViewById(R.id.airplane_mode_text);
        }
        if (mButton == null) {
            mButton = mPreference.findViewById(R.id.view_airplane_mode_networks_button);
        }
        if (mButton != null) {
            mButton.setOnClickListener(this);
        }
        refreshLayout();
    }

    @VisibleForTesting
    void refreshLayout() {
        boolean isWifiEnabled = mWifiManager.isWifiEnabled();
        if (mTextView != null) {
            mTextView.setText(isWifiEnabled ? R.string.viewing_airplane_mode_networks
                    : R.string.condition_airplane_title);
        }
        if (mButton != null) {
            mButton.setVisibility(isWifiEnabled ? View.GONE : View.VISIBLE);
        }
    }
}
