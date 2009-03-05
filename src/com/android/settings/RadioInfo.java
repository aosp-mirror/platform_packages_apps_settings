/*
 * Copyright (C) 2006 The Android Open Source Project
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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.INetStatService;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.NeighboringCellInfo;
import android.telephony.gsm.GsmCellLocation;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.EditText;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneStateIntentReceiver;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.gsm.GSMPhone;
import com.android.internal.telephony.gsm.PdpConnection;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class RadioInfo extends Activity {
    private final String TAG = "phone";
    
    private static final int EVENT_PHONE_STATE_CHANGED = 100;
    private static final int EVENT_SIGNAL_STRENGTH_CHANGED = 200;
    private static final int EVENT_SERVICE_STATE_CHANGED = 300;
    private static final int EVENT_CFI_CHANGED = 302;

    private static final int EVENT_QUERY_PREFERRED_TYPE_DONE = 1000;
    private static final int EVENT_SET_PREFERRED_TYPE_DONE = 1001;
    private static final int EVENT_QUERY_NEIGHBORING_CIDS_DONE = 1002;
    private static final int EVENT_SET_QXDMLOG_DONE = 1003;
    private static final int EVENT_SET_CIPHER_DONE = 1004;
    private static final int EVENT_QUERY_SMSC_DONE = 1005;
    private static final int EVENT_UPDATE_SMSC_DONE = 1006;

    private static final int MENU_ITEM_SELECT_BAND  = 0;
    private static final int MENU_ITEM_VIEW_ADN     = 1;
    private static final int MENU_ITEM_VIEW_FDN     = 2;
    private static final int MENU_ITEM_VIEW_SDN     = 3;
    private static final int MENU_ITEM_GET_PDP_LIST = 4;
    private static final int MENU_ITEM_TOGGLE_DATA  = 5;
    private static final int MENU_ITEM_TOGGLE_DATA_ON_BOOT = 6;

    private TextView mImei;
    private TextView number;
    private TextView callState;
    private TextView operatorName;
    private TextView roamingState;
    private TextView gsmState;
    private TextView gprsState;
    private TextView network;
    private TextView dBm;
    private TextView mMwi;
    private TextView mCfi;
    private TextView mLocation;
    private TextView mNeighboringCids;
    private TextView resets;
    private TextView attempts;
    private TextView successes;
    private TextView disconnects;
    private TextView sentSinceReceived;
    private TextView sent;
    private TextView received;
    private TextView mPingIpAddr;
    private TextView mPingHostname;
    private TextView mHttpClientTest;
    private TextView cipherState;
    private TextView dnsCheckState;
    private EditText smsc;
    private Button radioPowerButton;
    private Button qxdmLogButton;
    private Button cipherToggleButton;
    private Button dnsCheckToggleButton;
    private Button pingTestButton;
    private Button updateSmscButton;
    private Button refreshSmscButton;
    private Spinner preferredNetworkType;

    private TelephonyManager mTelephonyManager;
    private Phone phone = null;
    private PhoneStateIntentReceiver mPhoneStateReceiver;
    private INetStatService netstat;

    private OemCommands mOem = null;
    private boolean mQxdmLogEnabled;
    // The requested cipher state
    private boolean mCipherOn;

    private String mPingIpAddrResult;
    private String mPingHostnameResult;
    private String mHttpClientTestResult;
    private boolean mMwiValue = false;
    private boolean mCfiValue = false;

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onDataConnectionStateChanged(int state) {
            updateDataState();
            updateDataStats();
            updatePdpList();
            updateNetworkType();
        }

        @Override
        public void onDataActivity(int direction) {
            updateDataStats2();
        }

        @Override
        public void onCellLocationChanged(CellLocation location) {
            updateLocation(location);
        }

        @Override
        public void onMessageWaitingIndicatorChanged(boolean mwi) {
            mMwiValue = mwi;
            updateMessageWaiting();
        }

        @Override
        public void onCallForwardingIndicatorChanged(boolean cfi) {
            mCfiValue = cfi;
            updateCallRedirect();
        }
    };

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
                case EVENT_PHONE_STATE_CHANGED:
                    updatePhoneState();
                    break;

                case EVENT_SIGNAL_STRENGTH_CHANGED:
                    updateSignalStrength();
                    break;

                case EVENT_SERVICE_STATE_CHANGED:
                    updateServiceState();
                    updatePowerState();
                    break;

                case EVENT_QUERY_PREFERRED_TYPE_DONE:
                    ar= (AsyncResult) msg.obj;
                    if (ar.exception == null) {
                        int type = ((int[])ar.result)[0];
                        preferredNetworkType.setSelection(type, true);
                    } else {
                        preferredNetworkType.setSelection(3, true);
                    }
                    break;
                case EVENT_SET_PREFERRED_TYPE_DONE:
                    ar= (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        phone.getPreferredNetworkType(
                                obtainMessage(EVENT_QUERY_PREFERRED_TYPE_DONE));
                    }
                    break;
                case EVENT_QUERY_NEIGHBORING_CIDS_DONE:
                    ar= (AsyncResult) msg.obj;
                    if (ar.exception == null) {
                        updateNeighboringCids((ArrayList<NeighboringCellInfo>)ar.result);
                    } else {
                        mNeighboringCids.setText("unknown");
                    }
                    break;
                case EVENT_SET_QXDMLOG_DONE:
                    ar= (AsyncResult) msg.obj;
                    if (ar.exception == null) {
                        mQxdmLogEnabled = !mQxdmLogEnabled;
                        
                        updateQxdmState(mQxdmLogEnabled);
                        displayQxdmEnableResult();
                    }
                    break;
                case EVENT_SET_CIPHER_DONE:
                    ar= (AsyncResult) msg.obj;
                    if (ar.exception == null) {
                        setCiphPref(mCipherOn);
                    }
                    updateCiphState();
                    break;
                case EVENT_QUERY_SMSC_DONE:
                    ar= (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        smsc.setText("refresh error");
                    } else {
                        byte[] buf = (byte[]) ar.result;
                        smsc.setText(new String(buf));
                    }
                    break;
                case EVENT_UPDATE_SMSC_DONE:
                    updateSmscButton.setEnabled(true);
                    ar= (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        smsc.setText("update error");
                    }
                    break;
                default:
                    break;

            }
        }
    };

    private class OemCommands {

        public  final int OEM_QXDM_SDLOG_DEFAULT_FILE_SIZE = 32;
        public  final int OEM_QXDM_SDLOG_DEFAULT_MASK = 0;
        public  final int OEM_QXDM_SDLOG_DEFAULT_MAX_INDEX = 8;

        final int SIZE_OF_INT = 4;
        final int OEM_FEATURE_ENABLE = 1;
        final int OEM_FEATURE_DISABLE = 0;
        final int OEM_SIMPE_FEAUTURE_LEN = 1;

        final int OEM_QXDM_SDLOG_FUNCTAG = 0x00010000;
        final int OEM_QXDM_SDLOG_LEN = 4;
        final int OEM_PS_AUTO_ATTACH_FUNCTAG = 0x00020000;
        final int OEM_CIPHERING_FUNCTAG = 0x00020001;
        final int OEM_SMSC_UPDATE_FUNCTAG = 0x00020002;
        final int OEM_SMSC_QUERY_FUNCTAG = 0x00020003;
        final int OEM_SMSC_QUERY_LEN = 0;
        
        /**
         * The OEM interface to store QXDM to SD.
         *
         * To start/stop logging QXDM logs to SD card, use tag
         * OEM_RIL_HOOK_QXDM_SD_LOG_SETUP 0x00010000
         *
         * "data" is a const oem_ril_hook_qxdm_sdlog_setup_data_st *
         * ((const oem_ril_hook_qxdm_sdlog_setup_data_st *)data)->head.func_tag
         * should be OEM_RIL_HOOK_QXDM_SD_LOG_SETUP
         * ((const oem_ril_hook_qxdm_sdlog_setup_data_st *)data)->head.len
         * should be "sizeof(unsigned int) * 4"
         * ((const oem_ril_hook_qxdm_sdlog_setup_data_st *)data)->mode
         * could be 0 for 'stop logging', or 1 for 'start logging'
         * ((const oem_ril_hook_qxdm_sdlog_setup_data_st *)data)->log_file_size
         * will assign the size of each log file, and it could be a value between
         * 1 and 512 (in megabytes, default value is recommended to set as 32).
         * This value will be ignored when mode == 0.
         * ((const oem_ril_hook_qxdm_sdlog_setup_data_st *)data)->log_mask will
         * assign the rule to filter logs, and it is a bitmask (bit0 is for MsgAll,
         * bit1 is for LogAll, and bit2 is for EventAll) recommended to be set as 0
         * by default. This value will be ignored when mode == 0.
         * ((const oem_ril_hook_qxdm_sdlog_setup_data_st *)data)->log_max_fileindex
         * set the how many logfiles will storted before roll over. This value will
         * be ignored when mode == 0.
         *
         * "response" is NULL
         *
         * typedef struct _oem_ril_hook_raw_head_st {
         *      unsigned int func_tag;
         *      unsigned int len;
         * } oem_ril_hook_raw_head_st;
         *
         * typedef struct _oem_ril_hook_qxdm_sdlog_setup_data_st {
         *      oem_ril_hook_raw_head_st head;
         *      unsigned int mode;
         *      unsigned int log_file_size;
         *      unsigned int log_mask;
         *      unsigned int log_max_fileindex;
         * } oem_ril_hook_qxdm_sdlog_setup_data_st;
         *
         * @param enable set true to start logging QXDM in SD card
         * @param fileSize is the log file size in MB
         * @param mask is the log mask to filter
         * @param maxIndex is the maximum roll-over file number
         * @return byteArray to use in RIL RAW command
         */
        byte[] getQxdmSdlogData(boolean enable, int fileSize, int mask, int maxIndex) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            try {
                writeIntLittleEndian(dos, OEM_QXDM_SDLOG_FUNCTAG);
                writeIntLittleEndian(dos, OEM_QXDM_SDLOG_LEN * SIZE_OF_INT);
                writeIntLittleEndian(dos, enable ?
                        OEM_FEATURE_ENABLE : OEM_FEATURE_DISABLE);
                writeIntLittleEndian(dos, fileSize);
                writeIntLittleEndian(dos, mask);
                writeIntLittleEndian(dos, maxIndex);
            } catch (IOException e) {
                return null;
            }
            return bos.toByteArray();
        }

        byte[] getSmscQueryData() {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            try {
                writeIntLittleEndian(dos, OEM_SMSC_QUERY_FUNCTAG);
                writeIntLittleEndian(dos, OEM_SMSC_QUERY_LEN * SIZE_OF_INT);
            } catch (IOException e) {
                return null;
            }
            return bos.toByteArray();
        }

        byte[] getSmscUpdateData(String smsc) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            try {
                byte[] smsc_bytes = smsc.getBytes();
                writeIntLittleEndian(dos, OEM_SMSC_UPDATE_FUNCTAG);
                writeIntLittleEndian(dos, smsc_bytes.length);
                dos.write(smsc_bytes);
            } catch (IOException e) {
                return null;
            }
            return bos.toByteArray();
        }

        byte[] getPsAutoAttachData(boolean enable) {
            return getSimpleFeatureData(OEM_PS_AUTO_ATTACH_FUNCTAG, enable);
        }

        byte[] getCipheringData(boolean enable) {
            return getSimpleFeatureData(OEM_CIPHERING_FUNCTAG, enable);
        }
        
        private byte[] getSimpleFeatureData(int tag, boolean enable) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            try {
                writeIntLittleEndian(dos, tag);
                writeIntLittleEndian(dos, OEM_SIMPE_FEAUTURE_LEN * SIZE_OF_INT);
                writeIntLittleEndian(dos, enable ?
                        OEM_FEATURE_ENABLE : OEM_FEATURE_DISABLE);
            } catch (IOException e) {
                return null;
            }
            return bos.toByteArray();
        }

        private void writeIntLittleEndian(DataOutputStream dos, int val)
                throws IOException {
            dos.writeByte(val);
            dos.writeByte(val >> 8);
            dos.writeByte(val >> 16);
            dos.writeByte(val >> 24);
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.radio_info);

        mTelephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        phone = PhoneFactory.getDefaultPhone();

        mImei = (TextView) findViewById(R.id.imei);
        number = (TextView) findViewById(R.id.number);
        callState = (TextView) findViewById(R.id.call);
        operatorName = (TextView) findViewById(R.id.operator);
        roamingState = (TextView) findViewById(R.id.roaming);
        gsmState = (TextView) findViewById(R.id.gsm);
        gprsState = (TextView) findViewById(R.id.gprs);
        network = (TextView) findViewById(R.id.network);
        dBm = (TextView) findViewById(R.id.dbm);
        mMwi = (TextView) findViewById(R.id.mwi);
        mCfi = (TextView) findViewById(R.id.cfi);
        mLocation = (TextView) findViewById(R.id.location);
        mNeighboringCids = (TextView) findViewById(R.id.neighboring);

        resets = (TextView) findViewById(R.id.resets);
        attempts = (TextView) findViewById(R.id.attempts);
        successes = (TextView) findViewById(R.id.successes);
        disconnects = (TextView) findViewById(R.id.disconnects);
        sentSinceReceived = (TextView) findViewById(R.id.sentSinceReceived);
        sent = (TextView) findViewById(R.id.sent);
        received = (TextView) findViewById(R.id.received);
        cipherState = (TextView) findViewById(R.id.ciphState);
        smsc = (EditText) findViewById(R.id.smsc);
        dnsCheckState = (TextView) findViewById(R.id.dnsCheckState);

        mPingIpAddr = (TextView) findViewById(R.id.pingIpAddr);
        mPingHostname = (TextView) findViewById(R.id.pingHostname);
        mHttpClientTest = (TextView) findViewById(R.id.httpClientTest);

        preferredNetworkType = (Spinner) findViewById(R.id.preferredNetworkType);
        ArrayAdapter<String> adapter = new ArrayAdapter<String> (this,
                android.R.layout.simple_spinner_item, mPreferredNetworkLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);        
        preferredNetworkType.setAdapter(adapter);
        preferredNetworkType.setOnItemSelectedListener(mPreferredNetworkHandler);

        radioPowerButton = (Button) findViewById(R.id.radio_power);
        radioPowerButton.setOnClickListener(mPowerButtonHandler);

        qxdmLogButton = (Button) findViewById(R.id.qxdm_log);
        qxdmLogButton.setOnClickListener(mQxdmButtonHandler);

        cipherToggleButton = (Button) findViewById(R.id.ciph_toggle);
        cipherToggleButton.setOnClickListener(mCipherButtonHandler);
        pingTestButton = (Button) findViewById(R.id.ping_test);
        pingTestButton.setOnClickListener(mPingButtonHandler);
        updateSmscButton = (Button) findViewById(R.id.update_smsc);
        updateSmscButton.setOnClickListener(mUpdateSmscButtonHandler);
        refreshSmscButton = (Button) findViewById(R.id.refresh_smsc);
        refreshSmscButton.setOnClickListener(mRefreshSmscButtonHandler);
        dnsCheckToggleButton = (Button) findViewById(R.id.dns_check_toggle);
        dnsCheckToggleButton.setOnClickListener(mDnsCheckButtonHandler);
        
        mPhoneStateReceiver = new PhoneStateIntentReceiver(this, mHandler);
        mPhoneStateReceiver.notifySignalStrength(EVENT_SIGNAL_STRENGTH_CHANGED);
        mPhoneStateReceiver.notifyServiceState(EVENT_SERVICE_STATE_CHANGED);
        mPhoneStateReceiver.notifyPhoneCallState(EVENT_PHONE_STATE_CHANGED);
                         
        updateQxdmState(null);
        mOem = new OemCommands();

        phone.getPreferredNetworkType(
                mHandler.obtainMessage(EVENT_QUERY_PREFERRED_TYPE_DONE));
        phone.getNeighboringCids(
                mHandler.obtainMessage(EVENT_QUERY_NEIGHBORING_CIDS_DONE));

        netstat = INetStatService.Stub.asInterface(ServiceManager.getService("netstat"));

        CellLocation.requestLocationUpdate();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updatePhoneState();
        updateSignalStrength();
        updateMessageWaiting();
        updateCallRedirect();
        updateServiceState();
        updateLocation(mTelephonyManager.getCellLocation());
        updateDataState();
        updateDataStats();
        updateDataStats2();
        updatePowerState();
        updateQxdmState(null);
        updateProperties();
        updateCiphState();
        updateDnsCheckState();

        Log.i(TAG, "[RadioInfo] onResume: register phone & data intents");

        mPhoneStateReceiver.registerIntent();
        mTelephonyManager.listen(mPhoneStateListener,
                  PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                | PhoneStateListener.LISTEN_DATA_ACTIVITY
                | PhoneStateListener.LISTEN_CELL_LOCATION
                | PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR
                | PhoneStateListener.LISTEN_CALL_FORWARDING_INDICATOR);
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.i(TAG, "[RadioInfo] onPause: unregister phone & data intents");

        mPhoneStateReceiver.unregisterIntent();
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ITEM_SELECT_BAND, 0, R.string.radio_info_band_mode_label).setOnMenuItemClickListener(mSelectBandCallback)
                .setAlphabeticShortcut('b');
        menu.add(1, MENU_ITEM_VIEW_ADN, 0,
                R.string.radioInfo_menu_viewADN).setOnMenuItemClickListener(mViewADNCallback);
        menu.add(1, MENU_ITEM_VIEW_FDN, 0,
                R.string.radioInfo_menu_viewFDN).setOnMenuItemClickListener(mViewFDNCallback);
        menu.add(1, MENU_ITEM_VIEW_SDN, 0,
                R.string.radioInfo_menu_viewSDN).setOnMenuItemClickListener(mViewSDNCallback);
        menu.add(1, MENU_ITEM_GET_PDP_LIST,
                0, R.string.radioInfo_menu_getPDP).setOnMenuItemClickListener(mGetPdpList);
        menu.add(1, MENU_ITEM_TOGGLE_DATA,
                0, R.string.radioInfo_menu_disableData).setOnMenuItemClickListener(mToggleData);
        menu.add(1, MENU_ITEM_TOGGLE_DATA_ON_BOOT,
                0, R.string.radioInfo_menu_disableDataOnBoot).setOnMenuItemClickListener(mToggleDataOnBoot);
        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        // Get the TOGGLE DATA menu item in the right state.
        MenuItem item = menu.findItem(MENU_ITEM_TOGGLE_DATA);
        int state = mTelephonyManager.getDataState();
        boolean visible = true;

        switch (state) {
            case TelephonyManager.DATA_CONNECTED:
            case TelephonyManager.DATA_SUSPENDED:
                item.setTitle(R.string.radioInfo_menu_disableData);
                break;
            case TelephonyManager.DATA_DISCONNECTED:
                item.setTitle(R.string.radioInfo_menu_enableData);
                break;
            default:
                visible = false;
                break;
        }
        item.setVisible(visible);

        // Get the toggle-data-on-boot menu item in the right state.
        item = menu.findItem(MENU_ITEM_TOGGLE_DATA_ON_BOOT);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(phone.getContext());
        boolean value = sp.getBoolean(GSMPhone.DATA_DISABLED_ON_BOOT_KEY, false);
        if (value) {
            item.setTitle(R.string.radioInfo_menu_enableDataOnBoot);
        } else {
            item.setTitle(R.string.radioInfo_menu_disableDataOnBoot);
        }
        return true;
    }

    private boolean isRadioOn() {
        return phone.getServiceState().getState() != ServiceState.STATE_POWER_OFF;
    }
    
    private void updatePowerState() {
    	//log("updatePowerState");
        String buttonText = isRadioOn() ?
                            getString(R.string.turn_off_radio) :
                            getString(R.string.turn_on_radio);
        radioPowerButton.setText(buttonText);    	
    }

    private void updateQxdmState(Boolean newQxdmStatus) {
        SharedPreferences sp = 
          PreferenceManager.getDefaultSharedPreferences(phone.getContext());
        mQxdmLogEnabled = sp.getBoolean("qxdmstatus", false);
        // This is called from onCreate, onResume, and the handler when the status
        // is updated. 
        if (newQxdmStatus != null) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean("qxdmstatus", newQxdmStatus);
            editor.commit();
            mQxdmLogEnabled = newQxdmStatus;
        }
        
        String buttonText = mQxdmLogEnabled ?
                            getString(R.string.turn_off_qxdm) :
                            getString(R.string.turn_on_qxdm);
        qxdmLogButton.setText(buttonText);
    }

    private void setCiphPref(boolean value) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(phone.getContext());
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(GSMPhone.CIPHERING_KEY, value);
        editor.commit();
    }

    private boolean getCiphPref() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(phone.getContext());
        boolean ret = sp.getBoolean(GSMPhone.CIPHERING_KEY, true);
        return ret;
    }

    private void updateCiphState() {
        cipherState.setText(getCiphPref() ? "Ciphering ON" : "Ciphering OFF");
    }

    private void updateDnsCheckState() {
        GSMPhone gsmPhone = (GSMPhone) phone;
        dnsCheckState.setText(gsmPhone.isDnsCheckDisabled() ?
                "0.0.0.0 allowed" :"0.0.0.0 not allowed");
    }
    
    private final void
    updateSignalStrength() {
        int state =
                mPhoneStateReceiver.getServiceState().getState();
        Resources r = getResources();

        if ((ServiceState.STATE_OUT_OF_SERVICE == state) ||
                (ServiceState.STATE_POWER_OFF == state)) {
            dBm.setText("0");
        }
        
        int signalDbm = mPhoneStateReceiver.getSignalStrengthDbm();
        
        if (-1 == signalDbm) signalDbm = 0;

        int signalAsu = mPhoneStateReceiver.getSignalStrength();

        if (-1 == signalAsu) signalAsu = 0;

        dBm.setText(String.valueOf(signalDbm) + " "
            + r.getString(R.string.radioInfo_display_dbm) + "   "
            + String.valueOf(signalAsu) + " "
            + r.getString(R.string.radioInfo_display_asu));
    }

    private final void updateLocation(CellLocation location) {
        GsmCellLocation loc = (GsmCellLocation)location;
        Resources r = getResources();

        int lac = loc.getLac();
        int cid = loc.getCid();

        mLocation.setText(r.getString(R.string.radioInfo_lac) + " = "
                          + ((lac == -1) ? "unknown" : Integer.toHexString(lac))
                          + "   "
                          + r.getString(R.string.radioInfo_cid) + " = "
                + ((cid == -1) ? "unknown" : Integer.toHexString(cid)));
    }

    private final void updateNeighboringCids(ArrayList<NeighboringCellInfo> cids) {
        String neighborings = "";
        if (cids != null) {
            if ( cids.isEmpty() ) {
                neighborings = "no neighboring cells";
            } else {
                for (NeighboringCellInfo cell : cids) {
                    neighborings += "{" + Integer.toHexString(cell.getCid()) 
                    + "@" + cell.getRssi() + "} ";
                }
            }
        } else {
            neighborings = "unknown";
        }
        mNeighboringCids.setText(neighborings);
    }

    private final void
    updateMessageWaiting() {
        mMwi.setText(String.valueOf(mMwiValue));
    }

    private final void
    updateCallRedirect() {
        mCfi.setText(String.valueOf(mCfiValue));
    }


    private final void
    updateServiceState() {
        ServiceState serviceState = mPhoneStateReceiver.getServiceState();
        int state = serviceState.getState();
        Resources r = getResources();
        String display = r.getString(R.string.radioInfo_unknown);
        
        switch (state) {
            case ServiceState.STATE_IN_SERVICE:
                display = r.getString(R.string.radioInfo_service_in);
                break;
            case ServiceState.STATE_OUT_OF_SERVICE:
            case ServiceState.STATE_EMERGENCY_ONLY:
                display = r.getString(R.string.radioInfo_service_emergency);
                break;
            case ServiceState.STATE_POWER_OFF:
                display = r.getString(R.string.radioInfo_service_off);
                break;
        }
        
        gsmState.setText(display);
        
        if (serviceState.getRoaming()) {
            roamingState.setText(R.string.radioInfo_roaming_in);
        } else {
            roamingState.setText(R.string.radioInfo_roaming_not);
        }

        operatorName.setText(serviceState.getOperatorAlphaLong());
    }

    private final void
    updatePhoneState() {
        Phone.State state = mPhoneStateReceiver.getPhoneState();
        Resources r = getResources();
        String display = r.getString(R.string.radioInfo_unknown);

        switch (state) {
            case IDLE:
                display = r.getString(R.string.radioInfo_phone_idle);
                break;
            case RINGING:
                display = r.getString(R.string.radioInfo_phone_ringing);
                break;
            case OFFHOOK:
                display = r.getString(R.string.radioInfo_phone_offhook);
                break;
        }

        callState.setText(display);
    }

    private final void
    updateDataState() {
        int state = mTelephonyManager.getDataState();
        Resources r = getResources();
        String display = r.getString(R.string.radioInfo_unknown);

        switch (state) {
            case TelephonyManager.DATA_CONNECTED:
                display = r.getString(R.string.radioInfo_data_connected);
                break;
            case TelephonyManager.DATA_CONNECTING:
                display = r.getString(R.string.radioInfo_data_connecting);
                break;
            case TelephonyManager.DATA_DISCONNECTED:
                display = r.getString(R.string.radioInfo_data_disconnected);
                break;
            case TelephonyManager.DATA_SUSPENDED:
                display = r.getString(R.string.radioInfo_data_suspended);
                break;
        }
        
        gprsState.setText(display);
    }

    private final void updateNetworkType() {
        Resources r = getResources();
        String display = SystemProperties.get(TelephonyProperties.PROPERTY_DATA_NETWORK_TYPE,
                r.getString(R.string.radioInfo_unknown));

        network.setText(display);
    }

    private final void
    updateProperties() {
        String s;
        Resources r = getResources();

        s = phone.getDeviceId();
        if (s == null) s = r.getString(R.string.radioInfo_unknown); 
        mImei.setText(s);
        
        s = phone.getLine1Number();
        if (s == null) s = r.getString(R.string.radioInfo_unknown); 
        number.setText(s);
    }

    private final void updateDataStats() {
        String s;

        s = SystemProperties.get("net.gsm.radio-reset", "0");
        resets.setText(s);

        s = SystemProperties.get("net.gsm.attempt-gprs", "0");
        attempts.setText(s);

        s = SystemProperties.get("net.gsm.succeed-gprs", "0");
        successes.setText(s);

        //s = SystemProperties.get("net.gsm.disconnect", "0");
        //disconnects.setText(s);

        s = SystemProperties.get("net.ppp.reset-by-timeout", "0");
        sentSinceReceived.setText(s);
    }

    private final void updateDataStats2() {
        Resources r = getResources();

        try {
            long txPackets = netstat.getMobileTxPackets();
            long rxPackets = netstat.getMobileRxPackets();
            long txBytes   = netstat.getMobileTxBytes();
            long rxBytes   = netstat.getMobileRxBytes();
    
            String packets = r.getString(R.string.radioInfo_display_packets);
            String bytes   = r.getString(R.string.radioInfo_display_bytes);
    
            sent.setText(txPackets + " " + packets + ", " + txBytes + " " + bytes);
            received.setText(rxPackets + " " + packets + ", " + rxBytes + " " + bytes);
        } catch (RemoteException e) {
        }
    }

    /**
     * Ping a IP address.
     */
    private final void pingIpAddr() {
        try {
            // This is hardcoded IP addr. This is for testing purposes.
            // We would need to get rid of this before release.
            String ipAddress = "74.125.47.104";
            Process p = Runtime.getRuntime().exec("ping -c 1 " + ipAddress);
            int status = p.waitFor();
            if (status == 0) {
                mPingIpAddrResult = "Pass";
            } else {
                mPingIpAddrResult = "Fail: IP addr not reachable";
            }
        } catch (IOException e) {
            mPingIpAddrResult = "Fail: IOException";
        } catch (InterruptedException e) {
            mPingIpAddrResult = "Fail: InterruptedException";
        }
    }

    /**
     *  Ping a host name
     */
    private final void pingHostname() {
        try {
            Process p = Runtime.getRuntime().exec("ping -c 1 www.google.com"); 
            int status = p.waitFor();
            if (status == 0) {
                mPingHostnameResult = "Pass";
            } else {
                mPingHostnameResult = "Fail: Host unreachable";
            }
        } catch (UnknownHostException e) {
            mPingHostnameResult = "Fail: Unknown Host";
        } catch (IOException e) {
            mPingHostnameResult= "Fail: IOException";
        } catch (InterruptedException e) {
            mPingHostnameResult = "Fail: InterruptedException";
        }
    }

    /**
     * This function checks for basic functionality of HTTP Client.
     */
    private void httpClientTest() {
        HttpClient client = new DefaultHttpClient();
        try {
            HttpGet request = new HttpGet("http://www.google.com");
            HttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() == 200) {
                mHttpClientTestResult = "Pass";
            } else {
                mHttpClientTestResult = "Fail: Code: " + String.valueOf(response);
            }
            request.abort();
        } catch (IOException e) {
            mHttpClientTestResult = "Fail: IOException";
        }
    }

    private void refreshSmsc() {
        byte[] data = mOem.getSmscQueryData();
        if (data == null) return;
        phone.invokeOemRilRequestRaw(data,
                mHandler.obtainMessage(EVENT_QUERY_SMSC_DONE));
    }

    private final void updatePingState() {
        final Handler handler = new Handler();
        // Set all to unknown since the threads will take a few secs to update.
        mPingIpAddrResult = getResources().getString(R.string.radioInfo_unknown);
        mPingHostnameResult = getResources().getString(R.string.radioInfo_unknown);
        mHttpClientTestResult = getResources().getString(R.string.radioInfo_unknown);

        mPingIpAddr.setText(mPingIpAddrResult);
        mPingHostname.setText(mPingHostnameResult);
        mHttpClientTest.setText(mHttpClientTestResult);

        final Runnable updatePingResults = new Runnable() {
            public void run() {
                mPingIpAddr.setText(mPingIpAddrResult);
                mPingHostname.setText(mPingHostnameResult);
                mHttpClientTest.setText(mHttpClientTestResult);
            }
        };
        Thread ipAddr = new Thread() {
            @Override
            public void run() {
                pingIpAddr();
                handler.post(updatePingResults);
            }
        };
        ipAddr.start();

        Thread hostname = new Thread() {
            @Override
            public void run() {
                pingHostname();
                handler.post(updatePingResults);
            }
        };
        hostname.start();

        Thread httpClient = new Thread() {
            @Override
            public void run() {
                httpClientTest();
                handler.post(updatePingResults);
            }
        };
        httpClient.start();
    }

    private final void updatePdpList() {
        StringBuilder sb = new StringBuilder("========DATA=======\n");

        List<PdpConnection> pdps = phone.getCurrentPdpList();

        for (PdpConnection pdp : pdps) {
            sb.append("    State: ").append(pdp.getState().toString()).append("\n");
            if (pdp.getState().isActive()) {
                long timeElapsed =
                    (System.currentTimeMillis() - pdp.getConnectionTime())/1000;
                sb.append("    connected at ")
                  .append(DateUtils.timeString(pdp.getConnectionTime()))
                  .append(" and elapsed ")
                  .append(DateUtils.formatElapsedTime(timeElapsed))
                  .append("\n    to ")
                  .append(pdp.getApn().toString())
                  .append("\ninterface: ")
                  .append(phone.getInterfaceName(phone.getActiveApnTypes()[0]))
                  .append("\naddress: ")
                  .append(phone.getIpAddress(phone.getActiveApnTypes()[0]))
                  .append("\ngateway: ")
                  .append(phone.getGateway(phone.getActiveApnTypes()[0]));
                String[] dns = phone.getDnsServers(phone.getActiveApnTypes()[0]);
                if (dns != null) {
                    sb.append("\ndns: ").append(dns[0]).append(", ").append(dns[1]);
                }
            } else if (pdp.getState().isInactive()) {
                sb.append("    disconnected with last try at ")
                  .append(DateUtils.timeString(pdp.getLastFailTime()))
                  .append("\n    fail because ")
                  .append(pdp.getLastFailCause().toString());
            } else {
                sb.append("    is connecting to ")
                  .append(pdp.getApn().toString());
            }
            sb.append("\n===================");
        }


        disconnects.setText(sb.toString());
    }

    private void displayQxdmEnableResult() {
        String status = mQxdmLogEnabled ? "Start QXDM Log" : "Stop QXDM Log";

        DialogInterface mProgressPanel = new AlertDialog.
                Builder(this).setMessage(status).show();

        mHandler.postDelayed(
                new Runnable() {
                    public void run() {
                        finish();
                    }
                }, 2000);
    }

    private MenuItem.OnMenuItemClickListener mViewADNCallback = new MenuItem.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            // XXX We need to specify the component here because if we don't
            // the activity manager will try to resolve the type by calling
            // the content provider, which causes it to be loaded in a process
            // other than the Dialer process, which causes a lot of stuff to
            // break.
            intent.setClassName("com.android.phone",
                    "com.android.phone.SimContacts");
            startActivity(intent);
            return true;
        }
    };

    private MenuItem.OnMenuItemClickListener mViewFDNCallback = new MenuItem.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            // XXX We need to specify the component here because if we don't
            // the activity manager will try to resolve the type by calling
            // the content provider, which causes it to be loaded in a process
            // other than the Dialer process, which causes a lot of stuff to
            // break.
            intent.setClassName("com.android.phone",
                    "com.android.phone.FdnList");
            startActivity(intent);
            return true;
        }
    };

    private MenuItem.OnMenuItemClickListener mViewSDNCallback = new MenuItem.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            Intent intent = new Intent(
                Intent.ACTION_VIEW, Uri.parse("content://sim/sdn"));
            // XXX We need to specify the component here because if we don't
            // the activity manager will try to resolve the type by calling
            // the content provider, which causes it to be loaded in a process
            // other than the Dialer process, which causes a lot of stuff to
            // break.
            intent.setClassName("com.android.phone",
                    "com.android.phone.ADNList");
            startActivity(intent);
            return true;
        }
    };

    private void toggleDataDisabledOnBoot() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(phone.getContext());
        SharedPreferences.Editor editor = sp.edit();
        boolean value = sp.getBoolean(GSMPhone.DATA_DISABLED_ON_BOOT_KEY, false);
        editor.putBoolean(GSMPhone.DATA_DISABLED_ON_BOOT_KEY, !value);
        byte[] data = mOem.getPsAutoAttachData(value);
        if (data == null) {
            // don't commit
            return;
        }

        editor.commit();
        phone.invokeOemRilRequestRaw(data, null);
    }

    private MenuItem.OnMenuItemClickListener mToggleDataOnBoot = new MenuItem.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            toggleDataDisabledOnBoot();
            return true;
        }
    };
    
    private MenuItem.OnMenuItemClickListener mToggleData = new MenuItem.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            int state = mTelephonyManager.getDataState();
            switch (state) {
                case TelephonyManager.DATA_CONNECTED:
                    phone.disableDataConnectivity();
                    break;
                case TelephonyManager.DATA_DISCONNECTED:
                    phone.enableDataConnectivity();
                    break;
                default:
                    // do nothing
                    break;
            }
            return true;
        }
    };

    private MenuItem.OnMenuItemClickListener mGetPdpList = new MenuItem.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            phone.getPdpContextList(null);
            return true;
        }
    };

    private MenuItem.OnMenuItemClickListener mSelectBandCallback = new MenuItem.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            Intent intent = new Intent();
            intent.setClass(RadioInfo.this, BandMode.class);
            startActivity(intent);
            return true;
        }
    };

    OnClickListener mPowerButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            //log("toggle radio power: currently " + (isRadioOn()?"on":"off"));
            phone.setRadioPower(!isRadioOn());
        }
    };

    OnClickListener mCipherButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            mCipherOn = !getCiphPref();
            byte[] data = mOem.getCipheringData(mCipherOn);
            
            if (data == null)
                return;

            cipherState.setText("Setting...");
            phone.invokeOemRilRequestRaw(data,
                    mHandler.obtainMessage(EVENT_SET_CIPHER_DONE));
        }
    };
    
    OnClickListener mDnsCheckButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            GSMPhone gsmPhone = (GSMPhone) phone;
            gsmPhone.disableDnsCheck(!gsmPhone.isDnsCheckDisabled());
            updateDnsCheckState();
        }
    };
    
    OnClickListener mPingButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            updatePingState();
        }
    };

    OnClickListener mUpdateSmscButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            updateSmscButton.setEnabled(false);
            byte[] data = mOem.getSmscUpdateData(smsc.getText().toString());
            if (data == null) return;
            phone.invokeOemRilRequestRaw(data,
                    mHandler.obtainMessage(EVENT_UPDATE_SMSC_DONE));
        }
    };

    OnClickListener mRefreshSmscButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            refreshSmsc();
        }
    };

    OnClickListener mQxdmButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            byte[] data = mOem.getQxdmSdlogData(
                    !mQxdmLogEnabled,
                    mOem.OEM_QXDM_SDLOG_DEFAULT_FILE_SIZE,
                    mOem.OEM_QXDM_SDLOG_DEFAULT_MASK,
                    mOem.OEM_QXDM_SDLOG_DEFAULT_MAX_INDEX);

            if (data == null)
                return;

            phone.invokeOemRilRequestRaw(data,
                    mHandler.obtainMessage(EVENT_SET_QXDMLOG_DONE));
        }
    };

    AdapterView.OnItemSelectedListener
            mPreferredNetworkHandler = new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView parent, View v, int pos, long id) {
            Message msg = mHandler.obtainMessage(EVENT_SET_PREFERRED_TYPE_DONE);
            if (pos>=0 && pos<=2) {
                phone.setPreferredNetworkType(pos, msg);
            }
        }

        public void onNothingSelected(AdapterView parent) {
        }
    };

    private String[] mPreferredNetworkLabels = {
            "WCDMA preferred", "GSM only", "WCDMA only", "Unknown"};
}
