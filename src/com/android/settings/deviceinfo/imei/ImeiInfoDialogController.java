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

package com.android.settings.deviceinfo.imei;

import android.content.Context;
import android.content.res.Resources;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.TtsSpan;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;

public class ImeiInfoDialogController {

    private static final String TAG = "ImeiInfoDialog";

    @VisibleForTesting
    static final int ID_PRL_VERSION_VALUE = R.id.prl_version_value;
    private static final int ID_MIN_NUMBER_LABEL = R.id.min_number_label;
    @VisibleForTesting
    static final int ID_MIN_NUMBER_VALUE = R.id.min_number_value;
    @VisibleForTesting
    static final int ID_MEID_NUMBER_VALUE = R.id.meid_number_value;
    @VisibleForTesting
    static final int ID_IMEI_VALUE = R.id.imei_value;
    @VisibleForTesting
    static final int ID_IMEI_SV_VALUE = R.id.imei_sv_value;
    @VisibleForTesting
    static final int ID_CDMA_SETTINGS = R.id.cdma_settings;
    @VisibleForTesting
    static final int ID_GSM_SETTINGS = R.id.gsm_settings;

    private static CharSequence getTextAsDigits(CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            return "";
        }
        if (TextUtils.isDigitsOnly(text)) {
            final Spannable spannable = new SpannableStringBuilder(text);
            final TtsSpan span = new TtsSpan.DigitsBuilder(text.toString()).build();
            spannable.setSpan(span, 0, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text = spannable;
        }
        return text;
    }

    private final ImeiInfoDialogFragment mDialog;
    private final TelephonyManager mTelephonyManager;
    private final SubscriptionInfo mSubscriptionInfo;
    private final int mSlotId;

    public ImeiInfoDialogController(@NonNull ImeiInfoDialogFragment dialog, int slotId) {
        mDialog = dialog;
        mSlotId = slotId;
        final Context context = dialog.getContext();
        mSubscriptionInfo = context.getSystemService(SubscriptionManager.class)
                .getActiveSubscriptionInfoForSimSlotIndex(slotId);
        TelephonyManager tm = context.getSystemService(TelephonyManager.class);
        if (mSubscriptionInfo != null) {
            mTelephonyManager = context.getSystemService(TelephonyManager.class)
                    .createForSubscriptionId(mSubscriptionInfo.getSubscriptionId());
        } else if(isValidSlotIndex(slotId, tm)) {
            mTelephonyManager = tm;
        } else {
            mTelephonyManager = null;
        }
    }

    /**
     * Sets IMEI/MEID information based on whether the device is CDMA or GSM.
     */
    public void populateImeiInfo() {
        if (mTelephonyManager == null) {
            Log.w(TAG, "TelephonyManager for this slot is null. Invalid slot? id=" + mSlotId);
            return;
        }
        if (mTelephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
            updateDialogForCdmaPhone();
        } else {
            updateDialogForGsmPhone();
        }
    }

    private void updateDialogForCdmaPhone() {
        final Resources res = mDialog.getContext().getResources();
        mDialog.setText(ID_MEID_NUMBER_VALUE, getMeid());
        // MIN needs to read from SIM. So if no SIM, we should not show MIN on UI
        mDialog.setText(ID_MIN_NUMBER_VALUE, mSubscriptionInfo != null
                ? mTelephonyManager.getCdmaMin(mSubscriptionInfo.getSubscriptionId())
                : "");

        if (res.getBoolean(R.bool.config_msid_enable)) {
            mDialog.setText(ID_MIN_NUMBER_LABEL,
                    res.getString(R.string.status_msid_number));
        }

        mDialog.setText(ID_PRL_VERSION_VALUE, getCdmaPrlVersion());

        if ((mSubscriptionInfo != null && isCdmaLteEnabled()) ||
                    (mSubscriptionInfo == null && isSimPresent(mSlotId))) {
            // Show IMEI for LTE device
            mDialog.setText(ID_IMEI_VALUE,
                    getTextAsDigits(mTelephonyManager.getImei(mSlotId)));
            mDialog.setText(ID_IMEI_SV_VALUE,
                    getTextAsDigits(mTelephonyManager.getDeviceSoftwareVersion(mSlotId)));
        } else {
            // device is not GSM/UMTS, do not display GSM/UMTS features
            mDialog.removeViewFromScreen(ID_GSM_SETTINGS);
        }
    }

    private void updateDialogForGsmPhone() {
        mDialog.setText(ID_IMEI_VALUE, getTextAsDigits(mTelephonyManager.getImei(mSlotId)));
        mDialog.setText(ID_IMEI_SV_VALUE,
                getTextAsDigits(mTelephonyManager.getDeviceSoftwareVersion(mSlotId)));
        // device is not CDMA, do not display CDMA features
        mDialog.removeViewFromScreen(ID_CDMA_SETTINGS);
    }

    @VisibleForTesting
    String getCdmaPrlVersion() {
        // PRL needs to read from SIM. So if no SIM, return empty
        return mSubscriptionInfo != null ? mTelephonyManager.getCdmaPrlVersion() : "";
    }

    @VisibleForTesting
    boolean isCdmaLteEnabled() {
        return mTelephonyManager.isLteCdmaEvdoGsmWcdmaEnabled();
    }

    boolean isSimPresent(int slotId) {
        final int simState = mTelephonyManager.getSimState(slotId);
        if ((simState != TelephonyManager.SIM_STATE_ABSENT) &&
                (simState != TelephonyManager.SIM_STATE_UNKNOWN)) {
            return true;
        }
        return false;
    }

    @VisibleForTesting
    String getMeid() {
        return mTelephonyManager.getMeid(mSlotId);
    }

    @VisibleForTesting
    private boolean isValidSlotIndex(int slotIndex, TelephonyManager telephonyManager) {
        return slotIndex >= 0 && slotIndex < telephonyManager.getPhoneCount();
    }
}
