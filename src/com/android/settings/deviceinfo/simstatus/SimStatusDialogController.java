/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.deviceinfo.simstatus;

import android.annotation.Nullable;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.telephony.Annotation;
import android.telephony.CarrierConfigManager;
import android.telephony.CellBroadcastIntents;
import android.telephony.CellBroadcastService;
import android.telephony.CellSignalStrength;
import android.telephony.ICellBroadcastService;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyManager;
import android.telephony.UiccCardInfo;
import android.telephony.euicc.EuiccManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.android.internal.telephony.PhoneConstants;
import com.android.settings.R;
import com.android.settingslib.DeviceInfoUtils;
import com.android.settingslib.Utils;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.util.List;
import java.util.Map;

public class SimStatusDialogController implements LifecycleObserver, OnResume, OnPause {

    private final static String TAG = "SimStatusDialogCtrl";

    private static final String CELL_BROADCAST_SERVICE_PACKAGE = "com.android.cellbroadcastservice";

    @VisibleForTesting
    final static int NETWORK_PROVIDER_VALUE_ID = R.id.operator_name_value;
    @VisibleForTesting
    final static int PHONE_NUMBER_VALUE_ID = R.id.number_value;
    @VisibleForTesting
    final static int CELLULAR_NETWORK_STATE = R.id.data_state_value;
    @VisibleForTesting
    final static int OPERATOR_INFO_LABEL_ID = R.id.latest_area_info_label;
    @VisibleForTesting
    final static int OPERATOR_INFO_VALUE_ID = R.id.latest_area_info_value;
    @VisibleForTesting
    final static int SERVICE_STATE_VALUE_ID = R.id.service_state_value;
    @VisibleForTesting
    final static int SIGNAL_STRENGTH_LABEL_ID = R.id.signal_strength_label;
    @VisibleForTesting
    final static int SIGNAL_STRENGTH_VALUE_ID = R.id.signal_strength_value;
    @VisibleForTesting
    final static int CELL_VOICE_NETWORK_TYPE_VALUE_ID = R.id.voice_network_type_value;
    @VisibleForTesting
    final static int CELL_DATA_NETWORK_TYPE_VALUE_ID = R.id.data_network_type_value;
    @VisibleForTesting
    final static int ROAMING_INFO_VALUE_ID = R.id.roaming_state_value;
    @VisibleForTesting
    final static int ICCID_INFO_LABEL_ID = R.id.icc_id_label;
    @VisibleForTesting
    final static int ICCID_INFO_VALUE_ID = R.id.icc_id_value;
    @VisibleForTesting
    final static int EID_INFO_LABEL_ID = R.id.esim_id_label;
    @VisibleForTesting
    final static int EID_INFO_VALUE_ID = R.id.esim_id_value;
    @VisibleForTesting
    final static int IMS_REGISTRATION_STATE_LABEL_ID = R.id.ims_reg_state_label;
    @VisibleForTesting
    final static int IMS_REGISTRATION_STATE_VALUE_ID = R.id.ims_reg_state_value;

    private final OnSubscriptionsChangedListener mOnSubscriptionsChangedListener =
            new OnSubscriptionsChangedListener() {
                @Override
                public void onSubscriptionsChanged() {
                    mSubscriptionInfo = mSubscriptionManager.getActiveSubscriptionInfo(
                            mSubscriptionInfo.getSubscriptionId());
                    mTelephonyManager = mTelephonyManager.createForSubscriptionId(
                            mSubscriptionInfo.getSubscriptionId());
                    updateNetworkProvider();
                }
            };

    private SubscriptionInfo mSubscriptionInfo;

    private final int mSlotIndex;
    private TelephonyManager mTelephonyManager;

    private final SimStatusDialogFragment mDialog;
    private final SubscriptionManager mSubscriptionManager;
    private final CarrierConfigManager mCarrierConfigManager;
    private final EuiccManager mEuiccManager;
    private final Resources mRes;
    private final Context mContext;

    private boolean mShowLatestAreaInfo;

