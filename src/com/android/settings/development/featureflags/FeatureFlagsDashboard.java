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

package com.android.settings.development.featureflags;

import android.content.Context;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

public class FeatureFlagsDashboard extends DashboardFragment {

    private static final String TAG = "FeatureFlagsDashboard";

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SETTINGS_FEATURE_FLAGS_DASHBOARD;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.feature_flags_settings;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(FeatureFlagFooterPreferenceController.class).setFooterMixin(mFooterPreferenceMixin);
    }

    @Override
    public int getHelpResource() {
        return 0;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final Lifecycle lifecycle = getLifecycle();
        final FeatureFlagFooterPreferenceController footerController =
                new FeatureFlagFooterPreferenceController(context);
        controllers.add(new FeatureFlagsPreferenceController(context, lifecycle));
        controllers.add(footerController);
        lifecycle.addObserver(footerController);
        return controllers;
    }
}
