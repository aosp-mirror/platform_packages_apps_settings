/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static com.android.settingslib.RestrictedLockUtilsInternal.checkIfRestrictionEnforced;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.FeatureFlagUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.FeatureFlags;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.TetherUtil;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.util.concurrent.atomic.AtomicReference;

public class TetherPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, LifecycleObserver, OnCreate, OnResume, OnPause, OnDestroy {

    private static final String KEY_TETHER_SETTINGS = "tether_settings";

    private final boolean mAdminDisallowedTetherConfig;
    private final AtomicReference<BluetoothPan> mBluetoothPan;
    private final ConnectivityManager mConnectivityManager;
    private final BluetoothAdapter mBluetoothAdapter;
    @VisibleForTesting
    final BluetoothProfile.ServiceListener mBtProfileServiceListener =
            new android.bluetooth.BluetoothProfile.ServiceListener() {
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    mBluetoothPan.set((BluetoothPan) proxy);
                    updateSummary();
                }

                public void onServiceDisconnected(int profile) {
                    mBluetoothPan.set(null);
                }
            };

    private SettingObserver mAirplaneModeObserver;
    private Preference mPreference;
    private TetherBroadcastReceiver mTetherReceiver;

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    TetherPreferenceController() {
        super(null);
        mAdminDisallowedTetherConfig = false;
        mBluetoothPan = new AtomicReference<>();
        mConnectivityManager = null;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public TetherPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        mBluetoothPan = new AtomicReference<>();
        mAdminDisallowedTetherConfig = isTetherConfigDisallowed(context);
        mConnectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(KEY_TETHER_SETTINGS);
        if (mPreference != null && !mAdminDisallowedTetherConfig) {
            mPreference.setTitle(
                    com.android.settingslib.Utils.getTetheringLabel(mConnectivityManager));
        }
    }

    @Override
    public boolean isAvailable() {
        return TetherUtil.isTetherAvailable(mContext)
                && !FeatureFlagUtils.isEnabled(mContext, FeatureFlags.TETHER_ALL_IN_ONE);
    }

    @Override
    public void updateState(Preference preference) {
        updateSummary();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_TETHER_SETTINGS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (mBluetoothAdapter != null &&
            mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
            mBluetoothAdapter.getProfileProxy(mContext, mBtProfileServiceListener,
                    BluetoothProfile.PAN);
        }
    }

    @Override
    public void onResume() {
        if (mAirplaneModeObserver == null) {
            mAirplaneModeObserver = new SettingObserver();
        }
        if (mTetherReceiver == null) {
            mTetherReceiver = new TetherBroadcastReceiver();
        }
        mContext.registerReceiver(
                mTetherReceiver, new IntentFilter(ConnectivityManager.ACTION_TETHER_STATE_CHANGED));
        mContext.getContentResolver()
                .registerContentObserver(mAirplaneModeObserver.uri, false, mAirplaneModeObserver);
    }

    @Override
    public void onPause() {
        if (mAirplaneModeObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mAirplaneModeObserver);
        }
        if (mTetherReceiver != null) {
            mContext.unregisterReceiver(mTetherReceiver);
        }
    }

    @Override
    public void onDestroy() {
        final BluetoothProfile profile = mBluetoothPan.getAndSet(null);
        if (profile != null && mBluetoothAdapter != null) {
            mBluetoothAdapter.closeProfileProxy(BluetoothProfile.PAN, profile);
        }
    }

    public static boolean isTetherConfigDisallowed(Context context) {
        return checkIfRestrictionEnforced(
                context, DISALLOW_CONFIG_TETHERING, UserHandle.myUserId()) != null;
    }

    @VisibleForTesting
    void updateSummary() {
        if (mPreference == null) {
            // Preference is not ready yet.
            return;
        }
        String[] allTethered = mConnectivityManager.getTetheredIfaces();
        String[] wifiTetherRegex = mConnectivityManager.getTetherableWifiRegexs();
        String[] bluetoothRegex = mConnectivityManager.getTetherableBluetoothRegexs();

        boolean hotSpotOn = false;
        boolean tetherOn = false;
        if (allTethered != null) {
            if (wifiTetherRegex != null) {
                for (String tethered : allTethered) {
                    for (String regex : wifiTetherRegex) {
                        if (tethered.matches(regex)) {
                            hotSpotOn = true;
                            break;
                        }
                    }
                }
            }
            if (allTethered.length > 1) {
                // We have more than 1 tethered connection
                tetherOn = true;
            } else if (allTethered.length == 1) {
                // We have more than 1 tethered, it's either wifiTether (hotspot), or other type of
                // tether.
                tetherOn = !hotSpotOn;
            } else {
                // No tethered connection.
                tetherOn = false;
            }
        }
        if (!tetherOn
                && bluetoothRegex != null && bluetoothRegex.length > 0
                && mBluetoothAdapter != null
                && mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
            // Check bluetooth state. It's not included in mConnectivityManager.getTetheredIfaces.
            final BluetoothPan pan = mBluetoothPan.get();
            tetherOn = pan != null && pan.isTetheringOn();
        }
        if (!hotSpotOn && !tetherOn) {
            // Both off
            mPreference.setSummary(R.string.switch_off_text);
        } else if (hotSpotOn && tetherOn) {
            // Both on
            mPreference.setSummary(R.string.tether_settings_summary_hotspot_on_tether_on);
        } else if (hotSpotOn) {
            mPreference.setSummary(R.string.tether_settings_summary_hotspot_on_tether_off);
        } else {
            mPreference.setSummary(R.string.tether_settings_summary_hotspot_off_tether_on);
        }
    }

    private void updateSummaryToOff() {
        if (mPreference == null) {
            // Preference is not ready yet.
            return;
        }
        mPreference.setSummary(R.string.switch_off_text);
    }

    class SettingObserver extends ContentObserver {

        public final Uri uri;

        public SettingObserver() {
            super(new Handler());
            uri = Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (this.uri.equals(uri)) {
                boolean isAirplaneMode = Settings.Global.getInt(mContext.getContentResolver(),
                        Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
                if (isAirplaneMode) {
                    // Airplane mode is on. Update summary to say tether is OFF directly. We cannot
                    // go through updateSummary() because turning off tether takes time, and we
                    // might still get "ON" status when rerun updateSummary(). So, just say it's off
                    updateSummaryToOff();
                }
            }
        }
    }

    @VisibleForTesting
    class TetherBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            updateSummary();
        }

    }
}
