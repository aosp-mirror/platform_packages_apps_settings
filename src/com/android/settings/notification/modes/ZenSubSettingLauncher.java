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

package com.android.settings.notification.modes;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;

import com.android.settings.core.SubSettingLauncher;

class ZenSubSettingLauncher {

    static SubSettingLauncher forMode(Context context, String modeId) {
        return forModeFragment(context, ZenModeFragment.class, modeId,
                SettingsEnums.NOTIFICATION_ZEN_MODE_AUTOMATION);
    }

    private static SubSettingLauncher forModeFragment(Context context,
            Class<? extends ZenModeFragmentBase> fragmentClass, String modeId,
            int sourceMetricsCategory) {
        Bundle bundle = new Bundle();
        bundle.putString(ZenModeFragmentBase.MODE_ID, modeId);

        return new SubSettingLauncher(context)
                .setDestination(fragmentClass.getName())
                .setArguments(bundle)
                .setSourceMetricsCategory(sourceMetricsCategory);
    }
}
