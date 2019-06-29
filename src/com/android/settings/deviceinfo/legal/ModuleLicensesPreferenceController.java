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

import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.internal.util.ArrayUtils;
import com.android.settings.core.BasePreferenceController;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

public class ModuleLicensesPreferenceController extends BasePreferenceController {
    public ModuleLicensesPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        PackageManager packageManager = mContext.getPackageManager();
        List<ModuleInfo> modules = packageManager.getInstalledModules(0 /* flags */);
        PreferenceGroup group = screen.findPreference(getPreferenceKey());
        modules.stream()
                .sorted(Comparator.comparing(o -> o.getName().toString()))
                .filter(new Predicate(mContext))
                .forEach(module ->
                        group.addPreference(
                                new ModuleLicensePreference(group.getContext(), module)));
    }

    static class Predicate implements java.util.function.Predicate<ModuleInfo> {
        private final Context mContext;

        public Predicate(Context context) {
            mContext = context;
        }
        @Override
        public boolean test(ModuleInfo module) {
            try {
                return ArrayUtils.contains(
                        ModuleLicenseProvider.getPackageAssetManager(
                                mContext.getPackageManager(),
                                module.getPackageName())
                                        .list(""),
                        ModuleLicenseProvider.GZIPPED_LICENSE_FILE_NAME);
            } catch (IOException | PackageManager.NameNotFoundException e) {
                return false;
            }
        }
    }
}
