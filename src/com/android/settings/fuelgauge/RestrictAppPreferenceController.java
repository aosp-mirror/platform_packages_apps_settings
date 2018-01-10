/*
 * Copyright (C) 2018 The Android Open Source Project
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
 *
 *
 */

package com.android.settings.fuelgauge;

import android.app.AppOpsManager;
import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.core.BasePreferenceController;

import java.util.List;

/**
 * Controller to change and update the smart battery toggle
 */
public class RestrictAppPreferenceController extends BasePreferenceController {
    @VisibleForTesting
    static final String KEY_RESTRICT_APP = "restricted_app";

    private AppOpsManager mAppOpsManager;
    private List<AppOpsManager.PackageOps> mPackageOps;

    public RestrictAppPreferenceController(Context context) {
        super(context, KEY_RESTRICT_APP);
        mAppOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        mPackageOps = mAppOpsManager.getPackagesForOps(
                new int[]{AppOpsManager.OP_RUN_ANY_IN_BACKGROUND});
        final int num = mPackageOps != null ? mPackageOps.size() : 0;

        preference.setSummary(
                mContext.getResources().getQuantityString(R.plurals.restricted_app_summary, num,
                        num));
    }

}
