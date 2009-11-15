/*
 * Copyright (C) 2007 The Android Open Source Project
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

import com.android.internal.telephony.PhoneStateIntentReceiver;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.telephony.ServiceState;

import com.android.internal.telephony.TelephonyProperties;

public class AirplaneModeEnabler implements Preference.OnPreferenceChangeListener {

    private final Context mContext;

    private PhoneStateIntentReceiver mPhoneStateReceiver;
    
    private final CheckBoxPreference mCheckBoxPref;

    private static final int EVENT_SERVICE_STATE_CHANGED = 3;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_SERVICE_STATE_CHANGED:
                    onAirplaneModeChanged();
                    break;
            }
        }
    };

    public AirplaneModeEnabler(Context context, CheckBoxPreference airplaneModeCheckBoxPreference) {
        
        mContext = context;
        mCheckBoxPref = airplaneModeCheckBoxPreference;
        
        airplaneModeCheckBoxPreference.setPersistent(false);
    
        mPhoneStateReceiver = new PhoneStateIntentReceiver(mContext, mHandler);
        mPhoneStateReceiver.notifyServiceState(EVENT_SERVICE_STATE_CHANGED);
    }

    public void resume() {
        
        // This is the widget enabled state, not the preference toggled state
        mCheckBoxPref.setEnabled(true);
        mCheckBoxPref.setChecked(isAirplaneModeOn(mContext));

        mPhoneStateReceiver.registerIntent();
        mCheckBoxPref.setOnPreferenceChangeListener(this);
    }
    
    public void pause() {
        mPhoneStateReceiver.unregisterIntent();
        mCheckBoxPref.setOnPreferenceChangeListener(null);
    }
    
    public static boolean isAirplaneModeOn(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) != 0;
    }

    private void setAirplaneModeOn(boolean enabling) {
        
        mCheckBoxPref.setEnabled(false);
        mCheckBoxPref.setSummary(enabling ? R.string.airplane_mode_turning_on
                : R.string.airplane_mode_turning_off);
        
        // Change the system setting
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 
                                enabling ? 1 : 0);
        
        // Post the intent
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", enabling);
        mContext.sendBroadcast(intent);
    }

    /**
     * Called when we've received confirmation that the airplane mode was set.
     */
    private void onAirplaneModeChanged() {
        ServiceState serviceState = mPhoneStateReceiver.getServiceState();
        boolean airplaneModeEnabled = serviceState.getState() == ServiceState.STATE_POWER_OFF;
        mCheckBoxPref.setChecked(airplaneModeEnabled);
        mCheckBoxPref.setSummary(airplaneModeEnabled ? null : 
                mContext.getString(R.string.airplane_mode_summary));            
        mCheckBoxPref.setEnabled(true);
    }
    
    /**
     * Called when someone clicks on the checkbox preference.
     */
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (Boolean.parseBoolean(
                    SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE))) {
            // In ECM mode, do not update database at this point
        } else {
            setAirplaneModeOn((Boolean) newValue);
        }
        return true;
    }

    public void setAirplaneModeInECM(boolean isECMExit, boolean isAirplaneModeOn) {
        if (isECMExit) {
            // update database based on the current checkbox state
            setAirplaneModeOn(isAirplaneModeOn);
        } else {
            // update checkbox state based on database value
            onAirplaneModeChanged();
        }
    }

}
