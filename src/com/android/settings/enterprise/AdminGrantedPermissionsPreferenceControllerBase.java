
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

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.utils.StringUtil;

public abstract class AdminGrantedPermissionsPreferenceControllerBase
        extends AbstractPreferenceController implements PreferenceControllerMixin {

    private final String[] mPermissions;
    private final ApplicationFeatureProvider mFeatureProvider;
    private final boolean mAsync;
    private boolean mHasApps;

    public AdminGrantedPermissionsPreferenceControllerBase(Context context, boolean async,
            String[] permissions) {
        super(context);
        mPermissions = permissions;
        mFeatureProvider = FeatureFactory.getFeatureFactory()
                .getApplicationFeatureProvider();
        mAsync = async;
        mHasApps = false;
    }

    @Override
    public void updateState(Preference preference) {
        mFeatureProvider.calculateNumberOfAppsWithAdminGrantedPermissions(mPermissions,
                true /* async */,
                (num) -> {
                    if (num == 0) {
                        mHasApps = false;
                    } else {
                        preference.setSummary(StringUtil.getIcuPluralsString(mContext, num,
                                R.string.enterprise_privacy_number_packages_lower_bound));
                        mHasApps = true;
                    }
                    preference.setVisible(mHasApps);
                });
    }

    @Override
    public boolean isAvailable() {
        if (mAsync) {
            // When called on the main UI thread, we must not block. Since calculating the number of
            // apps that the admin has granted a given permissions takes a bit of time, we always
            // return true here and determine the pref's actual visibility asynchronously in
            // updateState().
            return true;
        }

        // When called by the search indexer, we are on a background thread that we can block. Also,
        // changes to the pref's visibility made in updateState() would not be seen by the indexer.
        // We block and return synchronously whether the admin has granted the given permissions to
        // any apps or not.
        final Boolean[] haveAppsWithAdminGrantedPermissions = { null };
        mFeatureProvider.calculateNumberOfAppsWithAdminGrantedPermissions(mPermissions,
                false /* async */, (num) -> haveAppsWithAdminGrantedPermissions[0] = num > 0);
        mHasApps = haveAppsWithAdminGrantedPermissions[0];
        return mHasApps;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!getPreferenceKey().equals(preference.getKey())) {
            return false;
        }
        if (!mHasApps) {
            return false;
        }
        return super.handlePreferenceTreeClick(preference);
    }
}
