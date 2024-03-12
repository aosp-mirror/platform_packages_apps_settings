/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.privatespace;

import android.app.settings.SettingsEnums;
import android.os.Bundle;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

/** Fragment representing the Private Space dashboard in Settings. */
public class PrivateSpaceDashboardFragment extends DashboardFragment {
    private static final String TAG = "PrivateSpaceDashboardFragment";

    @Override
    public void onCreate(Bundle icicle) {
        if (android.os.Flags.allowPrivateProfile()) {
            super.onCreate(icicle);
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.private_space_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PRIVATE_SPACE_SETTINGS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }
}
