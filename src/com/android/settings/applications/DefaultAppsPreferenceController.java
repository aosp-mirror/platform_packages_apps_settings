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

package com.android.settings.applications;

import android.app.role.RoleManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.icu.text.ListFormatter;
import android.text.TextUtils;

import androidx.core.text.BidiFormatter;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.applications.AppUtils;

import java.util.ArrayList;
import java.util.List;

public class DefaultAppsPreferenceController extends BasePreferenceController {

    private final PackageManager mPackageManager;
    private final RoleManager mRoleManager;

    public DefaultAppsPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);

        mPackageManager = context.getPackageManager();
        mRoleManager = context.getSystemService(RoleManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        final List<CharSequence> defaultAppLabels = new ArrayList<>();
        final CharSequence defaultBrowserLabel = getDefaultAppLabel(RoleManager.ROLE_BROWSER);
        if(!TextUtils.isEmpty(defaultBrowserLabel)) {
            defaultAppLabels.add(defaultBrowserLabel);
        }
        final CharSequence defaultPhoneLabel = getDefaultAppLabel(RoleManager.ROLE_DIALER);
        if(!TextUtils.isEmpty(defaultPhoneLabel)) {
            defaultAppLabels.add(defaultPhoneLabel);
        }
        final CharSequence defaultSmsLabel = getDefaultAppLabel(RoleManager.ROLE_SMS);
        if(!TextUtils.isEmpty(defaultSmsLabel)) {
            defaultAppLabels.add(defaultSmsLabel);
        }
        if (defaultAppLabels.isEmpty()) {
            return null;
        }
        return ListFormatter.getInstance().format(defaultAppLabels);
    }

    private CharSequence getDefaultAppLabel(String roleName) {
        final List<String> packageNames = mRoleManager.getRoleHolders(roleName);
        if (packageNames.isEmpty()) {
            return null;
        }
        final String packageName = packageNames.get(0);
        return BidiFormatter.getInstance().unicodeWrap(AppUtils.getApplicationLabel(mPackageManager,
                packageName));
    }
}
