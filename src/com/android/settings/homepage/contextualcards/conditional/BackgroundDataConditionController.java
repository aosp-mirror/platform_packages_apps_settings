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

package com.android.settings.homepage.contextualcards.conditional;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkPolicyManager;

import com.android.settings.Settings;

import java.util.Objects;

public class BackgroundDataConditionController implements ConditionalCardController {
    static final int ID = Objects.hash("BackgroundDataConditionController");

    private final Context mAppContext;
    private final ConditionManager mConditionManager;
    private final NetworkPolicyManager mNetworkPolicyManager;

    public BackgroundDataConditionController(Context appContext, ConditionManager manager) {
        mAppContext = appContext;
        mConditionManager = manager;
        mNetworkPolicyManager =
                (NetworkPolicyManager) appContext.getSystemService(Context.NETWORK_POLICY_SERVICE);
    }

    @Override
    public long getId() {
        return ID;
    }

    @Override
    public boolean isDisplayable() {
        return mNetworkPolicyManager.getRestrictBackground();
    }

    @Override
    public void onPrimaryClick(Context context) {
        context.startActivity(new Intent(context, Settings.DataUsageSummaryActivity.class));
    }

    @Override
    public void onActionClick() {
        mNetworkPolicyManager.setRestrictBackground(false);
        mConditionManager.onConditionChanged();
    }

    @Override
    public void startMonitoringStateChange() {

    }

    @Override
    public void stopMonitoringStateChange() {

    }
}
