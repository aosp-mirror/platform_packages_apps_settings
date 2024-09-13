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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.notification.modes.ZenIconLoader;
import com.android.settingslib.notification.modes.ZenMode;

class ZenModeHeaderController extends AbstractZenModeHeaderController {

    ZenModeHeaderController(@NonNull Context context, @NonNull ZenIconLoader iconLoader,
            @NonNull String key, @NonNull DashboardFragment fragment) {
        super(context, iconLoader, key, fragment);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        setUpHeader(screen,
                mContext.getResources().getDimensionPixelSize(R.dimen.zen_mode_header_size));
    }

    @Override
    public void updateState(Preference preference, @NonNull ZenMode zenMode) {
        updateIcon(preference, zenMode,
                icon -> IconUtil.makeModeHeader(mContext, icon),
                /* isSelected= */ zenMode.isActive());
    }
}
