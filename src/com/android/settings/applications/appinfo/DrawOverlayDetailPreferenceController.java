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

import static android.Manifest.permission.SYSTEM_ALERT_WINDOW;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.UserManager;

import com.android.settings.SettingsPreferenceFragment;

public class DrawOverlayDetailPreferenceController extends AppInfoPreferenceControllerBase {

    public DrawOverlayDetailPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        if (UserManager.get(mContext).isManagedProfile()) {
            return DISABLED_FOR_USER;
        }
        final PackageInfo packageInfo = mParent.getPackageInfo();
        if (packageInfo == null || packageInfo.requestedPermissions == null) {
            return DISABLED_FOR_USER;
        }
        for (int i = 0; i < packageInfo.requestedPermissions.length; i++) {
            if (packageInfo.requestedPermissions[i].equals(SYSTEM_ALERT_WINDOW)) {
                return AVAILABLE;
            }
        }
        return DISABLED_FOR_USER;
    }

    @Override
    protected Class<? extends SettingsPreferenceFragment> getDetailFragmentClass() {
        return DrawOverlayDetails.class;
    }

    @Override
    public CharSequence getSummary() {
        return DrawOverlayDetails.getSummary(mContext, mParent.getAppEntry());
    }
}
