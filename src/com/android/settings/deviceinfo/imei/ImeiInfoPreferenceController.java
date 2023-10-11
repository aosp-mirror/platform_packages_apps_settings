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

import android.content.Context;
import android.os.UserManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.deviceinfo.PhoneNumberSummaryPreference;
import com.android.settings.deviceinfo.simstatus.SlotSimStatus;
import com.android.settings.network.SubscriptionUtil;
import com.android.settingslib.Utils;

/**
 * Controller that manages preference for single and multi sim devices.
 */
public class ImeiInfoPreferenceController extends BasePreferenceController {

    private static final String TAG = "ImeiInfoPreferenceController";

    private static final String KEY_PREFERENCE_CATEGORY = "device_detail_category";
    public static final String DEFAULT_KEY = "imei_info";

    private TelephonyManager mTelephonyManager;
    private Fragment mFragment;
    private SlotSimStatus mSlotSimStatus;

    public ImeiInfoPreferenceController(Context context, String key) {
        super(context, key);
    }

    public void init(Fragment fragment, SlotSimStatus slotSimStatus) {
        mFragment = fragment;
        mSlotSimStatus = slotSimStatus;
    }

    private boolean isMultiSim() {
        return (mSlotSimStatus != null) && (mSlotSimStatus.size() > 1);
    }

    private int keyToSlotIndex(String key) {
        int simSlot = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
        try {
            simSlot = Integer.valueOf(key.replace(DEFAULT_KEY, "")) - 1;
        } catch (Exception exception) {
            Log.i(TAG, "Invalid key : " + key);
        }
        return simSlot;
    }

    private SubscriptionInfo getSubscriptionInfo(int simSlot) {
        return (mSlotSimStatus == null) ? null : mSlotSimStatus.getSubscriptionInfo(simSlot);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if ((!SubscriptionUtil.isSimHardwareVisible(mContext)) || (mSlotSimStatus == null)) {
            return;
        }
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        Preference preference = screen.findPreference(DEFAULT_KEY);
        if (!isAvailable() || preference == null || !preference.isVisible()) {
            return;
        }
        PreferenceCategory category = screen.findPreference(KEY_PREFERENCE_CATEGORY);

        int imeiPreferenceOrder = preference.getOrder();
        screen.removePreference(preference);
        preference.setVisible(false);

        // Add additional preferences for each imei slot in the device
        for (int simSlotNumber = 0; simSlotNumber < mSlotSimStatus.size(); simSlotNumber++) {
            Preference multiImeiPreference = createNewPreference(screen.getContext());
            multiImeiPreference.setOrder(imeiPreferenceOrder + 1 + simSlotNumber);
            multiImeiPreference.setKey(DEFAULT_KEY + (1 + simSlotNumber));
            multiImeiPreference.setEnabled(true);
            multiImeiPreference.setCopyingEnabled(true);
            category.addPreference(multiImeiPreference);
       }
    }

    @Override
    public void updateState(Preference preference) {
        updatePreference(preference, keyToSlotIndex(preference.getKey()));
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getString(R.string.device_info_protected_single_press);
    }

    private CharSequence getSummary(int simSlot) {
        final int phoneType = getPhoneType(simSlot);
        return phoneType == PHONE_TYPE_CDMA ? mTelephonyManager.getMeid(simSlot)
                : mTelephonyManager.getImei(simSlot);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        final int simSlot = keyToSlotIndex(preference.getKey());
        if (simSlot < 0) {
            return false;
        }

        ImeiInfoDialogFragment.show(mFragment, simSlot, preference.getTitle().toString());
        preference.setSummary(getSummary(simSlot));
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        boolean isAvailable = SubscriptionUtil.isSimHardwareVisible(mContext) &&
                mContext.getSystemService(UserManager.class).isAdminUser() &&
                !Utils.isWifiOnly(mContext);
        return isAvailable ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean useDynamicSliceSummary() {
        return true;
    }

    @VisibleForTesting
    protected void updatePreference(Preference preference, int simSlot) {
        preference.setTitle(getTitle(simSlot));
        preference.setSummary(getSummary());
    }

    private CharSequence getTitleForGsmPhone(int simSlot, boolean isPrimaryImei) {
        int titleId = isPrimaryImei ? R.string.imei_multi_sim_primary : R.string.imei_multi_sim;
        return isMultiSim() ? mContext.getString(titleId, simSlot + 1)
                : mContext.getString(R.string.status_imei);
    }

    private CharSequence getTitleForCdmaPhone(int simSlot, boolean isPrimaryImei) {
        int titleId = isPrimaryImei ? R.string.meid_multi_sim_primary : R.string.meid_multi_sim;
        return isMultiSim() ? mContext.getString(titleId, simSlot + 1)
                : mContext.getString(R.string.status_meid_number);
    }

    protected boolean isPrimaryImei(int simSlot) {
        CharSequence imei = getSummary(simSlot);
        if (imei == null) {
            return false;
        }
        String primaryImei = null;
        try {
            primaryImei = mTelephonyManager.getPrimaryImei();
        } catch (Exception exception) {
            Log.i(TAG, "PrimaryImei not available. " + exception);
        }
        return (primaryImei != null) && primaryImei.equals(imei.toString());
    }

    private CharSequence getTitle(int simSlot) {
        boolean isPrimaryImei = isMultiSim() && isPrimaryImei(simSlot);
        final int phoneType = getPhoneType(simSlot);
        return phoneType == PHONE_TYPE_CDMA ? getTitleForCdmaPhone(simSlot, isPrimaryImei)
                : getTitleForGsmPhone(simSlot, isPrimaryImei);
    }

    public int getPhoneType(int slotIndex) {
        SubscriptionInfo subInfo = getSubscriptionInfo(slotIndex);
        return mTelephonyManager.getCurrentPhoneType(subInfo != null ? subInfo.getSubscriptionId()
                : SubscriptionManager.DEFAULT_SUBSCRIPTION_ID);
    }

    @VisibleForTesting
    Preference createNewPreference(Context context) {
        return new PhoneNumberSummaryPreference(context);
    }
}
