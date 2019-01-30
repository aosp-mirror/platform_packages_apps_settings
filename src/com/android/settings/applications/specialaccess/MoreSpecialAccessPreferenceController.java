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
 * limitations under the License.
 */

package com.android.settings.applications.specialaccess;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

public class MoreSpecialAccessPreferenceController extends BasePreferenceController {

    private final Intent mIntent;

    public MoreSpecialAccessPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);

        final PackageManager packageManager = context.getPackageManager();
        final String packageName = packageManager.getPermissionControllerPackageName();
        if (packageName != null) {
            Intent intent = new Intent(Intent.ACTION_MANAGE_SPECIAL_APP_ACCESSES)
                    .setPackage(packageName);
            ResolveInfo resolveInfo = packageManager.resolveActivity(intent,
                    PackageManager.MATCH_DEFAULT_ONLY);
            mIntent = resolveInfo != null ? intent : null;
        } else {
            mIntent = null;
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return mIntent != null ? AVAILABLE_UNSEARCHABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), mPreferenceKey)) {
            if (mIntent != null) {
                mContext.startActivity(mIntent);
            }
            return true;
        }
        return false;
    }
}
