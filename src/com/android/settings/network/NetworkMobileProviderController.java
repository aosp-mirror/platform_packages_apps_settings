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

package com.android.settings.network;

import android.content.Context;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.wifi.WifiPickerTrackerHelper;
import com.android.settingslib.core.lifecycle.Lifecycle;

/**
 * This controls mobile network display of the internet page that only appears when there
 * are active mobile subscriptions. It shows an overview of available mobile network
 * connections with an entry for each subscription.
 *
 *  {@link NetworkMobileProviderController} is used to show subscription status on internet
 *  page for provider model. This original class can refer to {@link MultiNetworkHeaderController},
 *
  */
public class NetworkMobileProviderController extends BasePreferenceController implements
        SubscriptionsPreferenceController.UpdateListener {

    private static final String TAG = NetworkMobileProviderController.class.getSimpleName();

    public static final String PREF_KEY_PROVIDER_MOBILE_NETWORK = "provider_model_mobile_network";
    private static final int PREFERENCE_START_ORDER = 10;

    private PreferenceCategory mPreferenceCategory;
    private PreferenceScreen mPreferenceScreen;

    private SubscriptionsPreferenceController mSubscriptionsController;

    private int mOriginalExpandedChildrenCount;
    private boolean mHide;

    public NetworkMobileProviderController(Context context, String key) {
        super(context, key);
    }

    /**
     * Initialize NetworkMobileProviderController
     * @param lifecycle Lifecycle of Settings
     */
    public void init(Lifecycle lifecycle) {
        mSubscriptionsController = createSubscriptionsController(lifecycle);
    }

    @VisibleForTesting
    SubscriptionsPreferenceController createSubscriptionsController(Lifecycle lifecycle) {
        if (mSubscriptionsController == null) {
            return new SubscriptionsPreferenceController(
                    mContext,
                    lifecycle,
                    this,
                    PREF_KEY_PROVIDER_MOBILE_NETWORK,
                    PREFERENCE_START_ORDER);
        }
        return mSubscriptionsController;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (mSubscriptionsController == null) {
            Log.e(TAG, "[displayPreference] SubscriptionsController is null.");
            return;
        }
        mPreferenceScreen = screen;
        mOriginalExpandedChildrenCount = mPreferenceScreen.getInitialExpandedChildrenCount();
        mPreferenceCategory = screen.findPreference(PREF_KEY_PROVIDER_MOBILE_NETWORK);
        mPreferenceCategory.setVisible(isAvailable());
        // TODO(tomhsu) For the provider model, subscriptionsController shall do more
        // implementation of preference type change and summary control.
        mSubscriptionsController.displayPreference(screen);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mHide || mSubscriptionsController == null) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return mSubscriptionsController.isAvailable() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
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

    public void setWifiPickerTrackerHelper(WifiPickerTrackerHelper helper) {
        if (mSubscriptionsController != null) {
            mSubscriptionsController.setWifiPickerTrackerHelper(helper);
        }
    }

    /**
     * Hides the preference.
     */
    public void hidePreference(boolean hide, boolean immediately) {
        mHide = hide;
        if (immediately) {
            mPreferenceCategory.setVisible(hide ? false : isAvailable());
        }
    }
}
