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

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import android.content.Context;
import android.telephony.SubscriptionManager;

import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceScreen;

import com.android.settings.network.AllowedNetworkTypesListener;
import com.android.settings.widget.PreferenceCategoryController;

/**
 * Preference controller for "Network" category
 */
public class NetworkPreferenceCategoryController extends PreferenceCategoryController
        implements LifecycleObserver {

    private PreferenceScreen mPreferenceScreen;
    private AllowedNetworkTypesListener mAllowedNetworkTypesListener;
    protected int mSubId;

    public NetworkPreferenceCategoryController(Context context, String key) {
        super(context, key);
        mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        mAllowedNetworkTypesListener = new AllowedNetworkTypesListener(
                context.getMainExecutor());
        mAllowedNetworkTypesListener.setAllowedNetworkTypesListener(
                () -> updatePreference());
    }

    private void updatePreference() {
        displayPreference(mPreferenceScreen);
    }

    @OnLifecycleEvent(ON_START)
    public void onStart() {
        mAllowedNetworkTypesListener.register(mContext, mSubId);
    }

    @OnLifecycleEvent(ON_STOP)
    public void onStop() {
        mAllowedNetworkTypesListener.unregister(mContext, mSubId);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceScreen = screen;
    }

    NetworkPreferenceCategoryController init(int subId) {
        mSubId = subId;

        return this;
    }
}
