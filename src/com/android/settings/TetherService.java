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
import android.app.usage.UsageStatsManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.TetherUtil;

import java.util.ArrayList;
import java.util.List;

public class TetherService extends Service {
    private static final String TAG = "TetherService";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    @VisibleForTesting
    public static final String EXTRA_RESULT = "EntitlementResult";

    // Activity results to match the activity provision protocol.
    // Default to something not ok.
    private static final int RESULT_DEFAULT = Activity.RESULT_CANCELED;
    private static final int RESULT_OK = Activity.RESULT_OK;

    private static final String TETHER_CHOICE = "TETHER_TYPE";
    private static final int MS_PER_HOUR = 60 * 60 * 1000;

    private static final String PREFS = "tetherPrefs";
    private static final String KEY_TETHERS = "currentTethers";

    private int mCurrentTypeIndex;
    private boolean mInProvisionCheck;
    private UsageStatsManagerWrapper mUsageManagerWrapper;
    private ArrayList<Integer> mCurrentTethers;
    private ArrayMap<Integer, List<ResultReceiver>> mPendingCallbacks;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.d(TAG, "Creating TetherService");
        String provisionResponse = getResources().getString(
                com.android.internal.R.string.config_mobile_hotspot_provision_response);
        registerReceiver(mReceiver, new IntentFilter(provisionResponse),
                android.Manifest.permission.CONNECTIVITY_INTERNAL, null);
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        mCurrentTethers = stringToTethers(prefs.getString(KEY_TETHERS, ""));
        mCurrentTypeIndex = 0;
        mPendingCallbacks = new ArrayMap<>(3);
        mPendingCallbacks.put(ConnectivityManager.TETHERING_WIFI, new ArrayList<ResultReceiver>());
        mPendingCallbacks.put(ConnectivityManager.TETHERING_USB, new ArrayList<ResultReceiver>());
        mPendingCallbacks.put(
                ConnectivityManager.TETHERING_BLUETOOTH, new ArrayList<ResultReceiver>());
        if (mUsageManagerWrapper == null) {
            mUsageManagerWrapper = new UsageStatsManagerWrapper(this);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.hasExtra(ConnectivityManager.EXTRA_ADD_TETHER_TYPE)) {
            int type = intent.getIntExtra(ConnectivityManager.EXTRA_ADD_TETHER_TYPE,
                    ConnectivityManager.TETHERING_INVALID);
            ResultReceiver callback =
                    intent.getParcelableExtra(ConnectivityManager.EXTRA_PROVISION_CALLBACK);
            if (callback != null) {
                List<ResultReceiver> callbacksForType = mPendingCallbacks.get(type);
                if (callbacksForType != null) {
                    callbacksForType.add(callback);
                } else {
                    // Invalid tether type. Just ignore this request and report failure.
                    callback.send(ConnectivityManager.TETHER_ERROR_UNKNOWN_IFACE, null);
                    stopSelf();
                    return START_NOT_STICKY;
                }
            }

            if (!mCurrentTethers.contains(type)) {
                if (DEBUG) Log.d(TAG, "Adding tether " + type);
                mCurrentTethers.add(type);
            }
        }

        if (intent.hasExtra(ConnectivityManager.EXTRA_REM_TETHER_TYPE)) {
            if (!mInProvisionCheck) {
                int type = intent.getIntExtra(ConnectivityManager.EXTRA_REM_TETHER_TYPE,
                        ConnectivityManager.TETHERING_INVALID);
                int index = mCurrentTethers.indexOf(type);
                if (DEBUG) Log.d(TAG, "Removing tether " + type + ", index " + index);
                if (index >= 0) {
                    removeTypeAtIndex(index);
                }
                cancelAlarmIfNecessary();
            } else {
                if (DEBUG) Log.d(TAG, "Don't cancel alarm during provisioning");
            }
        }

        // Only set the alarm if we have one tether, meaning the one just added,
        // to avoid setting it when it was already set previously for another
        // type.
        if (intent.getBooleanExtra(ConnectivityManager.EXTRA_SET_ALARM, false)
                && mCurrentTethers.size() == 1) {
            scheduleAlarm();
        }

        if (intent.getBooleanExtra(ConnectivityManager.EXTRA_RUN_PROVISION, false)) {
            startProvisioning(mCurrentTypeIndex);
        } else if (!mInProvisionCheck) {
            // If we aren't running any provisioning, no reason to stay alive.
            if (DEBUG) Log.d(TAG, "Stopping self.  startid: " + startId);
            stopSelf();
            return START_NOT_STICKY;
        }
        // We want to be started if we are killed accidently, so that we can be sure we finish
        // the check.
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        if (mInProvisionCheck) {
            Log.e(TAG, "TetherService getting destroyed while mid-provisioning"
                    + mCurrentTethers.get(mCurrentTypeIndex));
        }
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit().putString(KEY_TETHERS, tethersToString(mCurrentTethers)).commit();

