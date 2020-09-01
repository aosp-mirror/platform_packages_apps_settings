/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.network.telephony;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.euicc.EuiccManager;
import android.text.TextUtils;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.security.ConfirmSimDeletionPreferenceController;
import com.android.settings.wifi.dpp.WifiDppUtils;

/** This controls a preference allowing the user to delete the profile for an eSIM. */
public class DeleteSimProfilePreferenceController extends BasePreferenceController {

    private SubscriptionInfo mSubscriptionInfo;
    private Fragment mParentFragment;
    private int mRequestCode;
    private boolean mConfirmationDefaultOn;

    public DeleteSimProfilePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mConfirmationDefaultOn =
                context.getResources()
                        .getBoolean(R.bool.config_sim_deletion_confirmation_default_on);
    }

    public void init(int subscriptionId, Fragment parentFragment, int requestCode) {
        mParentFragment = parentFragment;

        for (SubscriptionInfo info : SubscriptionUtil.getAvailableSubscriptions(mContext)) {
            if (info.getSubscriptionId() == subscriptionId && info.isEmbedded()) {
                mSubscriptionInfo = info;
                break;
            }
        }
        mRequestCode = requestCode;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            boolean confirmDeletion =
                    Settings.Global.getInt(
                            mContext.getContentResolver(),
                            ConfirmSimDeletionPreferenceController.KEY_CONFIRM_SIM_DELETION,
                            mConfirmationDefaultOn ? 1 : 0)
                            == 1;
            if (confirmDeletion) {
                WifiDppUtils.showLockScreen(mContext, () -> deleteSim());
            } else {
                deleteSim();
            }

            return true;
        }

        return false;
    }

    private void deleteSim() {
        final Intent intent = new Intent(EuiccManager.ACTION_DELETE_SUBSCRIPTION_PRIVILEGED);
        intent.putExtra(EuiccManager.EXTRA_SUBSCRIPTION_ID, mSubscriptionInfo.getSubscriptionId());
        mParentFragment.startActivityForResult(intent, mRequestCode);
        // result handled in MobileNetworkSettings
    }

    @Override
    public int getAvailabilityStatus() {
        if (mSubscriptionInfo != null) {
            return AVAILABLE;
        } else {
            return CONDITIONALLY_UNAVAILABLE;
        }
    }
}
