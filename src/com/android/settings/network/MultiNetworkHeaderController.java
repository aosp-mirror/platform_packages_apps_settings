/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.app.settings.SettingsEnums;
import android.content.Context;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.wifi.WifiConnectionPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

// This controls a header at the top of the Network & internet page that only appears when there
// are two or more active mobile subscriptions. It shows an overview of available network
// connections with an entry for wifi (if connected) and an entry for each subscription.
public class MultiNetworkHeaderController extends BasePreferenceController implements
        WifiConnectionPreferenceController.UpdateListener,
        SubscriptionsPreferenceController.UpdateListener {
    public static final String TAG = "MultiNetworkHdrCtrl";

    private WifiConnectionPreferenceController mWifiController;
    private SubscriptionsPreferenceController mSubscriptionsController;
    private PreferenceCategory mPreferenceCategory;
    private PreferenceScreen mPreferenceScreen;
    private int mOriginalExpandedChildrenCount;

    public MultiNetworkHeaderController(Context context, String key) {
        super(context, key);
    }

    public void init(Lifecycle lifecycle) {
        mWifiController = createWifiController(lifecycle);
        mSubscriptionsController = createSubscriptionsController(lifecycle);
    }

    @VisibleForTesting
    WifiConnectionPreferenceController createWifiController(Lifecycle lifecycle) {
        final int prefOrder = 0;
        return new WifiConnectionPreferenceController(mContext, lifecycle, this, mPreferenceKey,
                prefOrder, SettingsEnums.SETTINGS_NETWORK_CATEGORY);
    }

    @VisibleForTesting
    SubscriptionsPreferenceController createSubscriptionsController(Lifecycle lifecycle) {
        final int prefStartOrder = 10;
        return new SubscriptionsPreferenceController(mContext, lifecycle, this, mPreferenceKey,
                prefStartOrder);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceScreen = screen;
        mOriginalExpandedChildrenCount = mPreferenceScreen.getInitialExpandedChildrenCount();
        mPreferenceCategory = screen.findPreference(mPreferenceKey);
        mPreferenceCategory.setVisible(isAvailable());
        mWifiController.displayPreference(screen);
        mSubscriptionsController.displayPreference(screen);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mSubscriptionsController == null || !mSubscriptionsController.isAvailable()) {
            return CONDITIONALLY_UNAVAILABLE;
        } else {
            return AVAILABLE;
        }
    }

    @Override
    public void onChildrenUpdated() {
        final boolean available = isAvailable();
        // TODO(b/129893781) we need a better way to express where the advanced collapsing starts
        // for preference groups that have items dynamically added/removed in the top expanded
        // section.
        if (mOriginalExpandedChildrenCount != Integer.MAX_VALUE) {
            if (available) {
                mPreferenceScreen.setInitialExpandedChildrenCount(
                        mOriginalExpandedChildrenCount + mPreferenceCategory.getPreferenceCount());
            } else {
                mPreferenceScreen.setInitialExpandedChildrenCount(mOriginalExpandedChildrenCount);
            }
        }
        mPreferenceCategory.setVisible(available);
    }
}
