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

package com.android.settings.wifi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.internal.app.AlertActivity;
import com.android.settings.R;

/**
 * This activity handles requests to toggle WiFi by collecting user
 * consent and waiting until the state change is completed.
 */
public class RequestToggleWiFiActivity extends AlertActivity
        implements DialogInterface.OnClickListener {
    private static final String LOG_TAG = "RequestToggleWiFiActivity";

    private static final long TOGGLE_TIMEOUT_MILLIS = 10000; // 10 sec

    private static final int STATE_UNKNOWN = -1;
    private static final int STATE_ENABLE = 1;
    private static final int STATE_ENABLING = 2;
    private static final int STATE_DISABLE = 3;
    private static final int STATE_DISABLING = 4;

    private final StateChangeReceiver mReceiver = new StateChangeReceiver();

    private final Runnable mTimeoutCommand = () -> {
        if (!isFinishing() && !isDestroyed()) {
            finish();
        }
    };

    private @NonNull WifiManager mWiFiManager;
    private @NonNull CharSequence mAppLabel;

    private int mState = STATE_UNKNOWN;
    private int mLastUpdateState = STATE_UNKNOWN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWiFiManager = getSystemService(WifiManager.class);

        setResult(Activity.RESULT_CANCELED);

        String packageName = getIntent().getStringExtra(Intent.EXTRA_PACKAGE_NAME);
        if (TextUtils.isEmpty(packageName)) {
            finish();
            return;
        }

        try {
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(
                    packageName, 0);
            mAppLabel = applicationInfo.loadSafeLabel(getPackageManager(),
                    PackageItemInfo.DEFAULT_MAX_LABEL_SIZE_PX, PackageItemInfo.SAFE_LABEL_FLAG_TRIM
                            | PackageItemInfo.SAFE_LABEL_FLAG_FIRST_LINE);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, "Couldn't find app with package name " + packageName);
            finish();
            return;
        }

        String action = getIntent().getAction();
        switch (action) {
            case WifiManager.ACTION_REQUEST_ENABLE: {
                mState = STATE_ENABLE;
            } break;

            case WifiManager.ACTION_REQUEST_DISABLE: {
                mState = STATE_DISABLE;
            } break;

            default: {
                finish();
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE: {
                switch (mState) {
                    case STATE_ENABLE: {
                        mWiFiManager.setWifiEnabled(true);
                        mState = STATE_ENABLING;
                        scheduleToggleTimeout();
                        updateUi();
                    } break;

                    case STATE_DISABLE: {
                        mWiFiManager.setWifiEnabled(false);
                        mState = STATE_DISABLING;
                        scheduleToggleTimeout();
                        updateUi();
                    } break;
                }
            }
            break;
            case DialogInterface.BUTTON_NEGATIVE: {
                finish();
            }
            break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        mReceiver.register();

        final int wifiState = mWiFiManager.getWifiState();

        switch (mState) {
            case STATE_ENABLE: {
                switch (wifiState) {
                    case WifiManager.WIFI_STATE_ENABLED: {
                        setResult(RESULT_OK);
                        finish();
                    } return;

                    case WifiManager.WIFI_STATE_ENABLING: {
                        mState = STATE_ENABLING;
                        scheduleToggleTimeout();
                    } break;
                }
            } break;

            case STATE_DISABLE: {
                switch (wifiState) {
                    case WifiManager.WIFI_STATE_DISABLED: {
                        setResult(RESULT_OK);
                        finish();
                    }
                    return;

                    case WifiManager.WIFI_STATE_ENABLING: {
                        mState = STATE_DISABLING;
                        scheduleToggleTimeout();
                    }
                    break;
                }
            } break;

            case STATE_ENABLING: {
                switch (wifiState) {
                    case WifiManager.WIFI_STATE_ENABLED: {
                        setResult(RESULT_OK);
                        finish();
                    } return;

                    case WifiManager.WIFI_STATE_ENABLING: {
                        scheduleToggleTimeout();
                    } break;

                    case WifiManager.WIFI_STATE_DISABLED:
                    case WifiManager.WIFI_STATE_DISABLING: {
                        mState = STATE_ENABLE;
                    } break;
                }
            } break;

            case STATE_DISABLING: {
                switch (wifiState) {
                    case WifiManager.WIFI_STATE_DISABLED: {
                        setResult(RESULT_OK);
                        finish();
                    } return;

                    case WifiManager.WIFI_STATE_DISABLING: {
                        scheduleToggleTimeout();
                    } break;

                    case WifiManager.WIFI_STATE_ENABLED:
                    case WifiManager.WIFI_STATE_ENABLING: {
                        mState = STATE_DISABLE;
                    } break;
                }
            } break;
        }

        updateUi();
    }

    @Override
    protected void onStop() {
        mReceiver.unregister();
        unscheduleToggleTimeout();
        super.onStop();
    }

    private void updateUi() {
        if (mLastUpdateState == mState) {
            return;
        }
        mLastUpdateState = mState;

        switch (mState) {
            case STATE_ENABLE: {
                mAlertParams.mPositiveButtonText = getString(R.string.allow);
                mAlertParams.mPositiveButtonListener = this;
                mAlertParams.mNegativeButtonText = getString(R.string.deny);
                mAlertParams.mNegativeButtonListener = this;
                mAlertParams.mMessage = getString(R.string.wifi_ask_enable, mAppLabel);
            } break;

            case STATE_ENABLING: {
                // Params set button text only if non-null, but we want a null
                // button text to hide the button, so reset the controller directly.
                mAlert.setButton(DialogInterface.BUTTON_POSITIVE, null, null, null);
                mAlert.setButton(DialogInterface.BUTTON_NEGATIVE, null, null, null);
                mAlertParams.mPositiveButtonText = null;
                mAlertParams.mPositiveButtonListener = null;
                mAlertParams.mNegativeButtonText = null;
                mAlertParams.mNegativeButtonListener = null;
                mAlertParams.mMessage = getString(R.string.wifi_starting);
            } break;

            case STATE_DISABLE: {
                mAlertParams.mPositiveButtonText = getString(R.string.allow);
                mAlertParams.mPositiveButtonListener = this;
                mAlertParams.mNegativeButtonText = getString(R.string.deny);
                mAlertParams.mNegativeButtonListener = this;
                mAlertParams.mMessage = getString(R.string.wifi_ask_disable, mAppLabel);
            } break;

            case STATE_DISABLING: {
                // Params set button text only if non-null, but we want a null
                // button text to hide the button, so reset the controller directly.
                mAlert.setButton(DialogInterface.BUTTON_POSITIVE, null, null, null);
                mAlert.setButton(DialogInterface.BUTTON_NEGATIVE, null, null, null);
                mAlertParams.mPositiveButtonText = null;
                mAlertParams.mPositiveButtonListener = null;
                mAlertParams.mNegativeButtonText = null;
                mAlertParams.mNegativeButtonListener = null;
                mAlertParams.mMessage = getString(R.string.wifi_stopping);
            } break;
        }

        setupAlert();
    }

    @Override
    public void dismiss() {
        // Clicking on the dialog buttons dismisses the dialog and finishes
        // the activity but we want to finish after the WiFi state changed.
    }

    private void scheduleToggleTimeout() {
        getWindow().getDecorView().postDelayed(mTimeoutCommand, TOGGLE_TIMEOUT_MILLIS);
    }

    private void unscheduleToggleTimeout() {
        getWindow().getDecorView().removeCallbacks(mTimeoutCommand);
    }

    private final class StateChangeReceiver extends BroadcastReceiver {
        private final IntentFilter mFilter = new IntentFilter(
                WifiManager.WIFI_STATE_CHANGED_ACTION);

        public void register() {
            registerReceiver(this, mFilter);
        }

        public void unregister() {
            unregisterReceiver(this);
        }

        public void onReceive(Context context, Intent intent) {
            Activity activity = RequestToggleWiFiActivity.this;
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            final int currentState = mWiFiManager.getWifiState();
            switch (currentState) {
                case WifiManager.WIFI_STATE_ENABLED:
                case WifiManager.WIFI_STATE_DISABLED: {
                    if (mState == STATE_ENABLING || mState == STATE_DISABLING) {
                        RequestToggleWiFiActivity.this.setResult(Activity.RESULT_OK);
                        finish();
                    }
                } break;
            }
        }
    }
}
