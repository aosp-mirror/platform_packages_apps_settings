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
package com.android.settings.accounts;

import android.content.Context;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.core.PreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.drawer.CategoryKey;

import java.util.ArrayList;
import java.util.List;

public class UserAndAccountDashboardFragment extends DashboardFragment {

    private static final String TAG = "UserAndAccountDashboard";

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.ACCOUNT;
    }

    @Override
    protected String getCategoryKey() {
        return CategoryKey.CATEGORY_ACCOUNT;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.user_and_accounts_settings;
    }

    @Override
    protected List<PreferenceController> getPreferenceControllers(Context context) {
        final List<PreferenceController> controllers = new ArrayList<>();
        controllers.add(new EmergencyInfoPreferenceController(context));
        AddUserWhenLockedPreferenceController addUserWhenLockedPrefController =
                new AddUserWhenLockedPreferenceController(context);
        controllers.add(addUserWhenLockedPrefController);
        getLifecycle().addObserver(addUserWhenLockedPrefController);
        controllers.add(new AutoSyncDataPreferenceController(context, this));
        controllers.add(new AutoSyncPersonalDataPreferenceController(context, this));
        controllers.add(new AutoSyncWorkDataPreferenceController(context, this));
        return controllers;
    }

}