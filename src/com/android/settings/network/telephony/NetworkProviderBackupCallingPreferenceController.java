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
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.network.SubscriptionUtil;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;

import java.util.List;

/**
 * Preference controller for "Backup Calling" summary list
 */
public class NetworkProviderBackupCallingPreferenceController extends
        BasePreferenceController implements LifecycleObserver {

    private static final String TAG = "NetProvBackupCallingCtrl";
    private static final String KEY_PREFERENCE_CATEGORY = "provider_model_backup_calling_category";

    private PreferenceCategory mPreferenceCategory;
    private PreferenceScreen mPreferenceScreen;
    private NetworkProviderBackupCallingGroup mNetworkProviderBackupCallingGroup;
    private List<SubscriptionInfo> mSubscriptionList;

    /**
     * Preference controller for "Backup Calling" summary list
     */
    public NetworkProviderBackupCallingPreferenceController(Context context, String key) {
        super(context, key);
    }

    protected NetworkProviderBackupCallingGroup createBackupCallingControllerForSub(
            Lifecycle lifecycle, List<SubscriptionInfo> subscriptionList) {
        return new NetworkProviderBackupCallingGroup(mContext, lifecycle, subscriptionList,
                KEY_PREFERENCE_CATEGORY);
    }

    /**
     * Initialize the binding with Lifecycle
     *
     * @param lifecycle Lifecycle of UI which owns this Preference
     */
    public void init(Lifecycle lifecycle) {
        mSubscriptionList = getActiveSubscriptionList();
        mNetworkProviderBackupCallingGroup = createBackupCallingControllerForSub(lifecycle,
                mSubscriptionList);
    }

    private List<SubscriptionInfo> getActiveSubscriptionList() {
        SubscriptionManager subscriptionManager =
                mContext.getSystemService(SubscriptionManager.class);
        return SubscriptionUtil.getActiveSubscriptions(subscriptionManager);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mNetworkProviderBackupCallingGroup == null
                || mSubscriptionList == null
                || mSubscriptionList.size() < 2) {
            return CONDITIONALLY_UNAVAILABLE;
        } else {
            return AVAILABLE;
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceScreen = screen;
        mPreferenceCategory = screen.findPreference(KEY_PREFERENCE_CATEGORY);
        mPreferenceCategory.setVisible(isAvailable());
        mNetworkProviderBackupCallingGroup.displayPreference(screen);
    }
}
