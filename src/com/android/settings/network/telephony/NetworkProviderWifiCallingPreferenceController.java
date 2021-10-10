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

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;

/**
 * Preference controller for "Wifi Calling"
 */
public class NetworkProviderWifiCallingPreferenceController extends
        BasePreferenceController implements LifecycleObserver{

    private static final String TAG = "NetworkProviderWfcController";
    private static final String PREFERENCE_CATEGORY_KEY = "provider_model_calling_category";

    private NetworkProviderWifiCallingGroup mNetworkProviderWifiCallingGroup;
    private PreferenceCategory mPreferenceCategory;
    private PreferenceScreen mPreferenceScreen;

    public NetworkProviderWifiCallingPreferenceController(Context context, String key) {
        super(context, key);
    }

    public void init(Lifecycle lifecycle) {
        mNetworkProviderWifiCallingGroup = createWifiCallingControllerForSub(lifecycle);
    }

    @VisibleForTesting
    protected NetworkProviderWifiCallingGroup createWifiCallingControllerForSub(
            Lifecycle lifecycle) {
        return new NetworkProviderWifiCallingGroup(mContext, lifecycle, PREFERENCE_CATEGORY_KEY);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mNetworkProviderWifiCallingGroup == null
                || !mNetworkProviderWifiCallingGroup.isAvailable()) {
            return UNSUPPORTED_ON_DEVICE;
        } else {
            return AVAILABLE;
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceScreen = screen;
        mPreferenceCategory = screen.findPreference(PREFERENCE_CATEGORY_KEY);
        mPreferenceCategory.setVisible(isAvailable());
        mNetworkProviderWifiCallingGroup.displayPreference(screen);
    }
}
