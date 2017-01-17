/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.security;

import android.content.Context;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.trustagent.TrustAgentManager;
import com.android.settingslib.drawer.DashboardCategory;


/** FeatureProvider for security. */
public interface SecurityFeatureProvider {

    /** Update preferences with data from associated tiles. */
    void updatePreferences(Context context, PreferenceScreen preferenceScreen,
            DashboardCategory dashboardCategory);

    /** Returns the {@link TrustAgentManager} bound to this {@link SecurityFeatureProvider}. */
    TrustAgentManager getTrustAgentManager();
}
