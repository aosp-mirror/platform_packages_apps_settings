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
import android.os.UserManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.network.SubscriptionUtil;
import com.android.settingslib.Utils;
import com.android.settingslib.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;

public class SimStatusPreferenceController extends BasePreferenceController {

    private static final String KEY_PREFERENCE_CATEGORY = "device_detail_category";

    private Fragment mFragment;
    private SlotSimStatus mSlotSimStatus;
    private Observer<LifecycleOwner> mLifecycleOwnerObserver;
    private Observer mSimChangeObserver;

    public SimStatusPreferenceController(Context context, String prefKey) {
        super(context, prefKey);
    }

    /**
     * Initialize this preference controller.
     * @param fragment parent fragment
     * @param slotSimStatus SlotSimStatus object
     */
    public void init(Fragment fragment, SlotSimStatus slotSimStatus) {
        mFragment = fragment;
        mSlotSimStatus = slotSimStatus;
    }

    /**
     * Get the index of slot for this subscription.
     * @return index of slot
     */
    public int getSimSlotIndex() {
        return mSlotSimStatus == null ? SubscriptionManager.INVALID_SIM_SLOT_INDEX :
                mSlotSimStatus.findSlotIndexByKey(getPreferenceKey());
    }

    @Override
    public int getAvailabilityStatus() {
        if (getSimSlotIndex() == SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
            return UNSUPPORTED_ON_DEVICE;
        }
        boolean isAvailable = SubscriptionUtil.isSimHardwareVisible(mContext) &&
                mContext.getSystemService(UserManager.class).isAdminUser() &&
                !Utils.isWifiOnly(mContext);
        return isAvailable ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if ((!SubscriptionUtil.isSimHardwareVisible(mContext)) || (mSlotSimStatus == null)) {
            return;
        }
        String basePreferenceKey = mSlotSimStatus.getPreferenceKey(
                SubscriptionManager.INVALID_SIM_SLOT_INDEX);
        final Preference preference = screen.findPreference(basePreferenceKey);
        if (!isAvailable() || preference == null || !preference.isVisible()) {
            return;
        }
        final PreferenceCategory category = screen.findPreference(KEY_PREFERENCE_CATEGORY);

        mSlotSimStatus.setBasePreferenceOrdering(preference.getOrder());
        screen.removePreference(preference);
        preference.setVisible(false);

        // Add additional preferences for each sim in the device
        for (int simSlotNumber = 0; simSlotNumber < mSlotSimStatus.size(); simSlotNumber++) {
            final Preference multiSimPreference = createNewPreference(screen.getContext());
            multiSimPreference.setOrder(mSlotSimStatus.getPreferenceOrdering(simSlotNumber));
            multiSimPreference.setKey(mSlotSimStatus.getPreferenceKey(simSlotNumber));
            category.addPreference(multiSimPreference);
        }
    }

    @Override
    public void updateState(Preference preference) {
        if (mFragment == null) {
            return;
        }
        if (mLifecycleOwnerObserver == null) {
            final LiveData<LifecycleOwner> dataLifecycleOwner
                    = mFragment.getViewLifecycleOwnerLiveData();
            mLifecycleOwnerObserver = owner -> {
                if (owner != null) {
                    final int simSlot = getSimSlotIndex();
                    mSimChangeObserver = x -> updateStateBySlot(preference, simSlot);
                    mSlotSimStatus.observe(owner, mSimChangeObserver);
                } else {
                    if (mSimChangeObserver != null) {
                        mSlotSimStatus.removeObserver(mSimChangeObserver);
                        mSimChangeObserver = null;
                    }
                    dataLifecycleOwner.removeObserver(mLifecycleOwnerObserver);
                }
            };
            dataLifecycleOwner.observeForever(mLifecycleOwnerObserver);
        } else if (mSimChangeObserver != null) {
            final int simSlot = getSimSlotIndex();
            updateStateBySlot(preference, simSlot);
        }
    }

    protected void updateStateBySlot(Preference preference, int simSlot) {
        SubscriptionInfo subInfo = getSubscriptionInfo(simSlot);
        preference.setEnabled(subInfo != null);
        preference.setCopyingEnabled(subInfo != null);
        preference.setTitle(getPreferenceTitle(simSlot));
        preference.setSummary(getCarrierName(simSlot));
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        final int simSlot = getSimSlotIndex();
        if (simSlot == -1) {
            return false;
        }

        SimStatusDialogFragment.show(mFragment, simSlot, getPreferenceTitle(simSlot));
        return true;
    }

    private String getPreferenceTitle(int simSlot) {
        return mSlotSimStatus.size() > 1 ? mContext.getString(
                R.string.sim_status_title_sim_slot, simSlot + 1) : mContext.getString(
                R.string.sim_status_title);
    }

    private SubscriptionInfo getSubscriptionInfo(int simSlot) {
        return (mSlotSimStatus == null) ? null : mSlotSimStatus.getSubscriptionInfo(simSlot);
    }

    private CharSequence getCarrierName(int simSlot) {
        SubscriptionInfo subInfo = getSubscriptionInfo(simSlot);
        return (subInfo != null) ? subInfo.getCarrierName() :
                mContext.getText(R.string.device_info_not_available);
    }

    @VisibleForTesting
    Preference createNewPreference(Context context) {
        return new Preference(context);
    }

    @Override
    public void updateDynamicRawDataToIndex(List<SearchIndexableRaw> rawData) {
        int simSlot = getSimSlotIndex();
        SubscriptionInfo subInfo = getSubscriptionInfo(simSlot);
        if (subInfo == null) {
            /**
             * Only add to search when SIM is active
             * (presented in SIM Slot Status as availavle.)
             */
            return;
        }

        /* Have different search keywork when comes to eSIM */
        int keywordId = subInfo.isEmbedded() ?
                R.string.keywords_sim_status_esim : R.string.keywords_sim_status;

        SearchIndexableRaw data = new SearchIndexableRaw(mContext);
        data.key = getPreferenceKey();
        data.title = getPreferenceTitle(simSlot);
        data.screenTitle = mContext.getString(R.string.about_settings);
        data.keywords = mContext.getString(keywordId).toString();
        rawData.add(data);
    }
}
