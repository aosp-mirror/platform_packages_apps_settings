/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.system;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.applications.manageapplications.ResetAppPrefPreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.network.EraseEuiccDataController;
import com.android.settings.network.NetworkResetPreferenceController;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.privatespace.delete.ResetOptionsDeletePrivateSpaceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/** Settings fragment containing reset options. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ResetDashboardFragment extends DashboardFragment {

    private static final String TAG = "ResetDashboardFragment";
    public static final int PRIVATE_SPACE_DELETE_CREDENTIAL_REQUEST = 1;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.RESET_DASHBOARD;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.reset_dashboard_fragment;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (SubscriptionUtil.isSimHardwareVisible(context)) {
            use(EraseEuiccDataController.class).setFragment(this);
        }
        if (android.multiuser.Flags.enablePrivateSpaceFeatures()
                && android.multiuser.Flags.deletePrivateSpaceFromReset()) {
            ResetOptionsDeletePrivateSpaceController resetOptionsDeletePrivateSpaceController =
                    use(ResetOptionsDeletePrivateSpaceController.class);
            if (resetOptionsDeletePrivateSpaceController != null) {
                resetOptionsDeletePrivateSpaceController.setFragment(this);
            }
        }
        FactoryResetPreferenceController factoryResetPreferenceController =
                use(FactoryResetPreferenceController.class);
        if (factoryResetPreferenceController != null) {
            factoryResetPreferenceController.setFragment(this);
        }
    }

    @Override
    protected boolean shouldSkipForInitialSUW() {
        return true;
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        if (SubscriptionUtil.isSimHardwareVisible(context)) {
            controllers.add(new NetworkResetPreferenceController(context));
        }
        controllers.add(new ResetAppPrefPreferenceController(context, lifecycle));
        return controllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.reset_dashboard_fragment) {

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null /* lifecycle */);
                }
            };

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (use(ResetOptionsDeletePrivateSpaceController.class)
                .handleActivityResult(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
