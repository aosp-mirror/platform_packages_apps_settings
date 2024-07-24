/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.database.ContentObserver;
import android.net.Uri;
import android.os.PersistableBundle;
import android.provider.Telephony;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.ims.ImsManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.network.CarrierConfigCache;
import com.android.settings.network.SubscriptionUtil;

/**
 * Controller for the "Contact Discovery" option present in MobileNetworkSettings.
 */
public class ContactDiscoveryPreferenceController extends TelephonyTogglePreferenceController
        implements LifecycleObserver {
    private static final String TAG = "ContactDiscoveryPref";
    private static final Uri UCE_URI = Uri.withAppendedPath(Telephony.SimInfo.CONTENT_URI,
            Telephony.SimInfo.COLUMN_IMS_RCS_UCE_ENABLED);

    private ImsManager mImsManager;
    private CarrierConfigCache mCarrierConfigCache;
    private ContentObserver mUceSettingObserver;
    private FragmentManager mFragmentManager;

    @VisibleForTesting
    public Preference preference;

    public ContactDiscoveryPreferenceController(Context context, String key) {
        super(context, key);
        mImsManager = mContext.getSystemService(ImsManager.class);
        mCarrierConfigCache = CarrierConfigCache.getInstance(context);
    }

    void init(FragmentManager fragmentManager, int subId) {
        mFragmentManager = fragmentManager;
        mSubId = subId;
    }

    @Override
    public boolean isChecked() {
        return MobileNetworkUtils.isContactDiscoveryEnabled(mImsManager, mSubId);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        registerUceObserver();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        unregisterUceObserver();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isChecked) {
            showContentDiscoveryDialog();
            // launch dialog and wait for activity to return and ContentObserver to fire to update.
            return false;
        }
        MobileNetworkUtils.setContactDiscoveryEnabled(mImsManager, mSubId, false /*isEnabled*/);
        return true;
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        PersistableBundle bundle = mCarrierConfigCache.getConfigForSubId(subId);
        boolean shouldShowPresence = bundle != null
                && (bundle.getBoolean(
                CarrierConfigManager.KEY_USE_RCS_PRESENCE_BOOL, false /*default*/)
                || bundle.getBoolean(
                CarrierConfigManager.Ims.KEY_RCS_BULK_CAPABILITY_EXCHANGE_BOOL, false /*default*/));
        return shouldShowPresence ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        preference = screen.findPreference(getPreferenceKey());
    }

    private void registerUceObserver() {
        mUceSettingObserver = new ContentObserver(mContext.getMainThreadHandler()) {
            @Override
            public void onChange(boolean selfChange) {
                onChange(selfChange, null /*uri*/);
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                Log.d(TAG, "UCE setting changed, re-evaluating.");
                TwoStatePreference switchPref = (TwoStatePreference) preference;
                switchPref.setChecked(isChecked());
            }
        };
        mContext.getContentResolver().registerContentObserver(UCE_URI, true /*notifyForDecendants*/,
                mUceSettingObserver);
    }

    private void unregisterUceObserver() {
        mContext.getContentResolver().unregisterContentObserver(mUceSettingObserver);
    }

    private void showContentDiscoveryDialog() {
        ContactDiscoveryDialogFragment dialog = ContactDiscoveryDialogFragment.newInstance(
                mSubId, getCarrierDisplayName(preference.getContext()));
        dialog.show(mFragmentManager, ContactDiscoveryDialogFragment.getFragmentTag(mSubId));
    }

    private CharSequence getCarrierDisplayName(Context context) {
        CharSequence result = "";

        for (SubscriptionInfo info : SubscriptionUtil.getAvailableSubscriptions(context)) {
            if (mSubId == info.getSubscriptionId()) {
                result = SubscriptionUtil.getUniqueSubscriptionDisplayName(info, context);
                break;
            }
        }
        return result;
    }
}
