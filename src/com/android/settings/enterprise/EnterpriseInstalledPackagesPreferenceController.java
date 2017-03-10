/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.core.DynamicAvailabilityPreferenceController;
import com.android.settings.core.lifecycle.Lifecycle;
import com.android.settings.overlay.FeatureFactory;

public class EnterpriseInstalledPackagesPreferenceController
        extends DynamicAvailabilityPreferenceController {

    private static final String KEY_NUMBER_ENTERPRISE_INSTALLED_PACKAGES
            = "number_enterprise_installed_packages";
    private final ApplicationFeatureProvider mFeatureProvider;
    private final boolean mAsync;

    public EnterpriseInstalledPackagesPreferenceController(Context context, Lifecycle lifecycle,
            boolean async) {
        super(context, lifecycle);
        mFeatureProvider = FeatureFactory.getFactory(context)
                .getApplicationFeatureProvider(context);
        mAsync = async;
    }

    @Override
    public void updateState(Preference preference) {
        mFeatureProvider.calculateNumberOfInstalledApps(
                PackageManager.INSTALL_REASON_POLICY, true /* async */,
                (num) -> {
                    if (num == 0) {
                        preference.setVisible(false);
                    } else {
                        preference.setVisible(true);
                        preference.setSummary(mContext.getResources().getQuantityString(
                                R.plurals.enterprise_privacy_number_packages, num, num));
                    }
                });
    }

    @Override
    public boolean isAvailable() {
        if (mAsync) {
            // When called on the main UI thread, we must not block. Since calculating the number of
            // enterprise-installed apps takes a bit of time, we always return true here and
            // determine the pref's actual visibility asynchronously in updateState().
            return true;
        }

        // When called by the search indexer, we are on a background thread that we can block. Also,
        // changes to the pref's visibility made in updateState() would not be seen by the indexer.
        // We block and return synchronously whether there are enterprise-installed apps or not.
        final Boolean[] haveEnterpriseInstalledPackages = { null };
        mFeatureProvider.calculateNumberOfInstalledApps(PackageManager.INSTALL_REASON_POLICY,
                false /* async */, (num) -> haveEnterpriseInstalledPackages[0] = num > 0);
        return haveEnterpriseInstalledPackages[0];
    }

    @Override
    public String getPreferenceKey() {
        return KEY_NUMBER_ENTERPRISE_INSTALLED_PACKAGES;
    }
}
