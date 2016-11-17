/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.enterprise;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.core.PreferenceController;
import com.android.settings.overlay.FeatureFactory;

public class InstalledPackagesPreferenceController extends PreferenceController {

    private static final String KEY_NUMBER_INSTALLED_PACKAGES = "number_installed_packages";
    private final ApplicationFeatureProvider mFeatureProvider;

    public InstalledPackagesPreferenceController(Context context) {
        super(context);
        mFeatureProvider = FeatureFactory.getFactory(context)
                .getApplicationFeatureProvider(context);
    }

    @Override
    public void updateState(Preference preference) {
        mFeatureProvider.calculateNumberOfInstalledApps(
                new ApplicationFeatureProvider.NumberOfInstalledAppsCallback() {
                    @Override
                    public void onNumberOfInstalledAppsResult(int num) {
                        preference.setTitle(mContext.getResources().getQuantityString(
                                R.plurals.number_installed_packages, num, num));
                    }
                });
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_NUMBER_INSTALLED_PACKAGES;
    }
}
