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

import android.content.Context;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.security.MemtagHelper;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class DevelopmentMemtagPagePreferenceController extends DeveloperOptionsPreferenceController
        implements PreferenceControllerMixin {
    private static final String KEY_DEVELOPMENT_MEMTAG_PAGE = "development_memtag_page";

    public DevelopmentMemtagPagePreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return android.os.SystemProperties.getBoolean("ro.arm64.memtag.bootctl_supported", false);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_DEVELOPMENT_MEMTAG_PAGE;
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        MemtagHelper.setChecked(false);
    }
}