        if (DEBUG) Log.d(TAG, "Destroying TetherService");
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private void removeTypeAtIndex(int index) {
        mCurrentTethers.remove(index);
        // If we are currently in the middle of a check, we may need to adjust the
        // index accordingly.
        if (DEBUG) Log.d(TAG, "mCurrentTypeIndex: " + mCurrentTypeIndex);
        if (index <= mCurrentTypeIndex && mCurrentTypeIndex > 0) {
            mCurrentTypeIndex--;
        }
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

    private void disableWifiTethering() {
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.stopTethering(ConnectivityManager.TETHERING_WIFI);
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
        if (index < mCurrentTethers.size()) {
            Intent intent = getProvisionBroadcastIntent(index);
            setEntitlementAppActive(index);

            if (DEBUG) Log.d(TAG, "Sending provisioning broadcast: " + intent.getAction()
                    + " type: " + mCurrentTethers.get(index));

            sendBroadcast(intent);
            mInProvisionCheck = true;
        }
    }

    private Intent getProvisionBroadcastIntent(int index) {
        String provisionAction = getResources().getString(
                com.android.internal.R.string.config_mobile_hotspot_provision_app_no_ui);
        Intent intent = new Intent(provisionAction);
        int type = mCurrentTethers.get(index);
        intent.putExtra(TETHER_CHOICE, type);
        intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);

        return intent;
    }

    private void setEntitlementAppActive(int index) {
        final PackageManager packageManager = getPackageManager();
        Intent intent = getProvisionBroadcastIntent(index);
        List<ResolveInfo> resolvers =
                packageManager.queryBroadcastReceivers(intent, PackageManager.MATCH_ALL);
        if (resolvers.isEmpty()) {
            Log.e(TAG, "No found BroadcastReceivers for provision intent.");
            return;
        }

        for (ResolveInfo resolver : resolvers) {
            if (resolver.activityInfo.applicationInfo.isSystemApp()) {
                String packageName = resolver.activityInfo.packageName;
                mUsageManagerWrapper.setAppInactive(packageName, false);
            }
        }
    }

    private void scheduleAlarm() {
        Intent intent = new Intent(this, TetherService.class);
        intent.putExtra(ConnectivityManager.EXTRA_RUN_PROVISION, true);

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
        intent.putExtra(ConnectivityManager.EXTRA_REM_TETHER_TYPE, type);
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

    private void fireCallbacksForType(int type, int result) {
        List<ResultReceiver> callbacksForType = mPendingCallbacks.get(type);
        if (callbacksForType == null) {
            return;
        }
        int errorCode = result == RESULT_OK ? ConnectivityManager.TETHER_ERROR_NO_ERROR :
                ConnectivityManager.TETHER_ERROR_PROVISION_FAILED;
        for (ResultReceiver callback : callbacksForType) {
          if (DEBUG) Log.d(TAG, "Firing result: " + errorCode + " to callback");
          callback.send(errorCode, null);
        }
        callbacksForType.clear();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) Log.d(TAG, "Got provision result " + intent);
            String provisionResponse = getResources().getString(
                    com.android.internal.R.string.config_mobile_hotspot_provision_response);

            if (provisionResponse.equals(intent.getAction())) {
                if (!mInProvisionCheck) {
                    Log.e(TAG, "Unexpected provision response " + intent);
                    return;
                }
                int checkType = mCurrentTethers.get(mCurrentTypeIndex);
                mInProvisionCheck = false;
                int result = intent.getIntExtra(EXTRA_RESULT, RESULT_DEFAULT);
                if (result != RESULT_OK) {
                    switch (checkType) {
                        case ConnectivityManager.TETHERING_WIFI:
                            disableWifiTethering();
                            break;
                        case ConnectivityManager.TETHERING_BLUETOOTH:
                            disableBtTethering();
                            break;
                        case ConnectivityManager.TETHERING_USB:
                            disableUsbTethering();
                            break;
                    }
                }
                fireCallbacksForType(checkType, result);

                if (++mCurrentTypeIndex >= mCurrentTethers.size()) {
                    // We are done with all checks, time to die.
                    stopSelf();
                } else {
                    // Start the next check in our list.
                    startProvisioning(mCurrentTypeIndex);
                }
            }
        }
    };

    @VisibleForTesting
    void setUsageStatsManagerWrapper(UsageStatsManagerWrapper wrapper) {
        mUsageManagerWrapper = wrapper;
    }

    /**
     * A static helper class used for tests. UsageStatsManager cannot be mocked out becasue
     * it's marked final. This class can be mocked out instead.
     */
    @VisibleForTesting
    public static class UsageStatsManagerWrapper {
        private final UsageStatsManager mUsageStatsManager;

        UsageStatsManagerWrapper(Context context) {
            mUsageStatsManager = (UsageStatsManager)
                    context.getSystemService(Context.USAGE_STATS_SERVICE);
        }

        void setAppInactive(String packageName, boolean isInactive) {
            mUsageStatsManager.setAppInactive(packageName, isInactive);
        }
    }
}
