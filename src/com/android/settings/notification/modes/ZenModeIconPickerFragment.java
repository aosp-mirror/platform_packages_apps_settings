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

import com.google.common.collect.ImmutableList;

import java.util.List;

public class ZenModeIconPickerFragment extends ZenModeFragmentBase {
    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.modes_icon_picker;
    }

    @Override
    public int getMetricsCategory() {
        // TODO: b/332937635 - make this the correct metrics category
        return SettingsEnums.NOTIFICATION_ZEN_MODE_AUTOMATION;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return ImmutableList.of(
                new ZenModeIconPickerIconPreferenceController(context, "current_icon", this,
                        mBackend),
                new ZenModeIconPickerListPreferenceController(context, "icon_list", this,
                        // TODO: b/333901673 - Replace with correct icon list.
                        new TempIconOptionsProvider(), mBackend));
    }
}
