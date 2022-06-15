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

package com.android.settings.wifi.tether;

import static android.net.TetheringConstants.EXTRA_ADD_TETHER_TYPE;
import static android.net.TetheringConstants.EXTRA_PROVISION_CALLBACK;
import static android.net.TetheringConstants.EXTRA_REM_TETHER_TYPE;
import static android.net.TetheringConstants.EXTRA_RUN_PROVISION;
import static android.net.TetheringManager.TETHERING_BLUETOOTH;
import static android.net.TetheringManager.TETHERING_ETHERNET;
import static android.net.TetheringManager.TETHERING_INVALID;
import static android.net.TetheringManager.TETHERING_USB;
import static android.net.TetheringManager.TETHERING_WIFI;
import static android.net.TetheringManager.TETHER_ERROR_NO_ERROR;
import static android.net.TetheringManager.TETHER_ERROR_PROVISIONING_FAILED;
import static android.net.TetheringManager.TETHER_ERROR_UNKNOWN_IFACE;
import static android.telephony.SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX;
import static android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID;

import android.app.Activity;
import android.app.Service;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.TetheringManager;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TetherService extends Service {
    private static final String TAG = "TetherService";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    @VisibleForTesting
    public static final String EXTRA_RESULT = "EntitlementResult";
    @VisibleForTesting
    public static final String EXTRA_TETHER_SUBID = "android.net.extra.TETHER_SUBID";
    @VisibleForTesting
    public static final String EXTRA_TETHER_PROVISIONING_RESPONSE =
            "android.net.extra.TETHER_PROVISIONING_RESPONSE";
    @VisibleForTesting
    public static final String EXTRA_TETHER_SILENT_PROVISIONING_ACTION =
            "android.net.extra.TETHER_SILENT_PROVISIONING_ACTION";

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
    /** Intent action received from the provisioning app when entitlement check completes. */
    private String mExpectedProvisionResponseAction = null;
    /** Intent action sent to the provisioning app to request an entitlement check. */
    private String mProvisionAction;
    private int mSubId = INVALID_SUBSCRIPTION_ID;
    private TetherServiceWrapper mWrapper;
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
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        mCurrentTethers = stringToTethers(prefs.getString(KEY_TETHERS, ""));
        mCurrentTypeIndex = 0;
        mPendingCallbacks = new ArrayMap<>(3);
        mPendingCallbacks.put(TETHERING_WIFI, new ArrayList<ResultReceiver>());
        mPendingCallbacks.put(TETHERING_USB, new ArrayList<ResultReceiver>());
        mPendingCallbacks.put(TETHERING_BLUETOOTH, new ArrayList<ResultReceiver>());
        mPendingCallbacks.put(TETHERING_ETHERNET, new ArrayList<ResultReceiver>());
    }

    // Registers the broadcast receiver for the specified response action, first unregistering
    // the receiver if it was registered for a different response action.
    private void maybeRegisterReceiver(final String responseAction) {
        if (Objects.equals(responseAction, mExpectedProvisionResponseAction)) return;

        if (mExpectedProvisionResponseAction != null) unregisterReceiver(mReceiver);

        registerReceiver(mReceiver, new IntentFilter(responseAction),
                android.Manifest.permission.TETHER_PRIVILEGED, null /* handler */);
        mExpectedProvisionResponseAction = responseAction;
        if (DEBUG) Log.d(TAG, "registerReceiver " + responseAction);
    }

    private int stopSelfAndStartNotSticky() {
        stopSelf();
        return START_NOT_STICKY;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.hasExtra(EXTRA_TETHER_SUBID)) {
            final int tetherSubId = intent.getIntExtra(EXTRA_TETHER_SUBID, INVALID_SUBSCRIPTION_ID);
            final int subId = getTetherServiceWrapper().getActiveDataSubscriptionId();
            if (tetherSubId != subId) {
                Log.e(TAG, "This Provisioning request is outdated, current subId: " + subId);
                if (!mInProvisionCheck) {
                    stopSelf();
                }
                return START_NOT_STICKY;
            }
            mSubId = subId;
        }

        if (intent.hasExtra(EXTRA_ADD_TETHER_TYPE)) {
            int type = intent.getIntExtra(EXTRA_ADD_TETHER_TYPE, TETHERING_INVALID);
            ResultReceiver callback = intent.getParcelableExtra(EXTRA_PROVISION_CALLBACK);
            if (callback != null) {
                List<ResultReceiver> callbacksForType = mPendingCallbacks.get(type);
                if (callbacksForType != null) {
                    callbacksForType.add(callback);
                } else {
                    // Invalid tether type. Just ignore this request and report failure.
                    Log.e(TAG, "Invalid tethering type " + type + ", stopping");
                    callback.send(TETHER_ERROR_UNKNOWN_IFACE, null);
                    return stopSelfAndStartNotSticky();
                }
            }

            if (!mCurrentTethers.contains(type)) {
                if (DEBUG) Log.d(TAG, "Adding tether " + type);
                mCurrentTethers.add(type);
            }
        }

        mProvisionAction = intent.getStringExtra(EXTRA_TETHER_SILENT_PROVISIONING_ACTION);
        if (mProvisionAction == null) {
            Log.e(TAG, "null provisioning action, stop ");
            return stopSelfAndStartNotSticky();
        }

        final String response = intent.getStringExtra(EXTRA_TETHER_PROVISIONING_RESPONSE);
        if (response == null) {
            Log.e(TAG, "null provisioning response, stop ");
            return stopSelfAndStartNotSticky();
        }
        maybeRegisterReceiver(response);

        if (intent.hasExtra(EXTRA_REM_TETHER_TYPE)) {
            if (!mInProvisionCheck) {
                int type = intent.getIntExtra(EXTRA_REM_TETHER_TYPE, TETHERING_INVALID);
                int index = mCurrentTethers.indexOf(type);
                if (DEBUG) Log.d(TAG, "Removing tether " + type + ", index " + index);
                if (index >= 0) {
                    removeTypeAtIndex(index);
                }
            } else {
                if (DEBUG) Log.d(TAG, "Don't remove tether type during provisioning");
            }
        }

        if (intent.getBooleanExtra(EXTRA_RUN_PROVISION, false)) {
            startProvisioning(mCurrentTypeIndex);
        } else if (!mInProvisionCheck) {
            // If we aren't running any provisioning, no reason to stay alive.
            if (DEBUG) Log.d(TAG, "Stopping self.  startid: " + startId);
            return stopSelfAndStartNotSticky();
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

        if (mExpectedProvisionResponseAction != null) {
            unregisterReceiver(mReceiver);
            mExpectedProvisionResponseAction = null;
        }
        if (DEBUG) Log.d(TAG, "Destroying TetherService");
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

    private void disableTethering(final int tetheringType) {
        final TetheringManager tm = (TetheringManager) getSystemService(Context.TETHERING_SERVICE);
        tm.stopTethering(tetheringType);
    }

    private void startProvisioning(int index) {
        if (index >= mCurrentTethers.size()) return;

        Intent intent = getProvisionBroadcastIntent(index);
        setEntitlementAppActive(index);

        if (DEBUG) {
            Log.d(TAG, "Sending provisioning broadcast: " + intent.getAction()
                    + " type: " + mCurrentTethers.get(index));
        }

        sendBroadcast(intent);
        mInProvisionCheck = true;
    }

    private Intent getProvisionBroadcastIntent(int index) {
        if (mProvisionAction == null) Log.wtf(TAG, "null provisioning action");
        Intent intent = new Intent(mProvisionAction);
        int type = mCurrentTethers.get(index);
        intent.putExtra(TETHER_CHOICE, type);
        intent.putExtra(EXTRA_SUBSCRIPTION_INDEX, mSubId);
        intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND
                | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);

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
                getTetherServiceWrapper().setAppInactive(packageName, false);
            }
        }
    }

    private void fireCallbacksForType(int type, int result) {
        List<ResultReceiver> callbacksForType = mPendingCallbacks.get(type);
        if (callbacksForType == null) {
            return;
        }
        int errorCode = result == RESULT_OK ? TETHER_ERROR_NO_ERROR :
                TETHER_ERROR_PROVISIONING_FAILED;
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

            if (!intent.getAction().equals(mExpectedProvisionResponseAction)) {
                Log.e(TAG, "Received provisioning response for unexpected action="
                        + intent.getAction() + ", expected=" + mExpectedProvisionResponseAction);
                return;
            }

            if (!mInProvisionCheck) {
                Log.e(TAG, "Unexpected provisioning response when not in provisioning check"
                        + intent);
                return;
            }
            int checkType = mCurrentTethers.get(mCurrentTypeIndex);
            mInProvisionCheck = false;
            int result = intent.getIntExtra(EXTRA_RESULT, RESULT_DEFAULT);
            if (result != RESULT_OK) disableTethering(checkType);
            fireCallbacksForType(checkType, result);

            if (++mCurrentTypeIndex >= mCurrentTethers.size()) {
                // We are done with all checks, time to die.
                stopSelf();
            } else {
                // Start the next check in our list.
                startProvisioning(mCurrentTypeIndex);
            }
        }
    };

    @VisibleForTesting
    void setTetherServiceWrapper(TetherServiceWrapper wrapper) {
        mWrapper = wrapper;
    }

    private TetherServiceWrapper getTetherServiceWrapper() {
        if (mWrapper == null) {
            mWrapper = new TetherServiceWrapper(this);
        }
        return mWrapper;
    }

    /**
     * A static helper class used for tests. UsageStatsManager cannot be mocked out because
     * it's marked final. This class can be mocked out instead.
     */
    @VisibleForTesting
    public static class TetherServiceWrapper {
        private final UsageStatsManager mUsageStatsManager;

        TetherServiceWrapper(Context context) {
            mUsageStatsManager = (UsageStatsManager)
                    context.getSystemService(Context.USAGE_STATS_SERVICE);
        }

        void setAppInactive(String packageName, boolean isInactive) {
            mUsageStatsManager.setAppInactive(packageName, isInactive);
        }

        int getActiveDataSubscriptionId() {
            return SubscriptionManager.getActiveDataSubscriptionId();
        }
    }
}