    private final BroadcastReceiver mAreaInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateAreaInfoText();
        }
    };

    private PhoneStateListener mPhoneStateListener;

    private CellBroadcastServiceConnection mCellBroadcastServiceConnection;

    private class CellBroadcastServiceConnection implements ServiceConnection {
        private IBinder mService;

        @Nullable
        public IBinder getService() {
            return mService;
        }

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "connected to CellBroadcastService");
            this.mService = service;
            updateAreaInfoText();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            this.mService = null;
            Log.d(TAG, "mICellBroadcastService has disconnected unexpectedly");
        }

        @Override
        public void onBindingDied(ComponentName name) {
            this.mService = null;
            Log.d(TAG, "Binding died");
        }

        @Override
        public void onNullBinding(ComponentName name) {
            this.mService = null;
            Log.d(TAG, "Null binding");
        }
    }

    public SimStatusDialogController(@NonNull SimStatusDialogFragment dialog, Lifecycle lifecycle,
            int slotId) {
        mDialog = dialog;
        mContext = dialog.getContext();
        mSlotIndex = slotId;
        mSubscriptionInfo = getPhoneSubscriptionInfo(slotId);

        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        mCarrierConfigManager = mContext.getSystemService(CarrierConfigManager.class);
        mEuiccManager = mContext.getSystemService(EuiccManager.class);
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);

        mRes = mContext.getResources();

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    public void initialize() {
        updateEid();

        if (mSubscriptionInfo == null) {
            return;
        }

        mPhoneStateListener = getPhoneStateListener();
        updateNetworkProvider();

        final ServiceState serviceState = mTelephonyManager.getServiceState();
        final SignalStrength signalStrength = mTelephonyManager.getSignalStrength();

        updatePhoneNumber();
        updateLatestAreaInfo();
        updateServiceState(serviceState);
        updateSignalStrength(signalStrength);
        updateNetworkType();
        updateRoamingStatus(serviceState);
        updateIccidNumber();
        updateImsRegistrationState();
    }

    /**
     * Deinitialization works
     */
    public void deinitialize() {
        if (mShowLatestAreaInfo) {
            if (mCellBroadcastServiceConnection != null
                    && mCellBroadcastServiceConnection.getService() != null) {
                mContext.unbindService(mCellBroadcastServiceConnection);
            }
            mCellBroadcastServiceConnection = null;
        }
    }

    @Override
    public void onResume() {
        if (mSubscriptionInfo == null) {
            return;
        }
        mTelephonyManager = mTelephonyManager.createForSubscriptionId(
                mSubscriptionInfo.getSubscriptionId());
        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                        | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                        | PhoneStateListener.LISTEN_SERVICE_STATE);
        mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangedListener);

        if (mShowLatestAreaInfo) {
            updateAreaInfoText();
            mContext.registerReceiver(mAreaInfoReceiver,
                    new IntentFilter(CellBroadcastIntents.ACTION_AREA_INFO_UPDATED));
        }
    }

    @Override
    public void onPause() {
        if (mSubscriptionInfo == null) {
            return;
        }

        mSubscriptionManager.removeOnSubscriptionsChangedListener(mOnSubscriptionsChangedListener);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);

        if (mShowLatestAreaInfo) {
            mContext.unregisterReceiver(mAreaInfoReceiver);
        }
    }

    private void updateNetworkProvider() {
        final CharSequence carrierName =
                mSubscriptionInfo != null ? mSubscriptionInfo.getCarrierName() : null;
        mDialog.setText(NETWORK_PROVIDER_VALUE_ID, carrierName);
    }

    private void updatePhoneNumber() {
        // If formattedNumber is null or empty, it'll display as "Unknown".
        mDialog.setText(PHONE_NUMBER_VALUE_ID,
                DeviceInfoUtils.getBidiFormattedPhoneNumber(mContext, mSubscriptionInfo));
    }

    private void updateDataState(int state) {
        String networkStateValue;

        switch (state) {
            case TelephonyManager.DATA_CONNECTED:
                networkStateValue = mRes.getString(R.string.radioInfo_data_connected);
                break;
            case TelephonyManager.DATA_SUSPENDED:
                networkStateValue = mRes.getString(R.string.radioInfo_data_suspended);
                break;
            case TelephonyManager.DATA_CONNECTING:
                networkStateValue = mRes.getString(R.string.radioInfo_data_connecting);
                break;
            case TelephonyManager.DATA_DISCONNECTED:
                networkStateValue = mRes.getString(R.string.radioInfo_data_disconnected);
                break;
            default:
                networkStateValue = mRes.getString(R.string.radioInfo_unknown);
                break;
        }

        mDialog.setText(CELLULAR_NETWORK_STATE, networkStateValue);
    }

    /**
     * Update area info text retrieved from
     * {@link CellBroadcastService#getCellBroadcastAreaInfo(int)}
     */
    private void updateAreaInfoText() {
        if (!mShowLatestAreaInfo || mCellBroadcastServiceConnection == null) return;
        ICellBroadcastService cellBroadcastService =
                ICellBroadcastService.Stub.asInterface(
                        mCellBroadcastServiceConnection.getService());
        if (cellBroadcastService == null) return;
        try {
            mDialog.setText(OPERATOR_INFO_VALUE_ID,
                    cellBroadcastService.getCellBroadcastAreaInfo(
                            SimStatusDialogController.this.mSlotIndex));

        } catch (RemoteException e) {
            Log.d(TAG, "Can't get area info. e=" + e);
        }
    }

    /**
     * Bind cell broadcast service.
     */
    private void bindCellBroadcastService() {
        mCellBroadcastServiceConnection = new CellBroadcastServiceConnection();
        Intent intent = new Intent(CellBroadcastService.CELL_BROADCAST_SERVICE_INTERFACE);
        intent.setPackage(CELL_BROADCAST_SERVICE_PACKAGE);
        if (mCellBroadcastServiceConnection != null
                && mCellBroadcastServiceConnection.getService() == null) {
            if (!mContext.bindService(intent, mCellBroadcastServiceConnection,
                    Context.BIND_AUTO_CREATE)) {
                Log.e(TAG, "Unable to bind to service");
            }
        } else {
            Log.d(TAG, "skipping bindService because connection already exists");
        }
    }

    private void updateLatestAreaInfo() {
        mShowLatestAreaInfo = Resources.getSystem().getBoolean(
                com.android.internal.R.bool.config_showAreaUpdateInfoSettings)
                && mTelephonyManager.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA;

        if (mShowLatestAreaInfo) {
            // Bind cell broadcast service to get the area info. The info will be updated once
            // the service is connected.
            bindCellBroadcastService();
        } else {
            mDialog.removeSettingFromScreen(OPERATOR_INFO_LABEL_ID);
            mDialog.removeSettingFromScreen(OPERATOR_INFO_VALUE_ID);
        }
    }

    private void updateServiceState(ServiceState serviceState) {
        final int state = Utils.getCombinedServiceState(serviceState);
        if (!Utils.isInService(serviceState)) {
            resetSignalStrength();
        }

        String serviceStateValue;

        switch (state) {
            case ServiceState.STATE_IN_SERVICE:
                serviceStateValue = mRes.getString(R.string.radioInfo_service_in);
                break;
            case ServiceState.STATE_OUT_OF_SERVICE:
            case ServiceState.STATE_EMERGENCY_ONLY:
                // Set summary string of service state to radioInfo_service_out when
                // service state is both STATE_OUT_OF_SERVICE & STATE_EMERGENCY_ONLY
                serviceStateValue = mRes.getString(R.string.radioInfo_service_out);
                break;
            case ServiceState.STATE_POWER_OFF:
                serviceStateValue = mRes.getString(R.string.radioInfo_service_off);
                break;
            default:
                serviceStateValue = mRes.getString(R.string.radioInfo_unknown);
                break;
        }

        mDialog.setText(SERVICE_STATE_VALUE_ID, serviceStateValue);
    }

    private void updateSignalStrength(SignalStrength signalStrength) {
        if (signalStrength == null) {
            return;
        }
        final int subscriptionId = mSubscriptionInfo.getSubscriptionId();
        final PersistableBundle carrierConfig =
                mCarrierConfigManager.getConfigForSubId(subscriptionId);
        // by default we show the signal strength
        boolean showSignalStrength = true;
        if (carrierConfig != null) {
            showSignalStrength = carrierConfig.getBoolean(
                    CarrierConfigManager.KEY_SHOW_SIGNAL_STRENGTH_IN_SIM_STATUS_BOOL);
        }
        if (!showSignalStrength) {
            mDialog.removeSettingFromScreen(SIGNAL_STRENGTH_LABEL_ID);
            mDialog.removeSettingFromScreen(SIGNAL_STRENGTH_VALUE_ID);
            return;
        }

        ServiceState serviceState = mTelephonyManager.getServiceState();
        if (serviceState == null || !Utils.isInService(serviceState)) {
            return;
        }

        int signalDbm = getDbm(signalStrength);
        int signalAsu = getAsuLevel(signalStrength);

        if (signalDbm == -1) {
            signalDbm = 0;
        }

        if (signalAsu == -1) {
            signalAsu = 0;
        }

        mDialog.setText(SIGNAL_STRENGTH_VALUE_ID, mRes.getString(R.string.sim_signal_strength,
                signalDbm, signalAsu));
    }

    private void resetSignalStrength() {
        mDialog.setText(SIGNAL_STRENGTH_VALUE_ID, "0");
    }

    private void updateNetworkType() {
        // Whether EDGE, UMTS, etc...
        String dataNetworkTypeName = null;
        String voiceNetworkTypeName = null;
        final int subId = mSubscriptionInfo.getSubscriptionId();
        final int actualDataNetworkType = mTelephonyManager.getDataNetworkType();
        final int actualVoiceNetworkType = mTelephonyManager.getVoiceNetworkType();
        if (TelephonyManager.NETWORK_TYPE_UNKNOWN != actualDataNetworkType) {
            dataNetworkTypeName = getNetworkTypeName(actualDataNetworkType);
        }
        if (TelephonyManager.NETWORK_TYPE_UNKNOWN != actualVoiceNetworkType) {
            voiceNetworkTypeName = getNetworkTypeName(actualVoiceNetworkType);
        }

        boolean show4GForLTE = false;
        final PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(subId);
        if (carrierConfig != null) {
            show4GForLTE = carrierConfig.getBoolean(
                    CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL);
        }

        if (show4GForLTE) {
            if ("LTE".equals(dataNetworkTypeName)) {
                dataNetworkTypeName = "4G";
            }
            if ("LTE".equals(voiceNetworkTypeName)) {
                voiceNetworkTypeName = "4G";
            }
        }

        mDialog.setText(CELL_VOICE_NETWORK_TYPE_VALUE_ID, voiceNetworkTypeName);
        mDialog.setText(CELL_DATA_NETWORK_TYPE_VALUE_ID, dataNetworkTypeName);
    }

    private void updateRoamingStatus(ServiceState serviceState) {
        if (serviceState.getRoaming()) {
            mDialog.setText(ROAMING_INFO_VALUE_ID, mRes.getString(R.string.radioInfo_roaming_in));
        } else {
            mDialog.setText(ROAMING_INFO_VALUE_ID, mRes.getString(R.string.radioInfo_roaming_not));
        }
    }

    private void updateIccidNumber() {
        final int subscriptionId = mSubscriptionInfo.getSubscriptionId();
        final PersistableBundle carrierConfig =
                mCarrierConfigManager.getConfigForSubId(subscriptionId);
        // do not show iccid by default
        boolean showIccId = false;
        if (carrierConfig != null) {
            showIccId = carrierConfig.getBoolean(
                    CarrierConfigManager.KEY_SHOW_ICCID_IN_SIM_STATUS_BOOL);
        }
        if (!showIccId) {
            mDialog.removeSettingFromScreen(ICCID_INFO_LABEL_ID);
            mDialog.removeSettingFromScreen(ICCID_INFO_VALUE_ID);
        } else {
            mDialog.setText(ICCID_INFO_VALUE_ID, mTelephonyManager.getSimSerialNumber());
        }
    }

    private void updateEid() {
        boolean shouldHaveEid = false;
        String eid = null;

        if (mTelephonyManager.getPhoneCount() > PhoneConstants.MAX_PHONE_COUNT_SINGLE_SIM) {
            // Get EID per-SIM in multi-SIM mode
            Map<Integer, Integer> mapping = mTelephonyManager.getLogicalToPhysicalSlotMapping();
            int pSlotId = mapping.getOrDefault(mSlotIndex,
                    SubscriptionManager.INVALID_SIM_SLOT_INDEX);

            if (pSlotId != SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
                List<UiccCardInfo> infos = mTelephonyManager.getUiccCardsInfo();

                for (UiccCardInfo info : infos) {
                    if (info.getSlotIndex() == pSlotId) {
                        if (info.isEuicc()) {
                            shouldHaveEid = true;
                            eid = info.getEid();

                            if (TextUtils.isEmpty(eid)) {
                                eid = mEuiccManager.createForCardId(info.getCardId()).getEid();
                            }
                        }
                        break;
                    }
                }
            }
        } else if (mEuiccManager.isEnabled()) {
            // Get EID of default eSIM in single-SIM mode
            shouldHaveEid = true;
            eid = mEuiccManager.getEid();
        }

        if (!shouldHaveEid) {
            mDialog.removeSettingFromScreen(EID_INFO_LABEL_ID);
            mDialog.removeSettingFromScreen(EID_INFO_VALUE_ID);
        } else if (!TextUtils.isEmpty(eid)) {
            mDialog.setText(EID_INFO_VALUE_ID, eid);
        }
    }

    private void updateImsRegistrationState() {
        final int subscriptionId = mSubscriptionInfo.getSubscriptionId();
        final PersistableBundle carrierConfig =
                mCarrierConfigManager.getConfigForSubId(subscriptionId);
        final boolean showImsRegState = carrierConfig == null ? false :
                carrierConfig.getBoolean(
                        CarrierConfigManager.KEY_SHOW_IMS_REGISTRATION_STATUS_BOOL);
        if (showImsRegState) {
            final boolean isImsRegistered = mTelephonyManager.isImsRegistered(subscriptionId);
            mDialog.setText(IMS_REGISTRATION_STATE_VALUE_ID, mRes.getString(isImsRegistered ?
                    R.string.ims_reg_status_registered : R.string.ims_reg_status_not_registered));
        } else {
            mDialog.removeSettingFromScreen(IMS_REGISTRATION_STATE_LABEL_ID);
            mDialog.removeSettingFromScreen(IMS_REGISTRATION_STATE_VALUE_ID);
        }
    }

    private SubscriptionInfo getPhoneSubscriptionInfo(int slotId) {
        return SubscriptionManager.from(mContext).getActiveSubscriptionInfoForSimSlotIndex(slotId);
    }

    private int getDbm(SignalStrength signalStrength) {
        List<CellSignalStrength> cellSignalStrengthList = signalStrength.getCellSignalStrengths();
        int dbm = -1;
        if (cellSignalStrengthList == null) {
            return dbm;
        }

        for (CellSignalStrength cell : cellSignalStrengthList) {
            if (cell.getDbm() != -1) {
                dbm = cell.getDbm();
                break;
            }
        }

        return dbm;
    }

    private int getAsuLevel(SignalStrength signalStrength) {
        List<CellSignalStrength> cellSignalStrengthList = signalStrength.getCellSignalStrengths();
        int asu = -1;
        if (cellSignalStrengthList == null) {
            return asu;
        }

        for (CellSignalStrength cell : cellSignalStrengthList) {
            if (cell.getAsuLevel() != -1) {
                asu = cell.getAsuLevel();
                break;
            }
        }

        return asu;
    }

    @VisibleForTesting
    PhoneStateListener getPhoneStateListener() {
        return new PhoneStateListener() {
            @Override
            public void onDataConnectionStateChanged(int state) {
                updateDataState(state);
                updateNetworkType();
            }

            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                updateSignalStrength(signalStrength);
            }

            @Override
            public void onServiceStateChanged(ServiceState serviceState) {
                updateNetworkProvider();
                updateServiceState(serviceState);
                updateRoamingStatus(serviceState);
            }
        };
    }

    @VisibleForTesting
    static String getNetworkTypeName(@Annotation.NetworkType int type) {
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return "GPRS";
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return "EDGE";
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return "UMTS";
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                return "HSDPA";
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return "HSUPA";
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return "HSPA";
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return "CDMA";
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                return "CDMA - EvDo rev. 0";
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                return "CDMA - EvDo rev. A";
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return "CDMA - EvDo rev. B";
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                return "CDMA - 1xRTT";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "LTE";
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                return "CDMA - eHRPD";
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "iDEN";
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "HSPA+";
            case TelephonyManager.NETWORK_TYPE_GSM:
                return "GSM";
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                return "TD_SCDMA";
            case TelephonyManager.NETWORK_TYPE_IWLAN:
                return "IWLAN";
            case TelephonyManager.NETWORK_TYPE_LTE_CA:
                return "LTE_CA";
            case TelephonyManager.NETWORK_TYPE_NR:
                return "NR";
            default:
                return "UNKNOWN";
        }
    }
}
