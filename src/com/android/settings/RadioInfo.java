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
import android.app.QueuedWork;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellLocation;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.DataConnectionRealTimeInfo;
import android.telephony.NeighboringCellInfo;
import android.telephony.PreciseCallState;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.android.ims.ImsConfig;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.TelephonyProperties;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class RadioInfo extends Activity {
    private static final String TAG = "RadioInfo";

    private static final String[] mPreferredNetworkLabels = {
            "WCDMA preferred",
            "GSM only",
            "WCDMA only",
            "GSM auto (PRL)",
            "CDMA auto (PRL)",
            "CDMA only",
            "EvDo only",
            "Global auto (PRL)",
            "LTE/CDMA auto (PRL)",
            "LTE/UMTS auto (PRL)",
            "LTE/CDMA/UMTS auto (PRL)",
            "LTE only",
            "LTE/WCDMA",
            "TD-SCDMA only",
            "TD-SCDMA/WCDMA",
            "LTE/TD-SCDMA",
            "TD-SCDMA/GSM",
            "TD-SCDMA/UMTS",
            "LTE/TD-SCDMA/WCDMA",
            "LTE/TD-SCDMA/UMTS",
            "TD-SCDMA/CDMA/UMTS",
            "Global/TD-SCDMA",
            "Unknown"
    };


    private static final int CELL_INFO_LIST_RATE_DISABLED = Integer.MAX_VALUE;
    private static final int CELL_INFO_LIST_RATE_MAX = 0;


    private static final int IMS_VOLTE_PROVISIONED_CONFIG_ID =
        ImsConfig.ConfigConstants.VLT_SETTING_ENABLED;

    private static final int IMS_VT_PROVISIONED_CONFIG_ID =
        ImsConfig.ConfigConstants.LVC_SETTING_ENABLED;

    private static final int IMS_WFC_PROVISIONED_CONFIG_ID =
        ImsConfig.ConfigConstants.VOICE_OVER_WIFI_SETTING_ENABLED;

    //Values in must match mCellInfoRefreshRates
    private static final String[] mCellInfoRefreshRateLabels = {
            "Disabled",
            "Immediate",
            "Min 5s",
            "Min 10s",
            "Min 60s"
    };

    //Values in seconds, must match mCellInfoRefreshRateLabels
    private static final int mCellInfoRefreshRates[] = {
        CELL_INFO_LIST_RATE_DISABLED,
        CELL_INFO_LIST_RATE_MAX,
        5000,
        10000,
        60000
    };

    private void log(String s) {
        Log.d(TAG, s);
    }

    private static final int EVENT_CFI_CHANGED = 302;

    private static final int EVENT_QUERY_PREFERRED_TYPE_DONE = 1000;
    private static final int EVENT_SET_PREFERRED_TYPE_DONE = 1001;
    private static final int EVENT_QUERY_SMSC_DONE = 1005;
    private static final int EVENT_UPDATE_SMSC_DONE = 1006;

    private static final int MENU_ITEM_SELECT_BAND  = 0;
    private static final int MENU_ITEM_VIEW_ADN     = 1;
    private static final int MENU_ITEM_VIEW_FDN     = 2;
    private static final int MENU_ITEM_VIEW_SDN     = 3;
    private static final int MENU_ITEM_GET_PDP_LIST = 4;
    private static final int MENU_ITEM_TOGGLE_DATA  = 5;

    private TextView mDeviceId; //DeviceId is the IMEI in GSM and the MEID in CDMA
    private TextView number;
    private TextView callState;
    private TextView operatorName;
    private TextView roamingState;
    private TextView gsmState;
    private TextView gprsState;
    private TextView voiceNetwork;
    private TextView dataNetwork;
    private TextView dBm;
    private TextView mMwi;
    private TextView mCfi;
    private TextView mLocation;
    private TextView mNeighboringCids;
    private TextView mCellInfo;
    private TextView mDcRtInfoTv;
    private TextView sent;
    private TextView received;
    private TextView mPingHostnameV4;
    private TextView mPingHostnameV6;
    private TextView mHttpClientTest;
    private TextView dnsCheckState;
    private EditText smsc;
    private Switch radioPowerOnSwitch;
    private Button cellInfoRefreshRateButton;
    private Button dnsCheckToggleButton;
    private Button pingTestButton;
    private Button updateSmscButton;
    private Button refreshSmscButton;
    private Button oemInfoButton;
    private Switch imsVolteProvisionedSwitch;
    private Switch imsVtProvisionedSwitch;
    private Switch imsWfcProvisionedSwitch;
    private Spinner preferredNetworkType;
    private Spinner cellInfoRefreshRateSpinner;

    private TelephonyManager mTelephonyManager;
    private ImsManager mImsManager = null;
    private Phone phone = null;

    private String mPingHostnameResultV4;
    private String mPingHostnameResultV6;
    private String mHttpClientTestResult;
    private boolean mMwiValue = false;
    private boolean mCfiValue = false;

    private List<CellInfo> mCellInfoResult = null;
    private CellLocation mCellLocationResult = null;
    private List<NeighboringCellInfo> mNeighboringCellResult = null;

    private int mPreferredNetworkTypeResult;
    private int mCellInfoRefreshRateIndex;

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onDataConnectionStateChanged(int state) {
            updateDataState();
            updateNetworkType();
        }

        @Override
        public void onDataActivity(int direction) {
            updateDataStats2();
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            updateNetworkType();
            updatePhoneState(state);
        }

        @Override
        public void onPreciseCallStateChanged(PreciseCallState preciseState) {
            updateNetworkType();
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

        @Override
        public void onCellInfoChanged(List<CellInfo> arrayCi) {
            log("onCellInfoChanged: arrayCi=" + arrayCi);
            mCellInfoResult = arrayCi;
            updateCellInfo(mCellInfoResult);
        }

        @Override
        public void onDataConnectionRealTimeInfoChanged(DataConnectionRealTimeInfo dcRtInfo) {
            log("onDataConnectionRealTimeInfoChanged: dcRtInfo=" + dcRtInfo);
            updateDcRtInfoTv(dcRtInfo);
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            log("onSignalStrengthChanged: SignalStrength=" +signalStrength);
            updateSignalStrength(signalStrength);
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            log("onServiceStateChanged: ServiceState=" + serviceState);
            updateServiceState(serviceState);
            updateRadioPowerState();
            updateNetworkType();
            updateImsProvisionedState();
        }
    };

    private void updatePreferredNetworkType(int type) {
        if (type >= mPreferredNetworkLabels.length || type < 0) {
            log("EVENT_QUERY_PREFERRED_TYPE_DONE: unknown " +
                    "type=" + type);
            type = mPreferredNetworkLabels.length - 1; //set to Unknown
        }
        mPreferredNetworkTypeResult = type;

        preferredNetworkType.setSelection(mPreferredNetworkTypeResult, true);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
                case EVENT_QUERY_PREFERRED_TYPE_DONE:
                    ar= (AsyncResult) msg.obj;
                    if (ar.exception == null && ar.result != null) {
                        updatePreferredNetworkType(((int[])ar.result)[0]);
                    } else {
                        //In case of an exception, we will set this to unknown
                        updatePreferredNetworkType(mPreferredNetworkLabels.length-1);
                    }
                    break;
                case EVENT_SET_PREFERRED_TYPE_DONE:
                    ar= (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        log("Set preferred network type failed.");
                    }
                    break;
                case EVENT_QUERY_SMSC_DONE:
                    ar= (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        smsc.setText("refresh error");
                    } else {
                        smsc.setText((String)ar.result);
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
                    super.handleMessage(msg);
                    break;

            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.radio_info);

        log("Started onCreate");

        mTelephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        phone = PhoneFactory.getDefaultPhone();

        //TODO: Need to update this if the default phoneId changes?
        //      Better to have an instance per phone?
        mImsManager = ImsManager.getInstance(getApplicationContext(),
                SubscriptionManager.getDefaultVoicePhoneId());

        mDeviceId= (TextView) findViewById(R.id.imei);
        number = (TextView) findViewById(R.id.number);
        callState = (TextView) findViewById(R.id.call);
        operatorName = (TextView) findViewById(R.id.operator);
        roamingState = (TextView) findViewById(R.id.roaming);
        gsmState = (TextView) findViewById(R.id.gsm);
        gprsState = (TextView) findViewById(R.id.gprs);
        voiceNetwork = (TextView) findViewById(R.id.voice_network);
        dataNetwork = (TextView) findViewById(R.id.data_network);
        dBm = (TextView) findViewById(R.id.dbm);
        mMwi = (TextView) findViewById(R.id.mwi);
        mCfi = (TextView) findViewById(R.id.cfi);
        mLocation = (TextView) findViewById(R.id.location);
        mNeighboringCids = (TextView) findViewById(R.id.neighboring);
        mCellInfo = (TextView) findViewById(R.id.cellinfo);
        mCellInfo.setTypeface(Typeface.MONOSPACE);
        mDcRtInfoTv = (TextView) findViewById(R.id.dcrtinfo);

        sent = (TextView) findViewById(R.id.sent);
        received = (TextView) findViewById(R.id.received);
        smsc = (EditText) findViewById(R.id.smsc);
        dnsCheckState = (TextView) findViewById(R.id.dnsCheckState);
        mPingHostnameV4 = (TextView) findViewById(R.id.pingHostnameV4);
        mPingHostnameV6 = (TextView) findViewById(R.id.pingHostnameV6);
        mHttpClientTest = (TextView) findViewById(R.id.httpClientTest);

        preferredNetworkType = (Spinner) findViewById(R.id.preferredNetworkType);
        ArrayAdapter<String> adapter = new ArrayAdapter<String> (this,
                android.R.layout.simple_spinner_item, mPreferredNetworkLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        preferredNetworkType.setAdapter(adapter);

        cellInfoRefreshRateSpinner = (Spinner) findViewById(R.id.cell_info_rate_select);
        ArrayAdapter<String> cellInfoAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, mCellInfoRefreshRateLabels);
        cellInfoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cellInfoRefreshRateSpinner.setAdapter(cellInfoAdapter);

        imsVolteProvisionedSwitch = (Switch) findViewById(R.id.volte_provisioned_switch);
        imsVtProvisionedSwitch = (Switch) findViewById(R.id.vt_provisioned_switch);
        imsWfcProvisionedSwitch = (Switch) findViewById(R.id.wfc_provisioned_switch);

        radioPowerOnSwitch = (Switch) findViewById(R.id.radio_power);

        pingTestButton = (Button) findViewById(R.id.ping_test);
        pingTestButton.setOnClickListener(mPingButtonHandler);
        updateSmscButton = (Button) findViewById(R.id.update_smsc);
        updateSmscButton.setOnClickListener(mUpdateSmscButtonHandler);
        refreshSmscButton = (Button) findViewById(R.id.refresh_smsc);
        refreshSmscButton.setOnClickListener(mRefreshSmscButtonHandler);
        dnsCheckToggleButton = (Button) findViewById(R.id.dns_check_toggle);
        dnsCheckToggleButton.setOnClickListener(mDnsCheckButtonHandler);

        oemInfoButton = (Button) findViewById(R.id.oem_info);
        oemInfoButton.setOnClickListener(mOemInfoButtonHandler);
        PackageManager pm = getPackageManager();
        Intent oemInfoIntent = new Intent("com.android.settings.OEM_RADIO_INFO");
        List<ResolveInfo> oemInfoIntentList = pm.queryIntentActivities(oemInfoIntent, 0);
        if (oemInfoIntentList.size() == 0) {
            oemInfoButton.setEnabled(false);
        }

        mCellInfoRefreshRateIndex = 0; //disabled
        mPreferredNetworkTypeResult = mPreferredNetworkLabels.length - 1; //Unknown

        //FIXME: Replace with TelephonyManager call
        phone.getPreferredNetworkType(
                mHandler.obtainMessage(EVENT_QUERY_PREFERRED_TYPE_DONE));

        restoreFromBundle(icicle);
    }

    @Override
    protected void onResume() {
        super.onResume();

        log("Started onResume");

        updateMessageWaiting();
        updateCallRedirect();
        updateDataState();
        updateDataStats2();
        updateRadioPowerState();
        updateImsProvisionedState();
        updateProperties();
        updateDnsCheckState();
        updateNetworkType();

        updateNeighboringCids(mNeighboringCellResult);
        updateLocation(mCellLocationResult);
        updateCellInfo(mCellInfoResult);

        mPingHostnameV4.setText(mPingHostnameResultV4);
        mPingHostnameV6.setText(mPingHostnameResultV6);
        mHttpClientTest.setText(mHttpClientTestResult);

        cellInfoRefreshRateSpinner.setOnItemSelectedListener(mCellInfoRefreshRateHandler);
        //set selection after registering listener to force update
        cellInfoRefreshRateSpinner.setSelection(mCellInfoRefreshRateIndex);

        //set selection before registering to prevent update
        preferredNetworkType.setSelection(mPreferredNetworkTypeResult, true);
        preferredNetworkType.setOnItemSelectedListener(mPreferredNetworkHandler);

        radioPowerOnSwitch.setOnCheckedChangeListener(mRadioPowerOnChangeListener);
        imsVolteProvisionedSwitch.setOnCheckedChangeListener(mImsVolteCheckedChangeListener);
        imsVtProvisionedSwitch.setOnCheckedChangeListener(mImsVtCheckedChangeListener);
        imsWfcProvisionedSwitch.setOnCheckedChangeListener(mImsWfcCheckedChangeListener);

        mTelephonyManager.listen(mPhoneStateListener,
                  PhoneStateListener.LISTEN_CALL_STATE
        //b/27803938 - RadioInfo currently cannot read PRECISE_CALL_STATE
        //      | PhoneStateListener.LISTEN_PRECISE_CALL_STATE
                | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                | PhoneStateListener.LISTEN_DATA_ACTIVITY
                | PhoneStateListener.LISTEN_CELL_LOCATION
                | PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR
                | PhoneStateListener.LISTEN_CALL_FORWARDING_INDICATOR
                | PhoneStateListener.LISTEN_CELL_INFO
                | PhoneStateListener.LISTEN_SERVICE_STATE
                | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                | PhoneStateListener.LISTEN_DATA_CONNECTION_REAL_TIME_INFO);

        smsc.clearFocus();
    }

    @Override
    protected void onPause() {
        super.onPause();

        log("onPause: unregister phone & data intents");

        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        phone.setCellInfoListRate(CELL_INFO_LIST_RATE_DISABLED);
    }

    private void restoreFromBundle(Bundle b) {
        if( b == null) {
            return;
        }

        mPingHostnameResultV4 = b.getString("mPingHostnameResultV4","");
        mPingHostnameResultV6 = b.getString("mPingHostnameResultV6","");
        mHttpClientTestResult = b.getString("mHttpClientTestResult","");

        mPingHostnameV4.setText(mPingHostnameResultV4);
        mPingHostnameV6.setText(mPingHostnameResultV6);
        mHttpClientTest.setText(mHttpClientTestResult);

        mPreferredNetworkTypeResult = b.getInt("mPreferredNetworkTypeResult",
                                               mPreferredNetworkLabels.length - 1);

        mCellInfoRefreshRateIndex = b.getInt("mCellInfoRefreshRateIndex", 0);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("mPingHostnameResultV4", mPingHostnameResultV4);
        outState.putString("mPingHostnameResultV6", mPingHostnameResultV6);
        outState.putString("mHttpClientTestResult", mHttpClientTestResult);

        outState.putInt("mPreferredNetworkTypeResult", mPreferredNetworkTypeResult);
        outState.putInt("mCellInfoRefreshRateIndex", mCellInfoRefreshRateIndex);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ITEM_SELECT_BAND, 0, R.string.radio_info_band_mode_label)
                .setOnMenuItemClickListener(mSelectBandCallback)
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
                0, R.string.radio_info_data_connection_disable).setOnMenuItemClickListener(mToggleData);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Get the TOGGLE DATA menu item in the right state.
        MenuItem item = menu.findItem(MENU_ITEM_TOGGLE_DATA);
        int state = mTelephonyManager.getDataState();
        boolean visible = true;

        switch (state) {
            case TelephonyManager.DATA_CONNECTED:
            case TelephonyManager.DATA_SUSPENDED:
                item.setTitle(R.string.radio_info_data_connection_disable);
                break;
            case TelephonyManager.DATA_DISCONNECTED:
                item.setTitle(R.string.radio_info_data_connection_enable);
                break;
            default:
                visible = false;
                break;
        }
        item.setVisible(visible);
        return true;
    }

    private void updateDnsCheckState() {
        //FIXME: Replace with a TelephonyManager call
        dnsCheckState.setText(phone.isDnsCheckDisabled() ?
                "0.0.0.0 allowed" :"0.0.0.0 not allowed");
    }

    private final void
    updateSignalStrength(SignalStrength signalStrength) {
        Resources r = getResources();

        int signalDbm = signalStrength.getDbm();

        int signalAsu = signalStrength.getAsuLevel();

        if (-1 == signalAsu) signalAsu = 0;

        dBm.setText(String.valueOf(signalDbm) + " "
            + r.getString(R.string.radioInfo_display_dbm) + "   "
            + String.valueOf(signalAsu) + " "
            + r.getString(R.string.radioInfo_display_asu));
    }

    private final void updateLocation(CellLocation location) {
        Resources r = getResources();
        if (location instanceof GsmCellLocation) {
            GsmCellLocation loc = (GsmCellLocation)location;
            int lac = loc.getLac();
            int cid = loc.getCid();
            mLocation.setText(r.getString(R.string.radioInfo_lac) + " = "
                    + ((lac == -1) ? "unknown" : Integer.toHexString(lac))
                    + "   "
                    + r.getString(R.string.radioInfo_cid) + " = "
                    + ((cid == -1) ? "unknown" : Integer.toHexString(cid)));
        } else if (location instanceof CdmaCellLocation) {
            CdmaCellLocation loc = (CdmaCellLocation)location;
            int bid = loc.getBaseStationId();
            int sid = loc.getSystemId();
            int nid = loc.getNetworkId();
            int lat = loc.getBaseStationLatitude();
            int lon = loc.getBaseStationLongitude();
            mLocation.setText("BID = "
                    + ((bid == -1) ? "unknown" : Integer.toHexString(bid))
                    + "   "
                    + "SID = "
                    + ((sid == -1) ? "unknown" : Integer.toHexString(sid))
                    + "   "
                    + "NID = "
                    + ((nid == -1) ? "unknown" : Integer.toHexString(nid))
                    + "\n"
                    + "LAT = "
                    + ((lat == -1) ? "unknown" : Integer.toHexString(lat))
                    + "   "
                    + "LONG = "
                    + ((lon == -1) ? "unknown" : Integer.toHexString(lon)));
        } else {
            mLocation.setText("unknown");
        }


    }

    private final void updateNeighboringCids(List<NeighboringCellInfo> cids) {
        StringBuilder sb = new StringBuilder();

        if (cids != null) {
            if ( cids.isEmpty() ) {
                sb.append("no neighboring cells");
            } else {
                for (NeighboringCellInfo cell : cids) {
                    sb.append(cell.toString()).append(" ");
                }
            }
        } else {
            sb.append("unknown");
        }
        mNeighboringCids.setText(sb.toString());
    }

    private final String getCellInfoDisplayString(int i) {
        return (i != Integer.MAX_VALUE) ? Integer.toString(i) : "";
    }

    private final String getCellInfoDisplayString(long i) {
        return (i != Long.MAX_VALUE) ? Long.toString(i) : "";
    }

    private final String buildCdmaInfoString(CellInfoCdma ci) {
        CellIdentityCdma cidCdma = ci.getCellIdentity();
        CellSignalStrengthCdma ssCdma = ci.getCellSignalStrength();

        return String.format("%-3.3s %-5.5s %-5.5s %-5.5s %-6.6s %-6.6s %-6.6s %-6.6s %-5.5s",
                ci.isRegistered() ? "S  " : "   ",
                getCellInfoDisplayString(cidCdma.getSystemId()),
                getCellInfoDisplayString(cidCdma.getNetworkId()),
                getCellInfoDisplayString(cidCdma.getBasestationId()),
                getCellInfoDisplayString(ssCdma.getCdmaDbm()),
                getCellInfoDisplayString(ssCdma.getCdmaEcio()),
                getCellInfoDisplayString(ssCdma.getEvdoDbm()),
                getCellInfoDisplayString(ssCdma.getEvdoEcio()),
                getCellInfoDisplayString(ssCdma.getEvdoSnr()));
    }

    private final String buildGsmInfoString(CellInfoGsm ci) {
        CellIdentityGsm cidGsm = ci.getCellIdentity();
        CellSignalStrengthGsm ssGsm = ci.getCellSignalStrength();

        return String.format("%-3.3s %-3.3s %-3.3s %-5.5s %-5.5s %-6.6s %-4.4s %-4.4s\n",
                ci.isRegistered() ? "S  " : "   ",
                getCellInfoDisplayString(cidGsm.getMcc()),
                getCellInfoDisplayString(cidGsm.getMnc()),
                getCellInfoDisplayString(cidGsm.getLac()),
                getCellInfoDisplayString(cidGsm.getCid()),
                getCellInfoDisplayString(cidGsm.getArfcn()),
                getCellInfoDisplayString(cidGsm.getBsic()),
                getCellInfoDisplayString(ssGsm.getDbm()));
    }

    private final String buildLteInfoString(CellInfoLte ci) {
        CellIdentityLte cidLte = ci.getCellIdentity();
        CellSignalStrengthLte ssLte = ci.getCellSignalStrength();

        return String.format(
                "%-3.3s %-3.3s %-3.3s %-5.5s %-5.5s %-3.3s %-6.6s %-4.4s %-4.4s %-2.2s\n",
                ci.isRegistered() ? "S  " : "   ",
                getCellInfoDisplayString(cidLte.getMcc()),
                getCellInfoDisplayString(cidLte.getMnc()),
                getCellInfoDisplayString(cidLte.getTac()),
                getCellInfoDisplayString(cidLte.getCi()),
                getCellInfoDisplayString(cidLte.getPci()),
                getCellInfoDisplayString(cidLte.getEarfcn()),
                getCellInfoDisplayString(ssLte.getDbm()),
                getCellInfoDisplayString(ssLte.getRsrq()),
                getCellInfoDisplayString(ssLte.getTimingAdvance()));
    }

    private final String buildWcdmaInfoString(CellInfoWcdma ci) {
        CellIdentityWcdma cidWcdma = ci.getCellIdentity();
        CellSignalStrengthWcdma ssWcdma = ci.getCellSignalStrength();

        return String.format("%-3.3s %-3.3s %-3.3s %-5.5s %-5.5s %-6.6s %-3.3s %-4.4s\n",
                ci.isRegistered() ? "S  " : "   ",
                getCellInfoDisplayString(cidWcdma.getMcc()),
                getCellInfoDisplayString(cidWcdma.getMnc()),
                getCellInfoDisplayString(cidWcdma.getLac()),
                getCellInfoDisplayString(cidWcdma.getCid()),
                getCellInfoDisplayString(cidWcdma.getUarfcn()),
                getCellInfoDisplayString(cidWcdma.getPsc()),
                getCellInfoDisplayString(ssWcdma.getDbm()));
    }

    private final String buildCellInfoString(List<CellInfo> arrayCi) {
        String value = new String();
        StringBuilder cdmaCells = new StringBuilder(),
                gsmCells = new StringBuilder(),
                lteCells = new StringBuilder(),
                wcdmaCells = new StringBuilder();

        if (arrayCi != null) {
            for (CellInfo ci : arrayCi) {

                if (ci instanceof CellInfoLte) {
                    lteCells.append(buildLteInfoString((CellInfoLte) ci));
                } else if (ci instanceof CellInfoWcdma) {
                    wcdmaCells.append(buildWcdmaInfoString((CellInfoWcdma) ci));
                } else if (ci instanceof CellInfoGsm) {
                    gsmCells.append(buildGsmInfoString((CellInfoGsm) ci));
                } else if (ci instanceof CellInfoCdma) {
                    cdmaCells.append(buildCdmaInfoString((CellInfoCdma) ci));
                }
            }
            if (lteCells.length() != 0) {
                value += String.format(
                        "LTE\n%-3.3s %-3.3s %-3.3s %-5.5s %-5.5s %-3.3s %-6.6s %-4.4s %-4.4s %-2.2s\n",
                        "SRV", "MCC", "MNC", "TAC", "CID", "PCI", "EARFCN", "RSRP", "RSRQ", "TA");
                value += lteCells.toString();
            }
            if (wcdmaCells.length() != 0) {
                value += String.format("WCDMA\n%-3.3s %-3.3s %-3.3s %-5.5s %-5.5s %-6.6s %-3.3s %-4.4s\n",
                        "SRV", "MCC", "MNC", "LAC", "CID", "UARFCN", "PSC", "RSCP");
                value += wcdmaCells.toString();
            }
            if (gsmCells.length() != 0) {
                value += String.format("GSM\n%-3.3s %-3.3s %-3.3s %-5.5s %-5.5s %-6.6s %-4.4s %-4.4s\n",
                        "SRV", "MCC", "MNC", "LAC", "CID", "ARFCN", "BSIC", "RSSI");
                value += gsmCells.toString();
            }
            if (cdmaCells.length() != 0) {
                value += String.format(
                        "CDMA/EVDO\n%-3.3s %-5.5s %-5.5s %-5.5s %-6.6s %-6.6s %-6.6s %-6.6s %-5.5s\n",
                        "SRV", "SID", "NID", "BSID", "C-RSSI", "C-ECIO", "E-RSSI", "E-ECIO", "E-SNR");
                value += cdmaCells.toString();
            }
        } else {
            value ="unknown";
        }

        return value.toString();
    }

    private final void updateCellInfo(List<CellInfo> arrayCi) {
        mCellInfo.setText(buildCellInfoString(arrayCi));
    }

    private final void updateDcRtInfoTv(DataConnectionRealTimeInfo dcRtInfo) {
        mDcRtInfoTv.setText(dcRtInfo.toString());
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
    updateServiceState(ServiceState serviceState) {
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
    updatePhoneState(int state) {
        Resources r = getResources();
        String display = r.getString(R.string.radioInfo_unknown);

        switch (state) {
            case TelephonyManager.CALL_STATE_IDLE:
                display = r.getString(R.string.radioInfo_phone_idle);
                break;
            case TelephonyManager.CALL_STATE_RINGING:
                display = r.getString(R.string.radioInfo_phone_ringing);
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
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
        if( phone != null ) {
            ServiceState ss = phone.getServiceState();
            dataNetwork.setText(ServiceState.rilRadioTechnologyToString(
                    phone.getServiceState().getRilDataRadioTechnology()));
            voiceNetwork.setText(ServiceState.rilRadioTechnologyToString(
                    phone.getServiceState().getRilVoiceRadioTechnology()));
        }
    }

    private final void
    updateProperties() {
        String s;
        Resources r = getResources();

        s = phone.getDeviceId();
        if (s == null) s = r.getString(R.string.radioInfo_unknown);
        mDeviceId.setText(s);

        //FIXME: Replace with a TelephonyManager call
        s = phone.getLine1Number();
        if (s == null) s = r.getString(R.string.radioInfo_unknown);
        number.setText(s);
    }

    private final void updateDataStats2() {
        Resources r = getResources();

        long txPackets = TrafficStats.getMobileTxPackets();
        long rxPackets = TrafficStats.getMobileRxPackets();
        long txBytes   = TrafficStats.getMobileTxBytes();
        long rxBytes   = TrafficStats.getMobileRxBytes();

        String packets = r.getString(R.string.radioInfo_display_packets);
        String bytes   = r.getString(R.string.radioInfo_display_bytes);

        sent.setText(txPackets + " " + packets + ", " + txBytes + " " + bytes);
        received.setText(rxPackets + " " + packets + ", " + rxBytes + " " + bytes);
    }

    /**
     *  Ping a host name
     */
    private final void pingHostname() {
        try {
            try {
                Process p4 = Runtime.getRuntime().exec("ping -c 1 www.google.com");
                int status4 = p4.waitFor();
                if (status4 == 0) {
                    mPingHostnameResultV4 = "Pass";
                } else {
                    mPingHostnameResultV4 = String.format("Fail(%d)", status4);
                }
            } catch (IOException e) {
                mPingHostnameResultV4 = "Fail: IOException";
            }
            try {
                Process p6 = Runtime.getRuntime().exec("ping6 -c 1 www.google.com");
                int status6 = p6.waitFor();
                if (status6 == 0) {
                    mPingHostnameResultV6 = "Pass";
                } else {
                    mPingHostnameResultV6 = String.format("Fail(%d)", status6);
                }
            } catch (IOException e) {
                mPingHostnameResultV6 = "Fail: IOException";
            }
        } catch (InterruptedException e) {
            mPingHostnameResultV4 = mPingHostnameResultV6 = "Fail: InterruptedException";
        }
    }

    /**
     * This function checks for basic functionality of HTTP Client.
     */
    private void httpClientTest() {
        HttpURLConnection urlConnection = null;
        try {
            // TODO: Hardcoded for now, make it UI configurable
            URL url = new URL("https://www.google.com");
            urlConnection = (HttpURLConnection) url.openConnection();
            if (urlConnection.getResponseCode() == 200) {
                mHttpClientTestResult = "Pass";
            } else {
                mHttpClientTestResult = "Fail: Code: " + urlConnection.getResponseMessage();
            }
        } catch (IOException e) {
            mHttpClientTestResult = "Fail: IOException";
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private void refreshSmsc() {
        //FIXME: Replace with a TelephonyManager call
        phone.getSmscAddress(mHandler.obtainMessage(EVENT_QUERY_SMSC_DONE));
    }

    private final void updateAllCellInfo() {

        mCellInfo.setText("");
        mNeighboringCids.setText("");
        mLocation.setText("");

        final Runnable updateAllCellInfoResults = new Runnable() {
            public void run() {
                updateNeighboringCids(mNeighboringCellResult);
                updateLocation(mCellLocationResult);
                updateCellInfo(mCellInfoResult);
            }
        };

        Thread locThread = new Thread() {
            @Override
            public void run() {
                mCellInfoResult = mTelephonyManager.getAllCellInfo();
                mCellLocationResult = mTelephonyManager.getCellLocation();
                mNeighboringCellResult = mTelephonyManager.getNeighboringCellInfo();

                mHandler.post(updateAllCellInfoResults);
            }
        };
        locThread.start();
    }

    private final void updatePingState() {
        // Set all to unknown since the threads will take a few secs to update.
        mPingHostnameResultV4 = getResources().getString(R.string.radioInfo_unknown);
        mPingHostnameResultV6 = getResources().getString(R.string.radioInfo_unknown);
        mHttpClientTestResult = getResources().getString(R.string.radioInfo_unknown);

        mPingHostnameV4.setText(mPingHostnameResultV4);
        mPingHostnameV6.setText(mPingHostnameResultV6);
        mHttpClientTest.setText(mHttpClientTestResult);

        final Runnable updatePingResults = new Runnable() {
            public void run() {
                mPingHostnameV4.setText(mPingHostnameResultV4);
                mPingHostnameV6.setText(mPingHostnameResultV6);
                mHttpClientTest.setText(mHttpClientTestResult);
            }
        };

        Thread hostname = new Thread() {
            @Override
            public void run() {
                pingHostname();
                mHandler.post(updatePingResults);
            }
        };
        hostname.start();

        Thread httpClient = new Thread() {
            @Override
            public void run() {
                httpClientTest();
                mHandler.post(updatePingResults);
            }
        };
        httpClient.start();
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
                    "com.android.phone.settings.fdn.FdnList");
            startActivity(intent);
            return true;
        }
    };

    private MenuItem.OnMenuItemClickListener mViewSDNCallback = new MenuItem.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            Intent intent = new Intent(
                    Intent.ACTION_VIEW, Uri.parse("content://icc/sdn"));
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

    private MenuItem.OnMenuItemClickListener mGetPdpList = new MenuItem.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            //FIXME: Replace with a TelephonyManager call
            phone.getDataCallList(null);
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

    private MenuItem.OnMenuItemClickListener mToggleData = new MenuItem.OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
            int state = mTelephonyManager.getDataState();
            switch (state) {
                case TelephonyManager.DATA_CONNECTED:
                    //FIXME: Replace with a TelephonyManager call
                    phone.setDataEnabled(false);
                    break;
                case TelephonyManager.DATA_DISCONNECTED:
                    //FIXME: Replace with a TelephonyManager call
                    phone.setDataEnabled(true);
                    break;
                default:
                    // do nothing
                    break;
            }
            return true;
        }
    };

    private boolean isRadioOn() {
        //FIXME: Replace with a TelephonyManager call
        return phone.getServiceState().getState() != ServiceState.STATE_POWER_OFF;
    }

    private void updateRadioPowerState() {
        //delightful hack to prevent on-checked-changed calls from
        //actually forcing the radio preference to its transient/current value.
        radioPowerOnSwitch.setOnCheckedChangeListener(null);
        radioPowerOnSwitch.setChecked(isRadioOn());
        radioPowerOnSwitch.setOnCheckedChangeListener(mRadioPowerOnChangeListener);
    }

    void setImsVolteProvisionedState( boolean state ) {
        Log.d(TAG, "setImsVolteProvisioned state: " + ((state)? "on":"off"));
        setImsConfigProvisionedState( IMS_VOLTE_PROVISIONED_CONFIG_ID, state );
    }

    void setImsVtProvisionedState( boolean state ) {
        Log.d(TAG, "setImsVtProvisioned() state: " + ((state)? "on":"off"));
        setImsConfigProvisionedState( IMS_VT_PROVISIONED_CONFIG_ID, state );
    }

    void setImsWfcProvisionedState( boolean state ) {
        Log.d(TAG, "setImsWfcProvisioned() state: " + ((state)? "on":"off"));
        setImsConfigProvisionedState( IMS_WFC_PROVISIONED_CONFIG_ID, state );
    }

    void setImsConfigProvisionedState( int configItem, boolean state ) {
        if (phone != null && mImsManager != null) {
            QueuedWork.singleThreadExecutor().submit(new Runnable() {
                public void run() {
                    try {
                        mImsManager.getConfigInterface().setProvisionedValue(
                                configItem,
                                state? 1 : 0);
                    } catch (ImsException e) {
                        Log.e(TAG, "setImsConfigProvisioned() exception:", e);
                    }
                }
            });
        }
    }

    OnCheckedChangeListener mRadioPowerOnChangeListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            log("toggle radio power: currently " + (isRadioOn()?"on":"off"));
            phone.setRadioPower(isChecked);
       }
    };

    private boolean isImsVolteProvisioned() {
        if (phone != null && mImsManager != null) {
            return mImsManager.isVolteEnabledByPlatform(phone.getContext())
                && mImsManager.isVolteProvisionedOnDevice(phone.getContext());
        }
        return false;
    }

    OnCheckedChangeListener mImsVolteCheckedChangeListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            setImsVolteProvisionedState(isChecked);
       }
    };

    private boolean isImsVtProvisioned() {
        if (phone != null && mImsManager != null) {
            return mImsManager.isVtEnabledByPlatform(phone.getContext())
                && mImsManager.isVtProvisionedOnDevice(phone.getContext());
        }
        return false;
    }

    OnCheckedChangeListener mImsVtCheckedChangeListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            setImsVtProvisionedState(isChecked);
       }
    };

    private boolean isImsWfcProvisioned() {
        if (phone != null && mImsManager != null) {
            return mImsManager.isWfcEnabledByPlatform(phone.getContext())
                && mImsManager.isWfcProvisionedOnDevice(phone.getContext());
        }
        return false;
    }

    OnCheckedChangeListener mImsWfcCheckedChangeListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            setImsWfcProvisionedState(isChecked);
       }
    };

    private void updateImsProvisionedState() {
        log("updateImsProvisionedState isImsVolteProvisioned()=" + isImsVolteProvisioned());
        //delightful hack to prevent on-checked-changed calls from
        //actually forcing the ims provisioning to its transient/current value.
        imsVolteProvisionedSwitch.setOnCheckedChangeListener(null);
        imsVolteProvisionedSwitch.setChecked(isImsVolteProvisioned());
        imsVolteProvisionedSwitch.setOnCheckedChangeListener(mImsVolteCheckedChangeListener);

        imsVtProvisionedSwitch.setOnCheckedChangeListener(null);
        imsVtProvisionedSwitch.setChecked(isImsVtProvisioned());
        imsVtProvisionedSwitch.setOnCheckedChangeListener(mImsVtCheckedChangeListener);

        imsWfcProvisionedSwitch.setOnCheckedChangeListener(null);
        imsWfcProvisionedSwitch.setChecked(isImsWfcProvisioned());
        imsWfcProvisionedSwitch.setOnCheckedChangeListener(mImsWfcCheckedChangeListener);
    }

    OnClickListener mDnsCheckButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            //FIXME: Replace with a TelephonyManager call
            phone.disableDnsCheck(!phone.isDnsCheckDisabled());
            updateDnsCheckState();
        }
    };

    OnClickListener mOemInfoButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent("com.android.settings.OEM_RADIO_INFO");
            try {
                startActivity(intent);
            } catch (android.content.ActivityNotFoundException ex) {
                log("OEM-specific Info/Settings Activity Not Found : " + ex);
                // If the activity does not exist, there are no OEM
                // settings, and so we can just do nothing...
            }
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
            phone.setSmscAddress(smsc.getText().toString(),
                    mHandler.obtainMessage(EVENT_UPDATE_SMSC_DONE));
        }
    };

    OnClickListener mRefreshSmscButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            refreshSmsc();
        }
    };

    AdapterView.OnItemSelectedListener mPreferredNetworkHandler =
            new AdapterView.OnItemSelectedListener() {

        public void onItemSelected(AdapterView parent, View v, int pos, long id) {
            if (mPreferredNetworkTypeResult != pos && pos >= 0
                    && pos <= mPreferredNetworkLabels.length - 2) {
                mPreferredNetworkTypeResult = pos;
                Message msg = mHandler.obtainMessage(EVENT_SET_PREFERRED_TYPE_DONE);
                phone.setPreferredNetworkType(mPreferredNetworkTypeResult, msg);
            }
        }

        public void onNothingSelected(AdapterView parent) {
        }
    };

    AdapterView.OnItemSelectedListener mCellInfoRefreshRateHandler  =
            new AdapterView.OnItemSelectedListener() {

        public void onItemSelected(AdapterView parent, View v, int pos, long id) {
            mCellInfoRefreshRateIndex = pos;
            phone.setCellInfoListRate(mCellInfoRefreshRates[pos]);
            updateAllCellInfo();
        }

        public void onNothingSelected(AdapterView parent) {
        }
    };

}
