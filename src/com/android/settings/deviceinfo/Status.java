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

package com.android.settings.deviceinfo;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneStateIntentReceiver;
import com.android.internal.telephony.TelephonyProperties;
import com.android.settings.R;

import java.lang.ref.WeakReference;

/**
 * Display the following information
 * # Phone Number
 * # Network
 * # Roaming
 * # IMEI
 * # Network type
 * # Signal Strength
 * # Battery Strength  : TODO
 * # Uptime
 * # Awake Time
 * # XMPP/buzz/tickle status : TODO
 *
 */
public class Status extends PreferenceActivity {

    private static final String KEY_WIFI_MAC_ADDRESS = "wifi_mac_address";
    private static final String KEY_BT_ADDRESS = "bt_address";
    private static final int EVENT_SIGNAL_STRENGTH_CHANGED = 200;
    private static final int EVENT_SERVICE_STATE_CHANGED = 300;

    private static final int EVENT_UPDATE_STATS = 500;

    private TelephonyManager mTelephonyManager;
    private Phone mPhone = null;
    private PhoneStateIntentReceiver mPhoneStateReceiver;
    private Resources mRes;
    private Preference mSignalStrength;
    private Preference mUptime;
    private Preference mAwakeTime;

    private static String sUnknown;
    
    private Preference mBatteryStatus;
    private Preference mBatteryLevel;

    private Handler mHandler;

    private static class MyHandler extends Handler {
        private WeakReference<Status> mStatus;

