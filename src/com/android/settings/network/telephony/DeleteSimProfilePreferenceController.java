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
import android.telephony.SubscriptionInfo;
import android.telephony.euicc.EuiccManager;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.network.SubscriptionUtil;

/** This controls a preference allowing the user to delete the profile for an eSIM. */
public class DeleteSimProfilePreferenceController extends BasePreferenceController {

    private SubscriptionInfo mSubscriptionInfo;
    private Fragment mParentFragment;
    private int mRequestCode;

    public DeleteSimProfilePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    public void init(int subscriptionId, Fragment parentFragment, int requestCode) {
        mParentFragment = parentFragment;

        for (SubscriptionInfo info : SubscriptionUtil.getAvailableSubscriptions(
                mContext)) {
            if (info.getSubscriptionId() == subscriptionId && info.isEmbedded()) {
                mSubscriptionInfo = info;
                break;
            }
        }
        mRequestCode = requestCode;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference pref = screen.findPreference(getPreferenceKey());
        pref.setOnPreferenceClickListener(p -> {
            final Intent intent = new Intent(EuiccManager.ACTION_DELETE_SUBSCRIPTION_PRIVILEGED);
            intent.putExtra(EuiccManager.EXTRA_SUBSCRIPTION_ID,
                    mSubscriptionInfo.getSubscriptionId());
            mParentFragment.startActivityForResult(intent, mRequestCode);
            return true;
        });
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
