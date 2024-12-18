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
 * limitations under the License
 */

package com.android.settings.language;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import com.android.settings.Settings;
import com.android.settings.core.BasePreferenceController;

/**
 * This is a display controller for new language activity entry.
 * TODO(b/273642892): When new layout is on board, this class shall be removed.
 */
public class LanguagePreferenceController extends BasePreferenceController {
    public LanguagePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        setActivityEnabled(mContext, Settings.LanguageSettingsActivity.class, true);
        return AVAILABLE;
    }

    private static void setActivityEnabled(Context context, Class klass, final boolean isEnabled) {
        PackageManager packageManager = context.getPackageManager();

        ComponentName componentName =
                new ComponentName(context, klass);
        final int flag = isEnabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

        packageManager.setComponentEnabledSetting(
                componentName, flag, PackageManager.DONT_KILL_APP);
    }
}