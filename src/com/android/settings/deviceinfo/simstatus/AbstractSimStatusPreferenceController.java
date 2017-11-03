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

import android.app.Fragment;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.deviceinfo.AbstractSimStatusImeiInfoPreferenceController;

import java.util.List;

public abstract class AbstractSimStatusPreferenceController extends
        AbstractSimStatusImeiInfoPreferenceController implements PreferenceControllerMixin {

    protected final boolean mIsMultiSim;
    protected final TelephonyManager mTelephonyManager;
    private final SubscriptionManager mSubscriptionManager;
    private final Fragment mFragment;

    private Preference mPreference;

    public AbstractSimStatusPreferenceController(Context context, Fragment fragment) {
        super(context);

        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mSubscriptionManager = (SubscriptionManager) context.getSystemService(
                Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        mFragment = fragment;
        mIsMultiSim = mTelephonyManager.getPhoneCount() > 1;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference == null) {
            return;
        }

        mPreference.setTitle(getPreferenceTitle());
        mPreference.setSummary(getCarrierName());
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }

        SimStatusDialogFragment.show(mFragment, getSimSlot(), getPreferenceTitle());
        return true;
    }

    /**
     * @return The preference title for the displayed preference.
     */
    protected abstract String getPreferenceTitle();

    /**
     * @return The sim slot to retrieve sim status information about.
     */
    protected abstract int getSimSlot();

    private CharSequence getCarrierName() {
        final List<SubscriptionInfo> subscriptionInfoList =
                mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subscriptionInfoList != null) {
            for (SubscriptionInfo info : subscriptionInfoList) {
                if (info.getSimSlotIndex() == getSimSlot()) {
                    return info.getCarrierName();
                }
            }
        }
        return mContext.getText(R.string.device_info_not_available);
    }
}
