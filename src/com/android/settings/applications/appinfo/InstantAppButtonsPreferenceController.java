/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.appinfo;

import android.app.AlertDialog;
import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.applications.instantapps.InstantAppButtonsController;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.applications.AppUtils;

public class InstantAppButtonsPreferenceController extends BasePreferenceController {

    private static final String KEY_INSTANT_APP_BUTTONS = "instant_app_buttons";

    private final AppInfoDashboardFragment mParent;
    private final String mPackageName;
    private InstantAppButtonsController mInstantAppButtonsController;

    public InstantAppButtonsPreferenceController(Context context, AppInfoDashboardFragment parent,
            String packageName) {
        super(context, KEY_INSTANT_APP_BUTTONS);
        mParent = parent;
        mPackageName = packageName;
    }

    @Override
    public int getAvailabilityStatus() {
        return AppUtils.isInstant(mParent.getPackageInfo().applicationInfo)
                ? AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        LayoutPreference buttons =
                (LayoutPreference) screen.findPreference(KEY_INSTANT_APP_BUTTONS);
        mInstantAppButtonsController = getApplicationFeatureProvider()
                .newInstantAppButtonsController(mParent,
                        buttons.findViewById(R.id.instant_app_button_container),
                        id -> mParent.showDialogInner(id, 0))
                .setPackageName(mPackageName)
                .show();
    }

    public AlertDialog createDialog(int id) {
        return mInstantAppButtonsController.createDialog(id);
    }

    @VisibleForTesting
    ApplicationFeatureProvider getApplicationFeatureProvider() {
        return FeatureFactory.getFactory(mContext).getApplicationFeatureProvider(mContext);
    }
}
