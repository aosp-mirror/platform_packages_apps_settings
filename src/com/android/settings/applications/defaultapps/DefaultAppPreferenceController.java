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

package com.android.settings.applications.defaultapps;

import static com.android.settingslib.TwoTargetPreference.ICON_SIZE_MEDIUM;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.GearPreference;
import com.android.settingslib.TwoTargetPreference;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.core.AbstractPreferenceController;

public abstract class DefaultAppPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String TAG = "DefaultAppPrefControl";

    protected final PackageManager mPackageManager;
    protected final UserManager mUserManager;

    protected int mUserId;

    public DefaultAppPreferenceController(Context context) {
        super(context);
        mPackageManager = context.getPackageManager();
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mUserId = UserHandle.myUserId();
    }

    @Override
    public void updateState(Preference preference) {
        final DefaultAppInfo app = getDefaultAppInfo();
        CharSequence defaultAppLabel = getDefaultAppLabel();
        if (preference instanceof TwoTargetPreference) {
            // For use small icon because we are displaying an app preference.
            // We only need to do this for TwoTargetPreference because the other prefs are
            // already using AppPreference so their icon is already normalized.
            ((TwoTargetPreference) preference).setIconSize(ICON_SIZE_MEDIUM);
        }
        if (!TextUtils.isEmpty(defaultAppLabel)) {
            preference.setSummary(defaultAppLabel);
            Utils.setSafeIcon(preference, getDefaultAppIcon());
        } else {
            Log.d(TAG, "No default app");
            preference.setSummary(R.string.app_list_preference_none);
            preference.setIcon(null);
        }
        mayUpdateGearIcon(app, preference);
    }

    private void mayUpdateGearIcon(DefaultAppInfo app, Preference preference) {
        if (!(preference instanceof GearPreference)) {
            return;
        }
        final Intent settingIntent = getSettingIntent(app);
        if (settingIntent != null) {
            ((GearPreference) preference).setOnGearClickListener(
                    p -> startActivity(settingIntent));
        } else {
            ((GearPreference) preference).setOnGearClickListener(null);
        }
    }

    protected void startActivity(Intent intent) {
        mContext.startActivity(intent);
    }

    protected abstract DefaultAppInfo getDefaultAppInfo();

    /**
     * Returns an optional intent that will be launched when clicking "gear" icon.
     */
    protected Intent getSettingIntent(DefaultAppInfo info) {
        //By default return null. It's up to subclasses to provide logic.
        return null;
    }

    public Drawable getDefaultAppIcon() {
        if (!isAvailable()) {
            return null;
        }
        final DefaultAppInfo app = getDefaultAppInfo();
        if (app != null) {
            return app.loadIcon();
        }
        return null;
    }

    public CharSequence getDefaultAppLabel() {
        if (!isAvailable()) {
            return null;
        }
        final DefaultAppInfo app = getDefaultAppInfo();
        if (app != null) {
            return app.loadLabel();
        }
        return null;
    }
}
