/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

class SelectDSUPreferenceController extends DeveloperOptionsPreferenceController {

    private static final String DSU_LOADER_KEY = "dsu_loader";

    SelectDSUPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return DSU_LOADER_KEY;
    }

    private boolean isDSURunning() {
        return SystemProperties.getBoolean("ro.gsid.image_running", false);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (DSU_LOADER_KEY.equals(preference.getKey())) {
            if (isDSURunning()) {
                return true;
            }
            final Intent intent = new Intent(mContext, DSULoader.class);
            mContext.startActivity(intent);
            return true;
        }
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        int key = isDSURunning() ? R.string.dsu_is_running : R.string.dsu_loader_description;
        preference.setSummary(mContext.getResources().getString(key));
    }
}
