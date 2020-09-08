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

import static android.os.UserManager.DISALLOW_CONFIG_TETHERING;

import static com.android.settings.network.TetherEnabler.TETHERING_BLUETOOTH_ON;
import static com.android.settings.network.TetherEnabler.TETHERING_ETHERNET_ON;
import static com.android.settings.network.TetherEnabler.TETHERING_OFF;
import static com.android.settings.network.TetherEnabler.TETHERING_USB_ON;
import static com.android.settings.network.TetherEnabler.TETHERING_WIFI_ON;
import static com.android.settingslib.RestrictedLockUtilsInternal.checkIfRestrictionEnforced;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.UserHandle;
import android.util.FeatureFlagUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.FeatureFlags;
import com.android.settings.widget.MasterSwitchController;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settingslib.TetherUtil;

import java.util.concurrent.atomic.AtomicReference;

/**
 * This controller helps to manage the switch state and visibility of "Hotspot & tethering" switch
 * preference. It updates the preference summary text based on tethering state.
 */
public class AllInOneTetherPreferenceController extends BasePreferenceController implements
        LifecycleObserver, TetherEnabler.OnTetherStateUpdateListener {
    private static final String TAG = "AllInOneTetherPreferenceController";

    private int mTetheringState;

    private final boolean mAdminDisallowedTetherConfig;
    private final AtomicReference<BluetoothPan> mBluetoothPan;
    private final BluetoothAdapter mBluetoothAdapter;
    @VisibleForTesting
    final BluetoothProfile.ServiceListener mBtProfileServiceListener =
            new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    mBluetoothPan.set((BluetoothPan) proxy);
                }

                @Override
                public void onServiceDisconnected(int profile) {
                    mBluetoothPan.set(null);
                }
            };

    private MasterSwitchPreference mPreference;
    private TetherEnabler mTetherEnabler;

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    AllInOneTetherPreferenceController() {
        super(null /*context*/, "test");
        mAdminDisallowedTetherConfig = false;
        mBluetoothPan = new AtomicReference<>();
        mBluetoothAdapter = null;
    }

    public AllInOneTetherPreferenceController(Context context, String key) {
        super(context, key);
        mBluetoothPan = new AtomicReference<>();
        mAdminDisallowedTetherConfig = checkIfRestrictionEnforced(
                context, DISALLOW_CONFIG_TETHERING, UserHandle.myUserId()) != null;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(mPreferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!TetherUtil.isTetherAvailable(mContext)
                || !FeatureFlagUtils.isEnabled(mContext, FeatureFlags.TETHER_ALL_IN_ONE)) {
            return CONDITIONALLY_UNAVAILABLE;
        } else {
            return AVAILABLE;
        }
    }

    @Override
    public CharSequence getSummary() {
        switch (mTetheringState) {
            case TETHERING_OFF:
                return mContext.getString(R.string.tether_settings_summary_off);
            case TETHERING_WIFI_ON:
                return mContext.getString(R.string.tether_settings_summary_hotspot_only);
            case TETHERING_USB_ON:
                return mContext.getString(R.string.tether_settings_summary_usb_tethering_only);
            case TETHERING_BLUETOOTH_ON:
                return mContext.getString(
                        R.string.tether_settings_summary_bluetooth_tethering_only);
            case TETHERING_ETHERNET_ON:
                return mContext.getString(R.string.tether_settings_summary_ethernet_tethering_only);
            case TETHERING_WIFI_ON | TETHERING_USB_ON:
                return mContext.getString(R.string.tether_settings_summary_hotspot_and_usb);
            case TETHERING_WIFI_ON | TETHERING_BLUETOOTH_ON:
                return mContext.getString(R.string.tether_settings_summary_hotspot_and_bluetooth);
            case TETHERING_WIFI_ON | TETHERING_ETHERNET_ON:
                return mContext.getString(R.string.tether_settings_summary_hotspot_and_ethernet);
            case TETHERING_USB_ON | TETHERING_BLUETOOTH_ON:
                return mContext.getString(R.string.tether_settings_summary_usb_and_bluetooth);
            case TETHERING_USB_ON | TETHERING_ETHERNET_ON:
                return mContext.getString(R.string.tether_settings_summary_usb_and_ethernet);
            case TETHERING_BLUETOOTH_ON | TETHERING_ETHERNET_ON:
                return mContext.getString(R.string.tether_settings_summary_bluetooth_and_ethernet);
            case TETHERING_WIFI_ON | TETHERING_USB_ON | TETHERING_BLUETOOTH_ON:
                return mContext.getString(
                        R.string.tether_settings_summary_hotspot_and_usb_and_bluetooth);
            case TETHERING_WIFI_ON | TETHERING_USB_ON | TETHERING_ETHERNET_ON:
                return mContext.getString(
                        R.string.tether_settings_summary_hotspot_and_usb_and_ethernet);
            case TETHERING_WIFI_ON | TETHERING_BLUETOOTH_ON | TETHERING_ETHERNET_ON:
                return mContext.getString(
                        R.string.tether_settings_summary_hotspot_and_bluetooth_and_ethernet);
            case TETHERING_USB_ON | TETHERING_BLUETOOTH_ON | TETHERING_ETHERNET_ON:
                return mContext.getString(
                        R.string.tether_settings_summary_usb_and_bluetooth_and_ethernet);
            case TETHERING_WIFI_ON | TETHERING_USB_ON | TETHERING_BLUETOOTH_ON
                    | TETHERING_ETHERNET_ON:
                return mContext.getString(R.string.tether_settings_summary_all);
            default:
                Log.e(TAG, "Unknown tethering state");
                return mContext.getString(R.string.summary_placeholder);
        }
    }

    @OnLifecycleEvent(Event.ON_CREATE)
    public void onCreate() {
        if (mBluetoothAdapter != null
                && mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
            mBluetoothAdapter.getProfileProxy(mContext, mBtProfileServiceListener,
                        BluetoothProfile.PAN);
        }
    }

    @OnLifecycleEvent(Event.ON_RESUME)
    public void onResume() {
        if (mTetherEnabler != null) {
            mTetherEnabler.addListener(this);
        }
    }

    @OnLifecycleEvent(Event.ON_PAUSE)
    public void onPause() {
        if (mTetherEnabler != null) {
            mTetherEnabler.removeListener(this);
        }
    }

    @OnLifecycleEvent(Event.ON_DESTROY)
    public void onDestroy() {
        final BluetoothProfile profile = mBluetoothPan.getAndSet(null);
        if (profile != null && mBluetoothAdapter != null) {
            mBluetoothAdapter.closeProfileProxy(BluetoothProfile.PAN, profile);
        }
    }

    void initEnabler(Lifecycle lifecycle) {
        if (mPreference != null) {
            mTetherEnabler = new TetherEnabler(
                    mContext, new MasterSwitchController(mPreference), mBluetoothPan);
            if (lifecycle != null) {
                lifecycle.addObserver(mTetherEnabler);
            }
        } else {
            Log.e(TAG, "TetherEnabler is not initialized");
        }
    }

    @Override
    public void onTetherStateUpdated(@TetherEnabler.TetheringState int state) {
        mTetheringState = state;
        updateState(mPreference);
    }
}
