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

package com.android.settings.privatespace.onelock;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

public class UseOneLockSettingsFragment extends DashboardFragment {
    private static final String TAG = "UseOneLockSettings";
    public static final int UNIFY_PRIVATE_LOCK_WITH_DEVICE_REQUEST = 1;
    public static final int UNUNIFY_PRIVATE_LOCK_FROM_DEVICE_REQUEST = 2;

    @Override
    public void onCreate(Bundle icicle) {
        if (android.os.Flags.allowPrivateProfile()) {
            super.onCreate(icicle);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PRIVATE_SPACE_SETTINGS;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.privatespace_one_lock;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new UseOneLockControllerSwitch(context, this));
        controllers.add(new PrivateSpaceLockController(context, this));
        controllers.add(new FaceFingerprintUnlockController(context, this));
        return controllers;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (use(UseOneLockControllerSwitch.class)
                  .handleActivityResult(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
