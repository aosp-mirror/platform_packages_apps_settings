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

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.network.SubscriptionUtil;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Preference controller for "Backup Calling" summary list
 */
public class NetworkProviderBackupCallingPreferenceController extends
        BasePreferenceController implements LifecycleObserver {

    private static final String TAG = "NetProvBackupCallingCtrl";

    private Context mContext;
    private PreferenceCategory mPreferenceCategory;

    /**
     * Preference controller for "Backup Calling" summary list
     */
    public NetworkProviderBackupCallingPreferenceController(Context context, String key) {
        super(context, key);
        mContext = context;
    }

    /**
     * Initialize the binding with Lifecycle
     *
     * @param lifecycle Lifecycle of UI which owns this Preference
     */
    public void init(Lifecycle lifecycle) {
        lifecycle.addObserver(this);
    }

    @Override
    public int getAvailabilityStatus() {
        List<SubscriptionInfo> subList = getActiveSubscriptions();
        if (subList.size() < 2) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return (getPreferences(subList).size() >= 1) ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        PreferenceCategory prefCategory = screen.findPreference(getPreferenceKey());
        updatePreferenceList(prefCategory);
        prefCategory.setVisible(isAvailable());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        // Do nothing in this case since preference is invisible
        if (preference == null) {
            return;
        }
        updatePreferenceList((PreferenceCategory) preference);
    }

    private String getPreferenceKey(int subscriptionId) {
        return getPreferenceKey() + "_subId_" + subscriptionId;
    }

    private SwitchPreference getPreference(SubscriptionInfo subInfo) {
        int subId = subInfo.getSubscriptionId();
        BackupCallingPreferenceController prefCtrl =
                new BackupCallingPreferenceController(mContext, getPreferenceKey(subId));
        prefCtrl.init(subId);
        if (prefCtrl.getAvailabilityStatus(subId) != BasePreferenceController.AVAILABLE) {
            return null;
        }
        SwitchPreference pref = new SwitchPreference(mContext);
        prefCtrl.updateState(pref);
        pref.setTitle(SubscriptionUtil.getUniqueSubscriptionDisplayName(subInfo, mContext));
        return pref;
    }

    private List<SwitchPreference> getPreferences(List<SubscriptionInfo> subList) {
        return subList.stream()
                .map(subInfo -> getPreference(subInfo))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<SubscriptionInfo> getActiveSubscriptions() {
        return SubscriptionUtil.getActiveSubscriptions(
                mContext.getSystemService(SubscriptionManager.class));
    }

    private void updatePreferenceList(PreferenceCategory prefCategory) {
        List<SwitchPreference> prefList = getPreferences(getActiveSubscriptions());

        prefCategory.removeAll();
        for (SwitchPreference pref : prefList) {
            prefCategory.addPreference(pref);
        }
    }
}
