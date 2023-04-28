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

package com.android.settings.development;

import android.app.tare.EconomyManager;
import android.content.Context;
import android.content.Intent;
import android.os.UserManager;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.development.tare.TareHomePage;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/** PreferenceController that serves as an entry point to the TARE configuration screen. */
public class TarePreferenceController extends
        DeveloperOptionsPreferenceController implements PreferenceControllerMixin {

    private static final String KEY_TARE = "tare";

    private final EconomyManager mEconomyManager;
    private final UserManager mUserManager;

    public TarePreferenceController(Context context) {
        super(context);
        mEconomyManager = context.getSystemService(EconomyManager.class);
        mUserManager = context.getSystemService(UserManager.class);
    }

    @Override
    public boolean isAvailable() {
        // Enable the UI if the dedicated flag enables it or if TARE itself is on.
        final boolean settingEnabled = Settings.Global.getInt(
                mContext.getContentResolver(), Settings.Global.SHOW_TARE_DEVELOPER_OPTIONS, 0) == 1;
        final boolean isTareUiEnabled = settingEnabled
                || mEconomyManager.getEnabledMode() != EconomyManager.ENABLED_MODE_OFF;
        return isTareUiEnabled
                && !mUserManager.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_TARE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!KEY_TARE.equals(preference.getKey())) {
            return false;
        }
        mContext.startActivity(new Intent(mContext, TareHomePage.class));
        return false;
    }
}
