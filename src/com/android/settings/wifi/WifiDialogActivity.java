/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.wifi;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.os.UserManager.DISALLOW_ADD_WIFI_CONFIG;
import static android.os.UserManager.DISALLOW_CONFIG_WIFI;

import android.app.KeyguardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.SimpleClock;
import android.os.SystemClock;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.wifi.dpp.WifiDppUtils;
import com.android.settingslib.core.lifecycle.ObservableActivity;
import com.android.settingslib.wifi.AccessPoint;
import com.android.wifitrackerlib.NetworkDetailsTracker;
import com.android.wifitrackerlib.WifiEntry;

import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.util.ThemeHelper;

import java.lang.ref.WeakReference;
import java.time.Clock;
import java.time.ZoneOffset;

/**
 * The activity shows a Wi-fi editor dialog.
 *
 * TODO(b/152571756): This activity supports both WifiTrackerLib and SettingsLib because this is an
 *                    exported UI component, some other APPs (e.g., SetupWizard) still use
 *                    SettingsLib. Remove the SettingsLib compatible part after these APPs use
 *                    WifiTrackerLib.
 */
public class WifiDialogActivity extends ObservableActivity implements WifiDialog.WifiDialogListener,
        WifiDialog2.WifiDialog2Listener, DialogInterface.OnDismissListener {

    private static final String TAG = "WifiDialogActivity";

    // For the callers which support WifiTrackerLib.
    public static final String KEY_CHOSEN_WIFIENTRY_KEY = "key_chosen_wifientry_key";

    // For the callers which support SettingsLib.
    public static final String KEY_ACCESS_POINT_STATE = "access_point_state";

    /**
     * Boolean extra indicating whether this activity should connect to an access point on the
     * caller's behalf. If this is set to false, the caller should check
     * {@link #KEY_WIFI_CONFIGURATION} in the result data and save that using
     * {@link WifiManager#connect(WifiConfiguration, ActionListener)}. Default is true.
     */
    @VisibleForTesting
    static final String KEY_CONNECT_FOR_CALLER = "connect_for_caller";

    public static final String KEY_WIFI_CONFIGURATION = "wifi_configuration";

    @VisibleForTesting
    static final int RESULT_CONNECTED = RESULT_FIRST_USER;
    private static final int RESULT_FORGET = RESULT_FIRST_USER + 1;

    @VisibleForTesting
    static final int REQUEST_CODE_WIFI_DPP_ENROLLEE_QR_CODE_SCANNER = 0;

    // Max age of tracked WifiEntries.
    private static final long MAX_SCAN_AGE_MILLIS = 15_000;
    // Interval between initiating NetworkDetailsTracker scans.
    private static final long SCAN_INTERVAL_MILLIS = 10_000;

    @VisibleForTesting
    WifiDialog mDialog;
    private AccessPoint mAccessPoint;

    @VisibleForTesting
    WifiDialog2 mDialog2;

    // The received intent supports a key of WifiTrackerLib or SettingsLib.
    private boolean mIsWifiTrackerLib;

    private Intent mIntent;
    private NetworkDetailsTracker mNetworkDetailsTracker;
    private HandlerThread mWorkerThread;
    private WifiManager mWifiManager;
    private LockScreenMonitor mLockScreenMonitor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mIntent = getIntent();
        if (WizardManagerHelper.isSetupWizardIntent(mIntent)) {
            setTheme(SetupWizardUtils.getTransparentTheme(this, mIntent));
        }

        super.onCreate(savedInstanceState);
        if (!isConfigWifiAllowed() || !isAddWifiConfigAllowed()) {
            finish();
            return;
        }

        mIsWifiTrackerLib = !TextUtils.isEmpty(mIntent.getStringExtra(KEY_CHOSEN_WIFIENTRY_KEY));

        if (mIsWifiTrackerLib) {
            mWorkerThread = new HandlerThread(
                    TAG + "{" + Integer.toHexString(System.identityHashCode(this)) + "}",
                    Process.THREAD_PRIORITY_BACKGROUND);
            mWorkerThread.start();
            final Clock elapsedRealtimeClock = new SimpleClock(ZoneOffset.UTC) {
                @Override
                public long millis() {
                    return SystemClock.elapsedRealtime();
                }
            };
            mNetworkDetailsTracker = FeatureFactory.getFactory(this)
                    .getWifiTrackerLibProvider()
                    .createNetworkDetailsTracker(
                            getLifecycle(),
                            this,
                            new Handler(Looper.getMainLooper()),
                            mWorkerThread.getThreadHandler(),
                            elapsedRealtimeClock,
                            MAX_SCAN_AGE_MILLIS,
                            SCAN_INTERVAL_MILLIS,
                            mIntent.getStringExtra(KEY_CHOSEN_WIFIENTRY_KEY));
        } else {
            final Bundle accessPointState = mIntent.getBundleExtra(KEY_ACCESS_POINT_STATE);
            if (accessPointState != null) {
                mAccessPoint = new AccessPoint(this, accessPointState);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mDialog2 != null || mDialog != null || !hasWifiManager()) {
            return;
        }

        if (WizardManagerHelper.isAnySetupWizard(getIntent())) {
            createDialogWithSuwTheme();
        } else {
            if (mIsWifiTrackerLib) {
                mDialog2 = WifiDialog2.createModal(this, this,
                        mNetworkDetailsTracker.getWifiEntry(), WifiConfigUiBase2.MODE_CONNECT);
            } else {
                mDialog = WifiDialog.createModal(
                        this, this, mAccessPoint, WifiConfigUiBase.MODE_CONNECT);
            }
        }

        if (mIsWifiTrackerLib) {
            if (mDialog2 != null) {
                mDialog2.show();
                mDialog2.setOnDismissListener(this);
            }
        } else {
            if (mDialog != null) {
                mDialog.show();
                mDialog.setOnDismissListener(this);
            }
        }

        if (mDialog2 != null || mDialog != null) {
            mLockScreenMonitor = new LockScreenMonitor(this);
        }
    }

    @VisibleForTesting
    protected void createDialogWithSuwTheme() {
        final int targetStyle = ThemeHelper.isSetupWizardDayNightEnabled(this)
                ? R.style.SuwAlertDialogThemeCompat_DayNight :
                R.style.SuwAlertDialogThemeCompat_Light;
        if (mIsWifiTrackerLib) {
            mDialog2 = WifiDialog2.createModal(this, this,
                    mNetworkDetailsTracker.getWifiEntry(),
                    WifiConfigUiBase2.MODE_CONNECT, targetStyle);
        } else {
            mDialog = WifiDialog.createModal(this, this, mAccessPoint,
                    WifiConfigUiBase.MODE_CONNECT, targetStyle);
        }
    }

    @Override
    public void finish() {
        overridePendingTransition(0, 0);

        super.finish();
    }

    @Override
    public void onDestroy() {
        if (mIsWifiTrackerLib) {
            if (mDialog2 != null && mDialog2.isShowing()) {
                mDialog2 = null;
            }
            mWorkerThread.quit();
        } else {
            if (mDialog != null && mDialog.isShowing()) {
                mDialog = null;
            }
        }

        if (mLockScreenMonitor != null) {
            mLockScreenMonitor.release();
            mLockScreenMonitor = null;
        }
        super.onDestroy();
    }

    @Override
    public void onForget(WifiDialog2 dialog) {
        final WifiEntry wifiEntry = dialog.getController().getWifiEntry();
        if (wifiEntry != null && wifiEntry.canForget()) {
            wifiEntry.forget(null /* callback */);
        }

        setResult(RESULT_FORGET);
        finish();
    }

    @Override
    public void onForget(WifiDialog dialog) {
        if (!hasWifiManager()) return;
        final AccessPoint accessPoint = dialog.getController().getAccessPoint();
        if (accessPoint != null) {
            if (!accessPoint.isSaved()) {
                if (accessPoint.getNetworkInfo() != null &&
                        accessPoint.getNetworkInfo().getState() != NetworkInfo.State.DISCONNECTED) {
                    // Network is active but has no network ID - must be ephemeral.
                    mWifiManager.disableEphemeralNetwork(
                            AccessPoint.convertToQuotedString(accessPoint.getSsidStr()));
                } else {
                    // Should not happen, but a monkey seems to trigger it
                    Log.e(TAG, "Failed to forget invalid network " + accessPoint.getConfig());
                }
            } else {
                mWifiManager.forget(accessPoint.getConfig().networkId, null /* listener */);
            }
        }

        Intent resultData = new Intent();
        if (accessPoint != null) {
            Bundle accessPointState = new Bundle();
            accessPoint.saveWifiState(accessPointState);
            resultData.putExtra(KEY_ACCESS_POINT_STATE, accessPointState);
        }
        setResult(RESULT_FORGET);
        finish();
    }

    @Override
    public void onSubmit(WifiDialog2 dialog) {
        if (!hasWifiManager()) return;
        final WifiEntry wifiEntry = dialog.getController().getWifiEntry();
        final WifiConfiguration config = dialog.getController().getConfig();

        if (getIntent().getBooleanExtra(KEY_CONNECT_FOR_CALLER, true)) {
            if (config == null && wifiEntry != null && wifiEntry.canConnect()) {
                wifiEntry.connect(null /* callback */);
            } else {
                mWifiManager.connect(config, null /* listener */);
            }
        }

        Intent resultData = hasPermissionForResult() ? createResultData(config, null) : null;
        setResult(RESULT_CONNECTED, resultData);
        finish();
    }

    @Override
    public void onSubmit(WifiDialog dialog) {
        if (!hasWifiManager()) return;
        final WifiConfiguration config = dialog.getController().getConfig();
        final AccessPoint accessPoint = dialog.getController().getAccessPoint();

        if (getIntent().getBooleanExtra(KEY_CONNECT_FOR_CALLER, true)) {
            if (config == null) {
                if (accessPoint != null && accessPoint.isSaved()) {
                    mWifiManager.connect(accessPoint.getConfig(), null /* listener */);
                }
            } else {
                mWifiManager.save(config, null /* listener */);
                if (accessPoint != null) {
                    // accessPoint is null for "Add network"
                    NetworkInfo networkInfo = accessPoint.getNetworkInfo();
                    if (networkInfo == null || !networkInfo.isConnected()) {
                        mWifiManager.connect(config, null /* listener */);
                    }
                }
            }
        }

        Intent resultData = hasPermissionForResult() ? createResultData(config, accessPoint) : null;
        setResult(RESULT_CONNECTED, resultData);
        finish();
    }

    protected Intent createResultData(WifiConfiguration config, AccessPoint accessPoint) {
        Intent result = new Intent();
        if (accessPoint != null) {
            Bundle accessPointState = new Bundle();
            accessPoint.saveWifiState(accessPointState);
            result.putExtra(KEY_ACCESS_POINT_STATE, accessPointState);
        }
        if (config != null) {
            result.putExtra(KEY_WIFI_CONFIGURATION, config);
        }
        return result;
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        mDialog2 = null;
        mDialog = null;
        finish();
    }

    @Override
    public void onScan(WifiDialog2 dialog, String ssid) {
        Intent intent = WifiDppUtils.getEnrolleeQrCodeScannerIntent(dialog.getContext(), ssid);
        WizardManagerHelper.copyWizardManagerExtras(mIntent, intent);

        // Launch QR code scanner to join a network.
        startActivityForResult(intent, REQUEST_CODE_WIFI_DPP_ENROLLEE_QR_CODE_SCANNER);
    }

    @Override
    public void onScan(WifiDialog dialog, String ssid) {
        Intent intent = WifiDppUtils.getEnrolleeQrCodeScannerIntent(dialog.getContext(), ssid);
        WizardManagerHelper.copyWizardManagerExtras(mIntent, intent);

        // Launch QR code scanner to join a network.
        startActivityForResult(intent, REQUEST_CODE_WIFI_DPP_ENROLLEE_QR_CODE_SCANNER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_WIFI_DPP_ENROLLEE_QR_CODE_SCANNER) {
            if (resultCode != RESULT_OK) {
                return;
            }
            if (hasPermissionForResult()) {
                setResult(RESULT_CONNECTED, data);
            } else {
                setResult(RESULT_CONNECTED);
            }
            finish();
        }
    }

    @VisibleForTesting
    boolean isConfigWifiAllowed() {
        UserManager userManager = getSystemService(UserManager.class);
        if (userManager == null) return true;
        final boolean isConfigWifiAllowed = !userManager.hasUserRestriction(DISALLOW_CONFIG_WIFI);
        if (!isConfigWifiAllowed) {
            Log.e(TAG, "The user is not allowed to configure Wi-Fi.");
            EventLog.writeEvent(0x534e4554, "226133034", getApplicationContext().getUserId(),
                    "The user is not allowed to configure Wi-Fi.");
        }
        return isConfigWifiAllowed;
    }

    @VisibleForTesting
    boolean isAddWifiConfigAllowed() {
        UserManager userManager = getSystemService(UserManager.class);
        if (userManager != null && userManager.hasUserRestriction(DISALLOW_ADD_WIFI_CONFIG)) {
            Log.e(TAG, "The user is not allowed to add Wi-Fi configuration.");
            return false;
        }
        return true;
    }

    private boolean hasWifiManager() {
        if (mWifiManager != null) return true;
        mWifiManager = getSystemService(WifiManager.class);
        return (mWifiManager != null);
    }

    protected boolean hasPermissionForResult() {
        final String callingPackage = getCallingPackage();
        if (callingPackage == null) {
            Log.d(TAG, "Failed to get the calling package, don't return the result.");
            EventLog.writeEvent(0x534e4554, "185126813", -1 /* UID */, "no calling package");
            return false;
        }

        if (getPackageManager().checkPermission(ACCESS_FINE_LOCATION, callingPackage)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "The calling package has ACCESS_FINE_LOCATION permission for result.");
            return true;
        }

        Log.d(TAG, "The calling package does not have the necessary permissions for result.");
        try {
            EventLog.writeEvent(0x534e4554, "185126813",
                    getPackageManager().getPackageUid(callingPackage, 0 /* flags */),
                    "no permission");
        } catch (PackageManager.NameNotFoundException e) {
            EventLog.writeEvent(0x534e4554, "185126813", -1 /* UID */, "no permission");
            Log.w(TAG, "Cannot find the UID, calling package: " + callingPackage, e);
        }
        return false;
    }

    void dismissDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        if (mDialog2 != null) {
            mDialog2.dismiss();
            mDialog2 = null;
        }
    }

    @VisibleForTesting
    static final class LockScreenMonitor implements KeyguardManager.KeyguardLockedStateListener {
        private final WeakReference<WifiDialogActivity> mWifiDialogActivity;
        private KeyguardManager mKeyguardManager;

        LockScreenMonitor(WifiDialogActivity activity) {
            mWifiDialogActivity = new WeakReference<>(activity);
            mKeyguardManager = activity.getSystemService(KeyguardManager.class);
            mKeyguardManager.addKeyguardLockedStateListener(activity.getMainExecutor(), this);
        }

        void release() {
            if (mKeyguardManager == null) return;
            mKeyguardManager.removeKeyguardLockedStateListener(this);
            mKeyguardManager = null;
        }

        @Override
        public void onKeyguardLockedStateChanged(boolean isKeyguardLocked) {
            if (!isKeyguardLocked) return;
            WifiDialogActivity activity = mWifiDialogActivity.get();
            if (activity == null) return;
            activity.dismissDialog();

            Log.e(TAG, "Dismiss Wi-Fi dialog to prevent leaking user data on lock screen!");
            EventLog.writeEvent(0x534e4554, "231583603", -1 /* UID */,
                    "Leak Wi-Fi dialog on lock screen");
        }
    }
}
