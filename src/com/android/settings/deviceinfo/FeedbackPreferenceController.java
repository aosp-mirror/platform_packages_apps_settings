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
package com.android.settings.deviceinfo;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.DeviceInfoUtils;
import com.android.settingslib.core.AbstractPreferenceController;

public class FeedbackPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {
    private static final String KEY_DEVICE_FEEDBACK = "device_feedback";

    private final Fragment mHost;
    private final Intent intent;

    public FeedbackPreferenceController(Fragment host, Context context) {
        super(context);
        this.mHost = host;
        intent = new Intent("android.intent.action.BUG_REPORT");
    }

    @Override
    public boolean isAvailable() {
        return !TextUtils.isEmpty(DeviceInfoUtils.getFeedbackReporterPackage(mContext));
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        intent.setPackage(DeviceInfoUtils.getFeedbackReporterPackage(mContext));
        preference.setIntent(intent);

        // In some cases, cannot retrieve the report package from package manager,
        // For example, launched from lock screen.
        // Update this preference visibility after updateState.
        if (isAvailable() && !preference.isVisible()) {
            preference.setVisible(true);
        } else if (!isAvailable() && preference.isVisible()){
            preference.setVisible(false);
        }
    }

    public String getPreferenceKey() {
        return KEY_DEVICE_FEEDBACK;
    }

    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), KEY_DEVICE_FEEDBACK)) {
            return false;
        }
        if (!this.isAvailable()) {
            return false;
        }

        this.mHost.startActivityForResult(intent, 0);
        return true;
    }
}

