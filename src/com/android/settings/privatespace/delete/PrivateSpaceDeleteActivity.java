/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.privatespace.delete;

import android.app.settings.SettingsEnums;
import android.os.Bundle;

import androidx.navigation.fragment.NavHostFragment;

import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.settings.core.InstrumentedActivity;

import com.google.android.setupdesign.util.ThemeHelper;

public class PrivateSpaceDeleteActivity extends InstrumentedActivity {
    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PRIVATE_SPACE_SETTINGS;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!android.os.Flags.allowPrivateProfile()) {
            return;
        }
        setTheme(SetupWizardUtils.getTheme(this, getIntent()));
        ThemeHelper.trySetDynamicColor(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.privatespace_setup_root);
        NavHostFragment navHostFragment =
                (NavHostFragment)
                        getSupportFragmentManager().findFragmentById(R.id.ps_nav_host_fragment);
        navHostFragment.getNavController().setGraph(R.navigation.private_space_delete_nav);
    }
}
