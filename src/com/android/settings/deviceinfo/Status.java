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

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.telephony.CellBroadcastMessage;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneStateIntentReceiver;
import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.Utils;

import java.lang.ref.WeakReference;

/**
 * Display the following information
 * # Phone Number
 * # Network
 * # Roaming
 * # Device Id (IMEI in GSM and MEID in CDMA)
 * # Network type
 * # Operator info (area info cell broadcast for Brazil)
 * # Signal Strength
 * # Battery Strength  : TODO
 * # Uptime
 * # Awake Time
 * # XMPP/buzz/tickle status : TODO
 *
 */
public class Status extends PreferenceActivity {

    private static final String KEY_DATA_STATE = "data_state";
    private static final String KEY_SERVICE_STATE = "service_state";
    private static final String KEY_OPERATOR_NAME = "operator_name";
    private static final String KEY_ROAMING_STATE = "roaming_state";
    private static final String KEY_NETWORK_TYPE = "network_type";
    private static final String KEY_LATEST_AREA_INFO = "latest_area_info";
    private static final String KEY_PHONE_NUMBER = "number";
    private static final String KEY_IMEI_SV = "imei_sv";
    private static final String KEY_IMEI = "imei";
    private static final String KEY_PRL_VERSION = "prl_version";
    private static final String KEY_MIN_NUMBER = "min_number";
    private static final String KEY_MEID_NUMBER = "meid_number";
    private static final String KEY_SIGNAL_STRENGTH = "signal_strength";
    private static final String KEY_BATTERY_STATUS = "battery_status";
    private static final String KEY_BATTERY_LEVEL = "battery_level";
    private static final String KEY_IP_ADDRESS = "wifi_ip_address";
    private static final String KEY_WIFI_MAC_ADDRESS = "wifi_mac_address";
    private static final String KEY_BT_ADDRESS = "bt_address";
    private static final String KEY_SERIAL_NUMBER = "serial_number";
    private static final String KEY_ICC_ID = "icc_id";
    private static final String KEY_WIMAX_MAC_ADDRESS = "wimax_mac_address";
    private static final String[] PHONE_RELATED_ENTRIES = {
        KEY_DATA_STATE,
        KEY_SERVICE_STATE,
        KEY_OPERATOR_NAME,
        KEY_ROAMING_STATE,
        KEY_NETWORK_TYPE,
        KEY_LATEST_AREA_INFO,
        KEY_PHONE_NUMBER,
        KEY_IMEI,
        KEY_IMEI_SV,
        KEY_PRL_VERSION,
        KEY_MIN_NUMBER,
        KEY_MEID_NUMBER,
        KEY_SIGNAL_STRENGTH,
        KEY_ICC_ID
    };

    static final String CB_AREA_INFO_RECEIVED_ACTION =
            "android.cellbroadcastreceiver.CB_AREA_INFO_RECEIVED";

    static final String GET_LATEST_CB_AREA_INFO_ACTION =
            "android.cellbroadcastreceiver.GET_LATEST_CB_AREA_INFO";

    // Require the sender to have this permission to prevent third-party spoofing.
    static final String CB_AREA_INFO_SENDER_PERMISSION =
            "android.permission.RECEIVE_EMERGENCY_BROADCAST";

    // Broadcasts to listen to for connectivity changes.
    private static final String[] CONNECTIVITY_INTENTS = {
            BluetoothAdapter.ACTION_STATE_CHANGED,
            ConnectivityManager.CONNECTIVITY_ACTION_IMMEDIATE,
            WifiManager.LINK_CONFIGURATION_CHANGED_ACTION,
            WifiManager.NETWORK_STATE_CHANGED_ACTION,
    };

    private static final int EVENT_SIGNAL_STRENGTH_CHANGED = 200;
    private static final int EVENT_SERVICE_STATE_CHANGED = 300;

    private static final int EVENT_UPDATE_STATS = 500;

    private static final int EVENT_UPDATE_CONNECTIVITY = 600;

    private ConnectivityManager mCM;
    private TelephonyManager mTelephonyManager;
    private WifiManager mWifiManager;

    private Phone mPhone = null;
    private PhoneStateIntentReceiver mPhoneStateReceiver;
    private Resources mRes;
    private boolean mShowLatestAreaInfo;

    private String mUnknown;
    private String mUnavailable;

