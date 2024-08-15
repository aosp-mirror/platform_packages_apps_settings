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

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

/**
 * DND Calls Settings page to determine which priority senders can bypass DND when this mode is
 * activated.
 */
public class ZenModeCallsFragment extends ZenModeFragmentBase {

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new ZenModePrioritySendersPreferenceController(context,
                "zen_mode_settings_category_calls", false, mBackend, mHelperBackend));
        controllers.add(new ZenModeRepeatCallersPreferenceController(context,
                "zen_mode_repeat_callers", mBackend,
                context.getResources().getInteger(com.android.internal.R.integer
                        .config_zen_repeat_callers_threshold)));
        return controllers;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.modes_calls_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DND_CALLS;
    }

    @Override
    public void onResume() {
        super.onResume();
        use(ZenModePrioritySendersPreferenceController.class).onResume();
    }
}
