/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.mobilenetwork;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;

import com.android.settingslib.utils.ThreadUtils;

import androidx.preference.ListPreference;

public class CdmaSystemSelectListPreference extends ListPreference {

    private static final String LOG_TAG = "CdmaRoamingListPref";
    private static final boolean DBG = false;

    private TelephonyManager mTelephonyManager;
    private MyHandler mHandler = new MyHandler();

    public CdmaSystemSelectListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mHandler = new MyHandler();
        mTelephonyManager = TelephonyManager.from(context);
    }

    public CdmaSystemSelectListPreference(Context context) {
        this(context, null);
    }

    /**
     * Sets the subscription id associated with this preference.
     *
     * @param subId the subscription id.
     */
    public void setSubscriptionId(int subId) {
        mTelephonyManager = TelephonyManager.from(getContext()).createForSubscriptionId(subId);
        queryCdmaRoamingMode();
    }

    //TODO(b/114749736): Move this to preference controller
    protected void showDialog(Bundle state) {
        if (!mTelephonyManager.getEmergencyCallbackMode()) {
            // show Dialog
        }
    }

    //TODO(b/114749736): Move this to preference controller
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult && (getValue() != null)) {
            int buttonCdmaRoamingMode = Integer.parseInt(getValue());
            int settingsCdmaRoamingMode = Settings.Global.getInt(
                    getContext().getContentResolver(),
                    Settings.Global.CDMA_ROAMING_MODE,
                    TelephonyManager.CDMA_ROAMING_MODE_HOME);
            if (buttonCdmaRoamingMode != settingsCdmaRoamingMode) {
                int cdmaRoamingMode = TelephonyManager.CDMA_ROAMING_MODE_ANY;
                if (buttonCdmaRoamingMode != TelephonyManager.CDMA_ROAMING_MODE_ANY) {
                    cdmaRoamingMode = TelephonyManager.CDMA_ROAMING_MODE_HOME;
                }
                //Set the Settings.Secure network mode
                Settings.Global.putInt(
                        getContext().getContentResolver(),
                        Settings.Global.CDMA_ROAMING_MODE,
                        buttonCdmaRoamingMode);
                //Set the roaming preference mode
                setCdmaRoamingMode(cdmaRoamingMode);
            }
        } else {
            Log.d(LOG_TAG, String.format("onDialogClosed: positiveResult=%b value=%s -- do nothing",
                    positiveResult, getValue()));
        }
    }

    private class MyHandler extends Handler {

        static final int MESSAGE_GET_ROAMING_PREFERENCE = 0;
        static final int MESSAGE_SET_ROAMING_PREFERENCE = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_GET_ROAMING_PREFERENCE:
                    handleQueryCdmaRoamingPreference(msg);
                    break;

                case MESSAGE_SET_ROAMING_PREFERENCE:
                    handleSetCdmaRoamingPreference(msg);
                    break;
            }
        }

        private void handleQueryCdmaRoamingPreference(Message msg) {
            int cdmaRoamingMode = msg.arg1;

            if (cdmaRoamingMode != TelephonyManager.CDMA_ROAMING_MODE_RADIO_DEFAULT) {
                int settingsRoamingMode = Settings.Global.getInt(
                        getContext().getContentResolver(),
                        Settings.Global.CDMA_ROAMING_MODE,
                        TelephonyManager.CDMA_ROAMING_MODE_HOME);

                //check that statusCdmaRoamingMode is from an accepted value
                if (cdmaRoamingMode == TelephonyManager.CDMA_ROAMING_MODE_HOME
                        || cdmaRoamingMode == TelephonyManager.CDMA_ROAMING_MODE_ANY) {
                    //check changes in statusCdmaRoamingMode and updates settingsRoamingMode
                    if (cdmaRoamingMode != settingsRoamingMode) {
                        settingsRoamingMode = cdmaRoamingMode;
                        //changes the Settings.Secure accordingly to statusCdmaRoamingMode
                        Settings.Global.putInt(
                                getContext().getContentResolver(),
                                Settings.Global.CDMA_ROAMING_MODE,
                                settingsRoamingMode);
                    }
                    //changes the mButtonPreferredNetworkMode accordingly to modemNetworkMode
                    setValue(Integer.toString(cdmaRoamingMode));
                }
                else {
                    if(DBG) Log.i(LOG_TAG, "reset cdma roaming mode to default" );
                    resetCdmaRoamingModeToDefault();
                }
            }
        }

        private void handleSetCdmaRoamingPreference(Message msg) {
            boolean isSuccessed = (boolean) msg.obj;

            if (isSuccessed && (getValue() != null)) {
                int cdmaRoamingMode = Integer.parseInt(getValue());
                Settings.Global.putInt(
                        getContext().getContentResolver(),
                        Settings.Global.CDMA_ROAMING_MODE,
                        cdmaRoamingMode );
            } else {
                queryCdmaRoamingMode();
            }
        }

        private void resetCdmaRoamingModeToDefault() {
            //set the mButtonCdmaRoam
            setValue(Integer.toString(TelephonyManager.CDMA_ROAMING_MODE_ANY));
            //set the Settings.System
            Settings.Global.putInt(
                    getContext().getContentResolver(),
                    Settings.Global.CDMA_ROAMING_MODE,
                    TelephonyManager.CDMA_ROAMING_MODE_ANY);
            //Set the Status
            setCdmaRoamingMode(TelephonyManager.CDMA_ROAMING_MODE_ANY);
        }
    }

    private void queryCdmaRoamingMode() {
        ThreadUtils.postOnBackgroundThread(() -> {
            Message msg = mHandler.obtainMessage(MyHandler.MESSAGE_GET_ROAMING_PREFERENCE);
            msg.arg1 = mTelephonyManager.getCdmaRoamingMode();
            msg.sendToTarget();
        });
    }

    private void setCdmaRoamingMode(int mode) {
        ThreadUtils.postOnBackgroundThread(() -> {
            Message msg = mHandler.obtainMessage(MyHandler.MESSAGE_SET_ROAMING_PREFERENCE);
            msg.obj = mTelephonyManager.setCdmaRoamingMode(mode);
            msg.sendToTarget();
        });
    }
}
