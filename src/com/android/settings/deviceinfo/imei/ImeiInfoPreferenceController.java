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

import static android.telephony.TelephonyManager.PHONE_TYPE_CDMA;

import android.app.Fragment;
import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.TelephonyManager;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.deviceinfo.AbstractSimStatusImeiInfoPreferenceController;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller that manages preference for single and multi sim devices.
 */
public class ImeiInfoPreferenceController extends
        AbstractSimStatusImeiInfoPreferenceController implements PreferenceControllerMixin {

    private static final String KEY_IMEI_INFO = "imei_info";

    private final boolean mIsMultiSim;
    private final TelephonyManager mTelephonyManager;
    private final List<Preference> mPreferenceList = new ArrayList<>();
    private final Fragment mFragment;

    public ImeiInfoPreferenceController(Context context, Fragment fragment) {
        super(context);

        mFragment = fragment;
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mIsMultiSim = mTelephonyManager.getPhoneCount() > 1;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_IMEI_INFO;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference preference = screen.findPreference(getPreferenceKey());
        if (!isAvailable() || preference == null || !preference.isVisible()) {
            return;
        }

        mPreferenceList.add(preference);
        updatePreference(preference, 0 /* sim slot */);

        final int imeiPreferenceOrder = preference.getOrder();
        // Add additional preferences for each sim in the device
        for (int simSlotNumber = 1; simSlotNumber < mTelephonyManager.getPhoneCount();
                simSlotNumber++) {
            final Preference multiSimPreference = createNewPreference(screen.getContext());
            multiSimPreference.setOrder(imeiPreferenceOrder + simSlotNumber);
            multiSimPreference.setKey(KEY_IMEI_INFO + simSlotNumber);
            screen.addPreference(multiSimPreference);
            mPreferenceList.add(multiSimPreference);
            updatePreference(multiSimPreference, simSlotNumber);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        final int simSlot = mPreferenceList.indexOf(preference);
        if (simSlot == -1) {
            return false;
        }

        ImeiInfoDialogFragment.show(mFragment, simSlot, preference.getTitle().toString());
        return true;
    }

    private void updatePreference(Preference preference, int simSlot) {
        final int phoneType = mTelephonyManager.getPhoneType();
        if (phoneType == PHONE_TYPE_CDMA) {
            preference.setTitle(getTitleForCdmaPhone(simSlot));
            preference.setSummary(getMeid(simSlot));
        } else {
            // GSM phone
            preference.setTitle(getTitleForGsmPhone(simSlot));
            preference.setSummary(mTelephonyManager.getImei(simSlot));
        }
    }

    private CharSequence getTitleForGsmPhone(int simSlot) {
        return mIsMultiSim ? mContext.getString(R.string.imei_multi_sim, simSlot + 1)
                : mContext.getString(R.string.status_imei);
    }

    private CharSequence getTitleForCdmaPhone(int simSlot) {
        return mIsMultiSim ? mContext.getString(R.string.meid_multi_sim, simSlot + 1)
                : mContext.getString(R.string.status_meid_number);
    }

    @VisibleForTesting
    String getMeid(int simSlot) {
        return mTelephonyManager.getMeid(simSlot);
    }

    @VisibleForTesting
    Preference createNewPreference(Context context) {
        return new Preference(context);
    }
}
