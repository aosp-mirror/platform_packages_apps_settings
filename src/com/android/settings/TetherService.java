/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.wifi.WifiApEnabler;

import java.util.ArrayList;

public class TetherService extends Service {
    private static final String TAG = "TetherService";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    public static final String EXTRA_ADD_TETHER_TYPE = "extraAddTetherType";
    public static final String EXTRA_REM_TETHER_TYPE = "extraRemTetherType";
    public static final String EXTRA_SET_ALARM = "extraSetAlarm";
    public static final String EXTRA_RUN_PROVISION = "extraRunProvision";
    public static final String EXTRA_ENABLE_WIFI_TETHER = "extraEnableWifiTether";

    private static final String EXTRA_RESULT = "EntitlementResult";

    // Activity results to match the activity provision protocol.
    // Default to something not ok.
    private static final int RESULT_DEFAULT = Activity.RESULT_CANCELED;
    private static final int RESULT_OK = Activity.RESULT_OK;

    private static final String TETHER_CHOICE = "TETHER_TYPE";
    private static final int MS_PER_HOUR = 60 * 60 * 1000;

    private static final String PREFS = "tetherPrefs";
    private static final String KEY_TETHERS = "currentTethers";

    private int mCurrentTypeIndex;
    private boolean mEnableWifiAfterCheck;
    private boolean mInProvisionCheck;
    private ArrayList<Integer> mCurrentTethers;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.d(TAG, "Creating WifiProvisionService");
        String provisionResponse = getResources().getString(
                com.android.internal.R.string.config_mobile_hotspot_provision_response);
        registerReceiver(mReceiver, new IntentFilter(provisionResponse),
                android.Manifest.permission.CONNECTIVITY_INTERNAL, null);
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        mCurrentTethers = stringToTethers(prefs.getString(KEY_TETHERS, ""));
        mCurrentTypeIndex = 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.hasExtra(EXTRA_ADD_TETHER_TYPE)) {
            int type = intent.getIntExtra(EXTRA_ADD_TETHER_TYPE, TetherSettings.INVALID);
            if (!mCurrentTethers.contains(type)) {
                if (DEBUG) Log.d(TAG, "Adding tether " + type);
                mCurrentTethers.add(type);
            }
        }
        if (intent.hasExtra(EXTRA_REM_TETHER_TYPE)) {
            int type = intent.getIntExtra(EXTRA_REM_TETHER_TYPE, TetherSettings.INVALID);
            if (DEBUG) Log.d(TAG, "Removing tether " + type);
            int index = mCurrentTethers.indexOf(type);
            if (index >= 0) {
                mCurrentTethers.remove(index);
                // If we are currently in the middle of a check, we may need to adjust the
                // index accordingly.
                if (index <= mCurrentTypeIndex && mCurrentTypeIndex > 0) {
                    mCurrentTypeIndex--;
                }
            }
            cancelAlarmIfNecessary();
        }
        // Only set the alarm if we have one tether, meaning the one just added,
        // to avoid setting it when it was already set previously for another
        // type.
        if (intent.getBooleanExtra(EXTRA_SET_ALARM, false)
                && mCurrentTethers.size() == 1) {
            scheduleAlarm();
        }

        if (intent.getBooleanExtra(EXTRA_ENABLE_WIFI_TETHER, false)) {
            mEnableWifiAfterCheck = true;
        }

        if (intent.getBooleanExtra(EXTRA_RUN_PROVISION, false)) {
            startProvisioning(mCurrentTypeIndex);
        } else if (!mInProvisionCheck) {
            // If we aren't running any provisioning, no reason to stay alive.
            stopSelf();
            return START_NOT_STICKY;
        }
        // We want to be started if we are killed accidently, so that we can be sure we finish
        // the check.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mInProvisionCheck) {
            Log.e(TAG, "TetherService getting destroyed while mid-provisioning"
                    + mCurrentTethers.get(mCurrentTypeIndex));
        }
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit().putString(KEY_TETHERS, tethersToString(mCurrentTethers)).commit();

        if (DEBUG) Log.d(TAG, "Destroying WifiProvisionService");
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private ArrayList<Integer> stringToTethers(String tethersStr) {
        ArrayList<Integer> ret = new ArrayList<Integer>();
        if (TextUtils.isEmpty(tethersStr)) return ret;

        String[] tethersSplit = tethersStr.split(",");
        for (int i = 0; i < tethersSplit.length; i++) {
            ret.add(Integer.parseInt(tethersSplit[i]));
        }
        return ret;
    }

    private String tethersToString(ArrayList<Integer> tethers) {
        final StringBuffer buffer = new StringBuffer();
        final int N = tethers.size();
        for (int i = 0; i < N; i++) {
            if (i != 0) {
                buffer.append(',');
            }
            buffer.append(tethers.get(i));
        }

        return buffer.toString();
    }

    private void enableWifiTetheringIfNeeded() {
        if (!isHotspotEnabled(this)) {
            new WifiApEnabler(this, null).setSoftapEnabled(true);
        }
    }

    private void disableWifiTethering() {
        WifiApEnabler enabler = new WifiApEnabler(this, null);
        enabler.setSoftapEnabled(false);
    }

    private void disableUsbTethering() {
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.setUsbTethering(false);
    }

    private void disableBtTethering() {
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.getProfileProxy(this, new ServiceListener() {
                @Override
                public void onServiceDisconnected(int profile) { }

                @Override
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    ((BluetoothPan) proxy).setBluetoothTethering(false);
                    adapter.closeProfileProxy(BluetoothProfile.PAN, proxy);
                }
            }, BluetoothProfile.PAN);
        }
    }

    private void startProvisioning(int index) {
        String provisionAction = getResources().getString(
                com.android.internal.R.string.config_mobile_hotspot_provision_app_no_ui);
        if (DEBUG) Log.d(TAG, "Sending provisioning broadcast: " + provisionAction + " type: "
                + mCurrentTethers.get(index));
        Intent intent = new Intent(provisionAction);
        intent.putExtra(TETHER_CHOICE, mCurrentTethers.get(index));
        intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        sendBroadcast(intent);
        mInProvisionCheck = true;
    }

    private static boolean isHotspotEnabled(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        return wifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED;
    }

    public static void scheduleRecheckAlarm(Context context, int type) {
        Intent intent = new Intent(context, TetherService.class);
        intent.putExtra(EXTRA_ADD_TETHER_TYPE, type);
        intent.putExtra(EXTRA_SET_ALARM, true);
        context.startService(intent);
    }

    private void scheduleAlarm() {
        Intent intent = new Intent(this, TetherService.class);
        intent.putExtra(EXTRA_RUN_PROVISION, true);

        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int period = getResources().getInteger(
                com.android.internal.R.integer.config_mobile_hotspot_provision_check_period);
        long periodMs = period * MS_PER_HOUR;
        long firstTime = SystemClock.elapsedRealtime() + periodMs;
        if (DEBUG) Log.d(TAG, "Scheduling alarm at interval " + periodMs);
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, firstTime, periodMs,
                pendingIntent);
    }

    /**
     * Cancels the recheck alarm only if no tethering is currently active.
     *
     * Runs in the background, to get access to bluetooth service that takes time to bind.
     */
    public static void cancelRecheckAlarmIfNecessary(final Context context, int type) {
        Intent intent = new Intent(context, TetherService.class);
        intent.putExtra(EXTRA_REM_TETHER_TYPE, type);
        context.startService(intent);
    }

    private void cancelAlarmIfNecessary() {
        if (mCurrentTethers.size() != 0) {
            if (DEBUG) Log.d(TAG, "Tethering still active, not cancelling alarm");
            return;
        }
        Intent intent = new Intent(this, TetherService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        if (DEBUG) Log.d(TAG, "Tethering no longer active, canceling recheck");
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) Log.d(TAG, "Got provision result " + intent);
            String provisionResponse = context.getResources().getString(
                    com.android.internal.R.string.config_mobile_hotspot_provision_response);
            if (provisionResponse.equals(intent.getAction())) {
                mInProvisionCheck = false;
                int checkType = mCurrentTethers.get(mCurrentTypeIndex);
                if (intent.getIntExtra(EXTRA_RESULT, RESULT_DEFAULT) == RESULT_OK) {
                    if (checkType == TetherSettings.WIFI_TETHERING && mEnableWifiAfterCheck) {
                        enableWifiTetheringIfNeeded();
                        mEnableWifiAfterCheck = false;
                    }
                } else {
                    switch (checkType) {
                        case TetherSettings.WIFI_TETHERING:
                            disableWifiTethering();
                            break;
                        case TetherSettings.BLUETOOTH_TETHERING:
                            disableBtTethering();
                            break;
                        case TetherSettings.USB_TETHERING:
                            disableUsbTethering();
                            break;
                    }
                }
                if (++mCurrentTypeIndex == mCurrentTethers.size()) {
                    // We are done with all checks, time to die.
                    stopSelf();
                } else {
                    // Start the next check in our list.
                    startProvisioning(mCurrentTypeIndex);
                }
            }
        }
    };

}
