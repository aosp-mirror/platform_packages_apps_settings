/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.applications;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.util.Log;

import com.android.settings.AppHeader;
import com.android.settings.overlay.FeatureFactory;

import static com.android.settings.applications.AppHeaderController.ActionType;

public abstract class AppInfoWithHeader extends AppInfoBase {

    private boolean mCreated;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mCreated) {
            Log.w(TAG, "onActivityCreated: ignoring duplicate call");
            return;
        }
        mCreated = true;
        if (mPackageInfo == null) return;
        final Activity activity = getActivity();
        if (!FeatureFactory.getFactory(activity)
                .getDashboardFeatureProvider(activity).isEnabled()) {
            AppHeader.createAppHeader(this, mPackageInfo.applicationInfo.loadIcon(mPm),
                    mPackageInfo.applicationInfo.loadLabel(mPm), mPackageName,
                    mPackageInfo.applicationInfo.uid, 0);
        } else {
            final Preference pref = FeatureFactory.getFactory(activity)
                    .getApplicationFeatureProvider(activity)
                    .newAppHeaderController(this, null /* appHeader */)
                    .setIcon(mPackageInfo.applicationInfo.loadIcon(mPm))
                    .setLabel(mPackageInfo.applicationInfo.loadLabel(mPm))
                    .setSummary(mPackageInfo)
                    .setPackageName(mPackageName)
                    .setUid(mPackageInfo.applicationInfo.uid)
                    .setButtonActions(ActionType.ACTION_APP_INFO, ActionType.ACTION_NONE)
                    .done(getPrefContext());
            getPreferenceScreen().addPreference(pref);
        }
    }
}
