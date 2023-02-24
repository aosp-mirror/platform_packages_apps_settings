/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.privacy;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that shows several privacy toggle controls
 */
public class PrivacyControlsFragment extends DashboardFragment {
    private static final String TAG = "PrivacyDashboardFrag";
    private static final String CAMERA_KEY = "privacy_camera_toggle";
    private static final String MIC_KEY = "privacy_mic_toggle";

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new CameraToggleController(context, CAMERA_KEY));
        controllers.add(new MicToggleController(context, MIC_KEY));
        controllers.add(new ShowClipAccessNotificationPreferenceController(context));
        return controllers;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.TOP_LEVEL_PRIVACY;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.privacy_controls_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }
}
