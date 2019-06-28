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
 * limitations under the License
 */

package com.android.settings.deviceinfo.legal;

import android.content.Context;
import android.content.pm.ModuleInfo;
import android.content.pm.PackageManager;

import com.android.settings.core.BasePreferenceController;

import java.util.List;

public class ModuleLicensesListPreferenceController extends BasePreferenceController {
    public ModuleLicensesListPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        PackageManager packageManager = mContext.getPackageManager();
        List<ModuleInfo> modules = packageManager.getInstalledModules(0 /* flags */);
        return modules.stream().anyMatch(new ModuleLicensesPreferenceController.Predicate(mContext))
                ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }
}