        public MyHandler(Status activity) {
            mStatus = new WeakReference<Status>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            Status status = mStatus.get();
            if (status == null) {
                return;
            }

            switch (msg.what) {
                case EVENT_SIGNAL_STRENGTH_CHANGED:
                    status.updateSignalStrength();
                    break;

                case EVENT_SERVICE_STATE_CHANGED:
                    ServiceState serviceState = status.mPhoneStateReceiver.getServiceState();
                    status.updateServiceState(serviceState);
                    break;

                case EVENT_UPDATE_STATS:
                    status.updateTimes();
                    sendEmptyMessageDelayed(EVENT_UPDATE_STATS, 1000);
                    break;
            }
        }
    }

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {

                int level = intent.getIntExtra("level", 0);
                int scale = intent.getIntExtra("scale", 100);
                
                mBatteryLevel.setSummary(String.valueOf(level * 100 / scale) + "%");
                
                int plugType = intent.getIntExtra("plugged", 0);
                int status = intent.getIntExtra("status", BatteryManager.BATTERY_STATUS_UNKNOWN);
                String statusString;
                if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                    statusString = getString(R.string.battery_info_status_charging);
                    if (plugType > 0) {
                        statusString = statusString + " " + getString(
                                (plugType == BatteryManager.BATTERY_PLUGGED_AC)
                                        ? R.string.battery_info_status_charging_ac
                                        : R.string.battery_info_status_charging_usb);
                    }
                } else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
                    statusString = getString(R.string.battery_info_status_discharging);
                } else if (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
                    statusString = getString(R.string.battery_info_status_not_charging);
                } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
                    statusString = getString(R.string.battery_info_status_full);
                } else {
                    statusString = getString(R.string.battery_info_status_unknown);
                }
                mBatteryStatus.setSummary(statusString);
            }
        }
    };

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onDataConnectionStateChanged(int state) {
            updateDataState();
            updateNetworkType();
        }
    };
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mHandler = new MyHandler(this);
        
        mTelephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);

        addPreferencesFromResource(R.xml.device_info_status);
        mBatteryLevel = findPreference("battery_level");
        mBatteryStatus = findPreference("battery_status");
        
        mRes = getResources();
        if (sUnknown == null) {
            sUnknown = mRes.getString(R.string.device_info_default);
        }
        
        mPhone = PhoneFactory.getDefaultPhone();
        mSignalStrength = findPreference("signal_strength");
        mUptime = findPreference("up_time");
        mAwakeTime = findPreference("awake_time");
        
        setSummaryText("imei", mPhone.getDeviceId());
        setSummaryText("imei_sv",
                ((TelephonyManager) getSystemService(TELEPHONY_SERVICE))
                        .getDeviceSoftwareVersion());
        setSummaryText("number", mPhone.getLine1Number());

        mPhoneStateReceiver = new PhoneStateIntentReceiver(this, mHandler);
        mPhoneStateReceiver.notifySignalStrength(EVENT_SIGNAL_STRENGTH_CHANGED);
        mPhoneStateReceiver.notifyServiceState(EVENT_SERVICE_STATE_CHANGED);
        
        setWifiStatus();
        setBtStatus();
    }
    
    @Override
    protected void onResume() {
        super.onResume();

        mPhoneStateReceiver.registerIntent();
        registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        
        updateSignalStrength();
        updateServiceState(mPhone.getServiceState());
        updateDataState();
        
        mTelephonyManager.listen(mPhoneStateListener,
                  PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);

        mHandler.sendEmptyMessage(EVENT_UPDATE_STATS);
    }
    
    @Override
    public void onPause() {
        super.onPause();

        mPhoneStateReceiver.unregisterIntent();
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        unregisterReceiver(mBatteryInfoReceiver);
        mHandler.removeMessages(EVENT_UPDATE_STATS);
    }

    /**
     * @param preference The key for the Preference item
     * @param property The system property to fetch
     * @param alt The default value, if the property doesn't exist
     */
    private void setSummary(String preference, String property, String alt) {
        try {
            findPreference(preference).setSummary(
                    SystemProperties.get(property, alt));
        } catch (RuntimeException e) {
            
        }
    }

    private void setSummaryText(String preference, String text) {
        if (TextUtils.isEmpty(text)) {
            text = sUnknown;
        }
        
        findPreference(preference).setSummary(text);
    }
    
    private void updateNetworkType() {
        // Whether EDGE, UMTS, etc...
        setSummary("network_type", TelephonyProperties.PROPERTY_DATA_NETWORK_TYPE, sUnknown);
    }
    
    private void updateDataState() {
        int state = mTelephonyManager.getDataState();
        String display = mRes.getString(R.string.radioInfo_unknown);

        switch (state) {
            case TelephonyManager.DATA_CONNECTED:
                display = mRes.getString(R.string.radioInfo_data_connected);
                break;
            case TelephonyManager.DATA_SUSPENDED:
                display = mRes.getString(R.string.radioInfo_data_suspended);
                break;
            case TelephonyManager.DATA_CONNECTING:
                display = mRes.getString(R.string.radioInfo_data_connecting);
                break;
            case TelephonyManager.DATA_DISCONNECTED:
                display = mRes.getString(R.string.radioInfo_data_disconnected);
                break;
        }
        
        setSummaryText("data_state", display);
    }

    private void updateServiceState(ServiceState serviceState) {
        int state = serviceState.getState();
        String display = mRes.getString(R.string.radioInfo_unknown);
        
        switch (state) {
            case ServiceState.STATE_IN_SERVICE:
                display = mRes.getString(R.string.radioInfo_service_in);
                break;
            case ServiceState.STATE_OUT_OF_SERVICE:
            case ServiceState.STATE_EMERGENCY_ONLY:
                display = mRes.getString(R.string.radioInfo_service_out);
                break;
            case ServiceState.STATE_POWER_OFF:
                display = mRes.getString(R.string.radioInfo_service_off);
                break;
        }
        
        setSummaryText("service_state", display);
        
        if (serviceState.getRoaming()) {
            setSummaryText("roaming_state", mRes.getString(R.string.radioInfo_roaming_in));
        } else {
            setSummaryText("roaming_state", mRes.getString(R.string.radioInfo_roaming_not));
        }
        setSummaryText("operator_name", serviceState.getOperatorAlphaLong());
    }
    
    void updateSignalStrength() {
        int state =
                mPhoneStateReceiver.getServiceState().getState();
        Resources r = getResources();

        if ((ServiceState.STATE_OUT_OF_SERVICE == state) ||
                (ServiceState.STATE_POWER_OFF == state)) {
            mSignalStrength.setSummary("0");
        }
        
        int signalDbm = mPhoneStateReceiver.getSignalStrengthDbm();
        
        if (-1 == signalDbm) signalDbm = 0;

        int signalAsu = mPhoneStateReceiver.getSignalStrength();

        if (-1 == signalAsu) signalAsu = 0;

        mSignalStrength.setSummary(String.valueOf(signalDbm) + " "
                    + r.getString(R.string.radioInfo_display_dbm) + "   "
                    + String.valueOf(signalAsu) + " "
                    + r.getString(R.string.radioInfo_display_asu));
    }
    
    private void setWifiStatus() {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        Preference wifiMacAddressPref = findPreference(KEY_WIFI_MAC_ADDRESS);
        String macAddress = wifiInfo == null ? null : wifiInfo.getMacAddress();
        wifiMacAddressPref.setSummary(!TextUtils.isEmpty(macAddress) ? macAddress 
                : getString(R.string.status_unavailable));
    }

    private void setBtStatus() {
        BluetoothDevice bluetooth = (BluetoothDevice) getSystemService(BLUETOOTH_SERVICE);
        Preference btAddressPref = findPreference(KEY_BT_ADDRESS);

        if (bluetooth == null) {
            // device not BT capable
            getPreferenceScreen().removePreference(btAddressPref);
        } else {
            String address = bluetooth.isEnabled() ? bluetooth.getAddress() : null;
            btAddressPref.setSummary(!TextUtils.isEmpty(address) ? address
                    : getString(R.string.status_unavailable));
        }
    }

    void updateTimes() {
        long at = SystemClock.uptimeMillis() / 1000;
        long ut = SystemClock.elapsedRealtime() / 1000;
        long st = ut - at;

        if (ut == 0) {
            ut = 1;
        }

        mUptime.setSummary(convert(ut));
        mAwakeTime.setSummary(convert(at) + " (" + (((1000 * at / ut) + 5) / 10) + "%)");
    }
    
    private String pad(int n) {
        if (n >= 10) {
            return String.valueOf(n);
        } else {
            return "0" + String.valueOf(n);
        }
    }

    private String convert(long t) {
        int s = (int)(t % 60);
        int m = (int)((t / 60) % 60);
        int h = (int)((t / 3600));

        return h + ":" + pad(m) + ":" + pad(s);
    }
}
