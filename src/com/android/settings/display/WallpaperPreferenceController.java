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

import static android.os.UserManager.DISALLOW_SET_WALLPAPER;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;

import java.util.List;

public class WallpaperPreferenceController extends BasePreferenceController {
    private static final String TAG = "WallpaperPrefController";

    private final String mWallpaperPackage;
    private final String mWallpaperClass;
    private final String mStylesAndWallpaperClass;

    public WallpaperPreferenceController(Context context, String key) {
        super(context, key);
        mWallpaperPackage = mContext.getString(R.string.config_wallpaper_picker_package);
        mWallpaperClass = mContext.getString(R.string.config_wallpaper_picker_class);
        mStylesAndWallpaperClass =
                mContext.getString(R.string.config_styles_and_wallpaper_picker_class);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference preference = screen.findPreference(getPreferenceKey());
        preference.setTitle(getTitle());
    }

    public String getTitle() {
        return mContext.getString(areStylesAvailable()
                ? R.string.style_and_wallpaper_settings_title : R.string.wallpaper_settings_title);
    }

    public ComponentName getComponentName() {
        return new ComponentName(mWallpaperPackage,
                areStylesAvailable() ? mStylesAndWallpaperClass : mWallpaperClass);
    }

    public String getKeywords() {
        StringBuilder sb = new StringBuilder(mContext.getString(R.string.keywords_wallpaper));
        if (areStylesAvailable()) {
            sb.append(", ").append(mContext.getString(R.string.keywords_styles));
        }
        return sb.toString();
    }

    @Override
    public int getAvailabilityStatus() {
        if (TextUtils.isEmpty(mWallpaperPackage) || TextUtils.isEmpty(mWallpaperClass)) {
            Log.e(TAG, "No Wallpaper picker specified!");
            return UNSUPPORTED_ON_DEVICE;
        }
        return canResolveWallpaperComponent(mWallpaperClass)
                ? AVAILABLE_UNSEARCHABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        disablePreferenceIfManaged((RestrictedPreference) preference);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getPreferenceKey().equals(preference.getKey())) {
            final Intent intent = new Intent().setComponent(getComponentName());
            if (areStylesAvailable()) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            preference.getContext().startActivity(intent);
            return true;
        }
        return super.handlePreferenceTreeClick(preference);
    }

    /** Returns whether Styles & Wallpaper is enabled and available. */
    public boolean areStylesAvailable() {
        return !TextUtils.isEmpty(mStylesAndWallpaperClass)
                && canResolveWallpaperComponent(mStylesAndWallpaperClass);
    }

    private boolean canResolveWallpaperComponent(String className) {
        final ComponentName componentName = new ComponentName(mWallpaperPackage, className);
        final PackageManager pm = mContext.getPackageManager();
        final Intent intent = new Intent().setComponent(componentName);
        final List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0 /* flags */);
        return resolveInfos != null && !resolveInfos.isEmpty();
    }

    private void disablePreferenceIfManaged(RestrictedPreference pref) {
        final String restriction = DISALLOW_SET_WALLPAPER;
        if (pref != null) {
            pref.setDisabledByAdmin(null);
            if (RestrictedLockUtilsInternal.hasBaseUserRestriction(mContext,
                    restriction, UserHandle.myUserId())) {
                pref.setEnabled(false);
            } else {
                pref.checkRestrictionAndSetDisabled(restriction);
            }
        }
    }
}
