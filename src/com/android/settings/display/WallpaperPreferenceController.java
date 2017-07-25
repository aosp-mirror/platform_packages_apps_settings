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
package com.android.settings.display;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.AbstractPreferenceController;

import static android.os.UserManager.DISALLOW_SET_WALLPAPER;

import java.util.List;

public class WallpaperPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {

    private static final String TAG = "WallpaperPrefController";

    public static final String KEY_WALLPAPER = "wallpaper";

    private final String mWallpaperPackage;
    private final String mWallpaperClass;

    public WallpaperPreferenceController(Context context) {
        super(context);
        mWallpaperPackage = mContext.getString(R.string.config_wallpaper_picker_package);
        mWallpaperClass = mContext.getString(R.string.config_wallpaper_picker_class);
    }

    @Override
    public boolean isAvailable() {
        if (TextUtils.isEmpty(mWallpaperPackage) || TextUtils.isEmpty(mWallpaperClass)) {
            Log.e(TAG, "No Wallpaper picker specified!");
            return false;
        }
        final ComponentName componentName =
                new ComponentName(mWallpaperPackage, mWallpaperClass);
        final PackageManager pm = mContext.getPackageManager();
        final Intent intent = new Intent();
        intent.setComponent(componentName);
        final List<ResolveInfo> resolveInfos =
                pm.queryIntentActivities(intent, 0 /* flags */);
        return resolveInfos != null && resolveInfos.size() != 0;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_WALLPAPER;
    }

    @Override
    public void updateState(Preference preference) {
        disablePreferenceIfManaged((RestrictedPreference) preference);
    }

    private void disablePreferenceIfManaged(RestrictedPreference pref) {
        final String restriction = DISALLOW_SET_WALLPAPER;
        if (pref != null) {
            pref.setDisabledByAdmin(null);
            if (RestrictedLockUtils.hasBaseUserRestriction(mContext,
                    restriction, UserHandle.myUserId())) {
                pref.setEnabled(false);
            } else {
                pref.checkRestrictionAndSetDisabled(restriction);
            }
        }
    }
}
