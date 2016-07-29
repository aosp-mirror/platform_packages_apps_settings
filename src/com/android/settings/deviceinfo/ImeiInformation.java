/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;

import android.text.style.TtsSpan;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class ImeiInformation extends SettingsPreferenceFragment {

    private static final String KEY_PRL_VERSION = "prl_version";
    private static final String KEY_MIN_NUMBER = "min_number";
    private static final String KEY_MEID_NUMBER = "meid_number";
    private static final String KEY_ICC_ID = "icc_id";
    private static final String KEY_IMEI = "imei";
    private static final String KEY_IMEI_SV = "imei_sv";

    private SubscriptionManager mSubscriptionManager;
    private boolean isMultiSIM = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSubscriptionManager = SubscriptionManager.from(getContext());
        final TelephonyManager telephonyManager =
            (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        initPreferenceScreen(telephonyManager.getSimCount());
    }

    // Since there are multiple phone for dsds, therefore need to show information for different
    // phones.
    private void initPreferenceScreen(int slotCount) {
        isMultiSIM = (slotCount > 1);
        for (int slotId = 0; slotId < slotCount; slotId ++) {
            addPreferencesFromResource(R.xml.device_info_phone_status);
            setPreferenceValue(slotId);
            setNewKey(slotId);
        }
    }

    private void setPreferenceValue(int phoneId) {
        final Phone phone = PhoneFactory.getPhone(phoneId);

        if (phone != null) {
            if (phone.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
                setSummaryText(KEY_MEID_NUMBER, phone.getMeid());
                setSummaryText(KEY_MIN_NUMBER, phone.getCdmaMin());

                if (getResources().getBoolean(R.bool.config_msid_enable)) {
                    findPreference(KEY_MIN_NUMBER).setTitle(R.string.status_msid_number);
                }

                setSummaryText(KEY_PRL_VERSION, phone.getCdmaPrlVersion());

                if (phone.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE) {
                    // Show ICC ID and IMEI for LTE device
                    setSummaryText(KEY_ICC_ID, phone.getIccSerialNumber());
                    setSummaryTextAsDigit(KEY_IMEI, phone.getImei());
                    setSummaryTextAsDigit(KEY_IMEI_SV, phone.getDeviceSvn());
                } else {
                    // device is not GSM/UMTS, do not display GSM/UMTS features
                    // check Null in case no specified preference in overlay xml
                    removePreferenceFromScreen(KEY_IMEI_SV);
                    removePreferenceFromScreen(KEY_IMEI);
                    removePreferenceFromScreen(KEY_ICC_ID);
                }
            } else {
                setSummaryTextAsDigit(KEY_IMEI, phone.getImei());
                setSummaryTextAsDigit(KEY_IMEI_SV, phone.getDeviceSvn());
                // device is not CDMA, do not display CDMA features
                // check Null in case no specified preference in overlay xml
                removePreferenceFromScreen(KEY_PRL_VERSION);
                removePreferenceFromScreen(KEY_MEID_NUMBER);
                removePreferenceFromScreen(KEY_MIN_NUMBER);
                removePreferenceFromScreen(KEY_ICC_ID);
            }
        }
    }

    // Modify the preference key with prefix "_", so new added information preference can be set
    // related phone information.
    private void setNewKey(int slotId) {
        final PreferenceScreen prefScreen = getPreferenceScreen();
        final int count = prefScreen.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference pref = prefScreen.getPreference(i);
            String key = pref.getKey();
            if (!key.startsWith("_")){
                key = "_" + key + String.valueOf(slotId);
                pref.setKey(key);
                updateTitle(pref, slotId);
            }
        }
    }

    private void updateTitle(Preference pref, int slotId) {
        if (pref != null) {
            String title = pref.getTitle().toString();
            if (isMultiSIM) {
                // Slot starts from 1, slotId starts from 0 so plus 1
                title += " " + getResources().getString(R.string.slot_number, slotId + 1);
            }
            pref.setTitle(title);
        }
    }

    private void setSummaryText(String key, String text) {
        setSummaryText(key, text, false /* forceDigit */);
    }

    private void setSummaryTextAsDigit(String key, String text) {
        setSummaryText(key, text, true /* forceDigit */);
    }

    private void setSummaryText(String key, CharSequence text, boolean forceDigit) {
        final Preference preference = findPreference(key);

        if (TextUtils.isEmpty(text)) {
            text = getResources().getString(R.string.device_info_default);
        } else if (forceDigit && TextUtils.isDigitsOnly(text)) {
            final Spannable spannable = new SpannableStringBuilder(text);
            final TtsSpan span = new TtsSpan.DigitsBuilder(text.toString()).build();
            spannable.setSpan(span, 0, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text = spannable;
        }

        if (preference != null) {
            preference.setSummary(text);
        }
    }

    /**
     * Removes the specified preference, if it exists.
     * @param key the key for the Preference item
     */
    private void removePreferenceFromScreen(String key) {
        final Preference preference = findPreference(key);
        if (preference != null) {
            getPreferenceScreen().removePreference(preference);
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.DEVICEINFO_IMEI_INFORMATION;
    }
}
