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

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.deviceinfo.AbstractSimStatusImeiInfoPreferenceController;

import java.util.ArrayList;
import java.util.List;

public class SimStatusPreferenceController extends
        AbstractSimStatusImeiInfoPreferenceController implements PreferenceControllerMixin {

    private static final String KEY_SIM_STATUS = "sim_status";
    private static final String KEY_PREFERENCE_CATEGORY = "device_detail_category";

    private final TelephonyManager mTelephonyManager;
    private final SubscriptionManager mSubscriptionManager;
    private final Fragment mFragment;
    private final List<Preference> mPreferenceList = new ArrayList<>();

    public SimStatusPreferenceController(Context context, Fragment fragment) {
        super(context);

        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mSubscriptionManager = (SubscriptionManager) context.getSystemService(
                Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        mFragment = fragment;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SIM_STATUS;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference preference = screen.findPreference(getPreferenceKey());
        if (!isAvailable() || preference == null || !preference.isVisible()) {
            return;
        }
        final PreferenceCategory category = screen.findPreference(KEY_PREFERENCE_CATEGORY);

        mPreferenceList.add(preference);

        final int simStatusOrder = preference.getOrder();
        // Add additional preferences for each sim in the device
        for (int simSlotNumber = 1; simSlotNumber < mTelephonyManager.getPhoneCount();
                simSlotNumber++) {
            final Preference multiSimPreference = createNewPreference(screen.getContext());
            multiSimPreference.setCopyingEnabled(true);
            multiSimPreference.setOrder(simStatusOrder + simSlotNumber);
            multiSimPreference.setKey(KEY_SIM_STATUS + simSlotNumber);
            category.addPreference(multiSimPreference);
            mPreferenceList.add(multiSimPreference);
        }
    }

    @Override
    public void updateState(Preference preference) {
        for (int simSlotNumber = 0; simSlotNumber < mPreferenceList.size(); simSlotNumber++) {
            final Preference simStatusPreference = mPreferenceList.get(simSlotNumber);
            simStatusPreference.setTitle(getPreferenceTitle(simSlotNumber /* sim slot */));
            simStatusPreference.setSummary(getCarrierName(simSlotNumber /* sim slot */));
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        final int simSlot = mPreferenceList.indexOf(preference);
        if (simSlot == -1) {
            return false;
        }

        SimStatusDialogFragment.show(mFragment, simSlot, getPreferenceTitle(simSlot));
        return true;
    }

    private String getPreferenceTitle(int simSlot) {
        return mTelephonyManager.getPhoneCount() > 1 ? mContext.getString(
                R.string.sim_status_title_sim_slot, simSlot + 1) : mContext.getString(
                R.string.sim_status_title);
    }

    private CharSequence getCarrierName(int simSlot) {
        final List<SubscriptionInfo> subscriptionInfoList =
                mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subscriptionInfoList != null) {
            for (SubscriptionInfo info : subscriptionInfoList) {
                if (info.getSimSlotIndex() == simSlot) {
                    return info.getCarrierName();
                }
            }
        }
        return mContext.getText(R.string.device_info_not_available);
    }

    @VisibleForTesting
    Preference createNewPreference(Context context) {
        return new Preference(context);
    }
}
