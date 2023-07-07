/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.network;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.widget.PreferenceCategoryController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;

public class NetworkProviderSimsCategoryController extends PreferenceCategoryController implements
        LifecycleObserver {
    private static final String LOG_TAG = "NetworkProviderSimsCategoryController";
    private static final String KEY_PREFERENCE_CATEGORY_SIM = "provider_model_sim_category";
    private NetworkProviderSimListController mNetworkProviderSimListController;
    private PreferenceCategory mPreferenceCategory;

    public NetworkProviderSimsCategoryController(Context context, String key, Lifecycle lifecycle,
            LifecycleOwner lifecycleOwner) {
        super(context, key);
        mNetworkProviderSimListController =
                new NetworkProviderSimListController(mContext, lifecycle, lifecycleOwner);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mNetworkProviderSimListController == null
                || !mNetworkProviderSimListController.isAvailable()) {
            return CONDITIONALLY_UNAVAILABLE;
        } else {
            return AVAILABLE;
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mNetworkProviderSimListController.displayPreference(screen);
        mPreferenceCategory = screen.findPreference(KEY_PREFERENCE_CATEGORY_SIM);
        if (mPreferenceCategory == null) {
            Log.d(LOG_TAG, "displayPreference(), Can not find the category.");
            return;
        }
        mPreferenceCategory.setVisible(isAvailable());
    }
}
