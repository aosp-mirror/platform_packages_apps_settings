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

import android.content.Context;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

// This controls a header at the top of the Network & internet page that only appears when there
// are two or more active mobile subscriptions. It shows an overview of available network
// connections with an entry for wifi (if connected) and an entry for each subscription.
public class MultiNetworkHeaderController extends BasePreferenceController implements
        SubscriptionsPreferenceController.UpdateListener {
    public static final String TAG = "MultiNetworkHdrCtrl";

    private SubscriptionsPreferenceController mSubscriptionsController;
    private PreferenceCategory mPreferenceCategory;

    public MultiNetworkHeaderController(Context context, String key) {
        super(context, key);
    }

    public void init(Lifecycle lifecycle) {
        mSubscriptionsController = createSubscriptionsController(lifecycle);
        // TODO(asargent) - add in a controller for showing wifi status here
    }

    @VisibleForTesting
    SubscriptionsPreferenceController createSubscriptionsController(Lifecycle lifecycle) {
        return new SubscriptionsPreferenceController(mContext, lifecycle, this, mPreferenceKey, 10);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceCategory = (PreferenceCategory) screen.findPreference(mPreferenceKey);
        mPreferenceCategory.setVisible(isAvailable());
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
        mPreferenceCategory.setVisible(isAvailable());
    }
}
