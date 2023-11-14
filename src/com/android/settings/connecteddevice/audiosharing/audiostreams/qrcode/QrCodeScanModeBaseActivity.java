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

package com.android.settings.connecteddevice.audiosharing.audiostreams.qrcode;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;

import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.ObservableActivity;

import com.google.android.setupdesign.util.ThemeHelper;
import com.google.android.setupdesign.util.ThemeResolver;

public abstract class QrCodeScanModeBaseActivity extends ObservableActivity {

    private static final String THEME_KEY = "setupwizard.theme";
    private static final String THEME_DEFAULT_VALUE = "SudThemeGlifV3_DayNight";
    protected FragmentManager mFragmentManager;

    protected abstract void handleIntent(Intent intent);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int defaultTheme =
                ThemeHelper.isSetupWizardDayNightEnabled(this)
                        ? com.google.android.setupdesign.R.style.SudThemeGlifV3_DayNight
                        : com.google.android.setupdesign.R.style.SudThemeGlifV3_Light;
        ThemeResolver themeResolver =
                new ThemeResolver.Builder(ThemeResolver.getDefault())
                        .setDefaultTheme(defaultTheme)
                        .setUseDayNight(true)
                        .build();
        setTheme(
                themeResolver.resolve(
                        SystemProperties.get(THEME_KEY, THEME_DEFAULT_VALUE),
                        /* suppressDayNight= */ !ThemeHelper.isSetupWizardDayNightEnabled(this)));

        setContentView(R.layout.qrcode_scan_mode_activity);
        mFragmentManager = getSupportFragmentManager();

        if (savedInstanceState == null) {
            handleIntent(getIntent());
        }
    }
}