    private Preference mSignalStrength;
    private Preference mUptime;
    private Preference mBatteryStatus;
    private Preference mBatteryLevel;
    private Preference mBtAddress;
    private Preference mIpAddress;
    private Preference mWifiMacAddress;
    private Preference mWimaxMacAddress;

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

                case EVENT_UPDATE_CONNECTIVITY:
                    status.updateConnectivity();
                    break;
            }
        }
    }

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                mBatteryLevel.setSummary(Utils.getBatteryPercentage(intent));
                mBatteryStatus.setSummary(Utils.getBatteryStatus(getResources(), intent));
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

    private BroadcastReceiver mAreaInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (CB_AREA_INFO_RECEIVED_ACTION.equals(action)) {
                Bundle extras = intent.getExtras();
                if (extras == null) {
                    return;
                }
                CellBroadcastMessage cbMessage = (CellBroadcastMessage) extras.get("message");
                if (cbMessage != null && cbMessage.getServiceCategory() == 50) {
                    String latestAreaInfo = cbMessage.getMessageBody();
                    updateAreaInfo(latestAreaInfo);
                }
            }
        }
    };

    private IntentFilter mConnectivityIntentFilter;
    private final BroadcastReceiver mConnectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ArrayUtils.contains(CONNECTIVITY_INTENTS, action)) {
                mHandler.sendEmptyMessage(EVENT_UPDATE_CONNECTIVITY);
            }
        }
    };

    private boolean hasBluetooth() {
        return BluetoothAdapter.getDefaultAdapter() != null;
    }

    private boolean hasWimax() {
        return  mCM.getNetworkInfo(ConnectivityManager.TYPE_WIMAX) != null;
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mHandler = new MyHandler(this);

        mCM = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        mTelephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        addPreferencesFromResource(R.xml.device_info_status);
        mBatteryLevel = findPreference(KEY_BATTERY_LEVEL);
        mBatteryStatus = findPreference(KEY_BATTERY_STATUS);
        mBtAddress = findPreference(KEY_BT_ADDRESS);
        mWifiMacAddress = findPreference(KEY_WIFI_MAC_ADDRESS);
        mWimaxMacAddress = findPreference(KEY_WIMAX_MAC_ADDRESS);
        mIpAddress = findPreference(KEY_IP_ADDRESS);

        mRes = getResources();
        mUnknown = mRes.getString(R.string.device_info_default);
        mUnavailable = mRes.getString(R.string.status_unavailable);

        if (UserHandle.myUserId() == UserHandle.USER_OWNER) {
            mPhone = PhoneFactory.getDefaultPhone();
        }
        // Note - missing in zaku build, be careful later...
        mSignalStrength = findPreference(KEY_SIGNAL_STRENGTH);
        mUptime = findPreference("up_time");

        if (mPhone == null || Utils.isWifiOnly(getApplicationContext())) {
            for (String key : PHONE_RELATED_ENTRIES) {
                removePreferenceFromScreen(key);
            }
        } else {
            // NOTE "imei" is the "Device ID" since it represents
            //  the IMEI in GSM and the MEID in CDMA
            if (mPhone.getPhoneName().equals("CDMA")) {
                setSummaryText(KEY_MEID_NUMBER, mPhone.getMeid());
                setSummaryText(KEY_MIN_NUMBER, mPhone.getCdmaMin());
                if (getResources().getBoolean(R.bool.config_msid_enable)) {
                    findPreference(KEY_MIN_NUMBER).setTitle(R.string.status_msid_number);
                }
                setSummaryText(KEY_PRL_VERSION, mPhone.getCdmaPrlVersion());
                removePreferenceFromScreen(KEY_IMEI_SV);

                if (mPhone.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE) {
                    // Show ICC ID and IMEI for LTE device
                    setSummaryText(KEY_ICC_ID, mPhone.getIccSerialNumber());
                    setSummaryText(KEY_IMEI, mPhone.getImei());
                } else {
                    // device is not GSM/UMTS, do not display GSM/UMTS features
                    // check Null in case no specified preference in overlay xml
                    removePreferenceFromScreen(KEY_IMEI);
                    removePreferenceFromScreen(KEY_ICC_ID);
                }
            } else {
                setSummaryText(KEY_IMEI, mPhone.getDeviceId());

                setSummaryText(KEY_IMEI_SV,
                        ((TelephonyManager) getSystemService(TELEPHONY_SERVICE))
                            .getDeviceSoftwareVersion());

                // device is not CDMA, do not display CDMA features
                // check Null in case no specified preference in overlay xml
                removePreferenceFromScreen(KEY_PRL_VERSION);
                removePreferenceFromScreen(KEY_MEID_NUMBER);
                removePreferenceFromScreen(KEY_MIN_NUMBER);
                removePreferenceFromScreen(KEY_ICC_ID);

                // only show area info when SIM country is Brazil
                if ("br".equals(mTelephonyManager.getSimCountryIso())) {
                    mShowLatestAreaInfo = true;
                }
            }

            String rawNumber = mTelephonyManager.getLine1Number();  // may be null or empty
            String formattedNumber = null;
            if (!TextUtils.isEmpty(rawNumber)) {
                formattedNumber = PhoneNumberUtils.formatNumber(rawNumber);
            }
            // If formattedNumber is null or empty, it'll display as "Unknown".
            setSummaryText(KEY_PHONE_NUMBER, formattedNumber);

            mPhoneStateReceiver = new PhoneStateIntentReceiver(this, mHandler);
            mPhoneStateReceiver.notifySignalStrength(EVENT_SIGNAL_STRENGTH_CHANGED);
            mPhoneStateReceiver.notifyServiceState(EVENT_SERVICE_STATE_CHANGED);

            if (!mShowLatestAreaInfo) {
                removePreferenceFromScreen(KEY_LATEST_AREA_INFO);
            }
        }

        if (!hasBluetooth()) {
            getPreferenceScreen().removePreference(mBtAddress);
            mBtAddress = null;
        }

        if (!hasWimax()) {
            getPreferenceScreen().removePreference(mWimaxMacAddress);
            mWimaxMacAddress = null;
        }

        mConnectivityIntentFilter = new IntentFilter();
        for (String intent: CONNECTIVITY_INTENTS) {
             mConnectivityIntentFilter.addAction(intent);
        }

        updateConnectivity();

        String serial = Build.SERIAL;
        if (serial != null && !serial.equals("")) {
            setSummaryText(KEY_SERIAL_NUMBER, serial);
        } else {
            removePreferenceFromScreen(KEY_SERIAL_NUMBER);
        }

        // Make every pref on this screen copy its data to the clipboard on longpress.
        // Super convenient for capturing the IMEI, MAC addr, serial, etc.
        getListView().setOnItemLongClickListener(
            new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view,
                        int position, long id) {
                    ListAdapter listAdapter = (ListAdapter) parent.getAdapter();
                    Preference pref = (Preference) listAdapter.getItem(position);

                    ClipboardManager cm = (ClipboardManager)
                            getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setText(pref.getSummary());
                    Toast.makeText(
                        Status.this,
                        com.android.internal.R.string.text_copied,
                        Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mPhone != null && !Utils.isWifiOnly(getApplicationContext())) {
            mPhoneStateReceiver.registerIntent();

            updateSignalStrength();
            updateServiceState(mPhone.getServiceState());
            updateDataState();
            mTelephonyManager.listen(mPhoneStateListener,
                    PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
            if (mShowLatestAreaInfo) {
                registerReceiver(mAreaInfoReceiver, new IntentFilter(CB_AREA_INFO_RECEIVED_ACTION),
                        CB_AREA_INFO_SENDER_PERMISSION, null);
                // Ask CellBroadcastReceiver to broadcast the latest area info received
                Intent getLatestIntent = new Intent(GET_LATEST_CB_AREA_INFO_ACTION);
                sendBroadcastAsUser(getLatestIntent, UserHandle.ALL,
                        CB_AREA_INFO_SENDER_PERMISSION);
            }
        }
        registerReceiver(mConnectivityReceiver, mConnectivityIntentFilter,
                         android.Manifest.permission.CHANGE_NETWORK_STATE, null);
        registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        mHandler.sendEmptyMessage(EVENT_UPDATE_STATS);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mPhone != null && !Utils.isWifiOnly(getApplicationContext())) {
            mPhoneStateReceiver.unregisterIntent();
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        if (mShowLatestAreaInfo) {
            unregisterReceiver(mAreaInfoReceiver);
        }
        unregisterReceiver(mBatteryInfoReceiver);
        unregisterReceiver(mConnectivityReceiver);
        mHandler.removeMessages(EVENT_UPDATE_STATS);
    }

    /**
     * Removes the specified preference, if it exists.
     * @param key the key for the Preference item
     */
    private void removePreferenceFromScreen(String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            getPreferenceScreen().removePreference(pref);
        }
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
               text = mUnknown;
             }
             // some preferences may be missing
             if (findPreference(preference) != null) {
                 findPreference(preference).setSummary(text);
             }
    }

    private void updateNetworkType() {
        // Whether EDGE, UMTS, etc...
        String networktype = null;
        if (TelephonyManager.NETWORK_TYPE_UNKNOWN != mTelephonyManager.getNetworkType()) {
            networktype = mTelephonyManager.getNetworkTypeName();
        }
        setSummaryText(KEY_NETWORK_TYPE, networktype);
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

        setSummaryText(KEY_DATA_STATE, display);
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

        setSummaryText(KEY_SERVICE_STATE, display);

        if (serviceState.getRoaming()) {
            setSummaryText(KEY_ROAMING_STATE, mRes.getString(R.string.radioInfo_roaming_in));
        } else {
            setSummaryText(KEY_ROAMING_STATE, mRes.getString(R.string.radioInfo_roaming_not));
        }
        setSummaryText(KEY_OPERATOR_NAME, serviceState.getOperatorAlphaLong());
    }

    private void updateAreaInfo(String areaInfo) {
        if (areaInfo != null) {
            setSummaryText(KEY_LATEST_AREA_INFO, areaInfo);
        }
    }

    void updateSignalStrength() {
        // TODO PhoneStateIntentReceiver is deprecated and PhoneStateListener
        // should probably used instead.

        // not loaded in some versions of the code (e.g., zaku)
        if (mSignalStrength != null) {
            int state =
                    mPhoneStateReceiver.getServiceState().getState();
            Resources r = getResources();

            if ((ServiceState.STATE_OUT_OF_SERVICE == state) ||
                    (ServiceState.STATE_POWER_OFF == state)) {
                mSignalStrength.setSummary("0");
                return;
            }

            int signalDbm = mPhoneStateReceiver.getSignalStrengthDbm();

            if (-1 == signalDbm) signalDbm = 0;

            int signalAsu = mPhoneStateReceiver.getSignalStrengthLevelAsu();

            if (-1 == signalAsu) signalAsu = 0;

            mSignalStrength.setSummary(String.valueOf(signalDbm) + " "
                        + r.getString(R.string.radioInfo_display_dbm) + "   "
                        + String.valueOf(signalAsu) + " "
                        + r.getString(R.string.radioInfo_display_asu));
        }
    }

    private void setWimaxStatus() {
        if (mWimaxMacAddress != null) {
            String macAddress = SystemProperties.get("net.wimax.mac.address", mUnavailable);
            mWimaxMacAddress.setSummary(macAddress);
        }
    }

    private void setWifiStatus() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        String macAddress = wifiInfo == null ? null : wifiInfo.getMacAddress();
        mWifiMacAddress.setSummary(!TextUtils.isEmpty(macAddress) ? macAddress : mUnavailable);
    }

    private void setIpAddressStatus() {
        String ipAddress = Utils.getDefaultIpAddresses(this.mCM);
        if (ipAddress != null) {
            mIpAddress.setSummary(ipAddress);
        } else {
            mIpAddress.setSummary(mUnavailable);
        }
    }

    private void setBtStatus() {
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        if (bluetooth != null && mBtAddress != null) {
            String address = bluetooth.isEnabled() ? bluetooth.getAddress() : null;
            if (!TextUtils.isEmpty(address)) {
               // Convert the address to lowercase for consistency with the wifi MAC address.
                mBtAddress.setSummary(address.toLowerCase());
            } else {
                mBtAddress.setSummary(mUnavailable);
            }
        }
    }

    void updateConnectivity() {
        setWimaxStatus();
        setWifiStatus();
        setBtStatus();
        setIpAddressStatus();
    }

    void updateTimes() {
        long at = SystemClock.uptimeMillis() / 1000;
        long ut = SystemClock.elapsedRealtime() / 1000;

        if (ut == 0) {
            ut = 1;
        }

        mUptime.setSummary(convert(ut));
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
