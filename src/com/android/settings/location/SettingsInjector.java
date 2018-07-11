/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.location;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.IconDrawableFactory;
import android.util.Log;

import com.android.settings.widget.AppPreference;
import com.android.settings.widget.RestrictedAppPreference;
import com.android.settingslib.location.BaseSettingsInjector;
import com.android.settingslib.location.InjectedSetting;

import java.util.ArrayList;
import java.util.List;

import androidx.preference.Preference;

/**
 * Adds the preferences specified by the {@link InjectedSetting} objects to a preference group.
 */
public class SettingsInjector extends BaseSettingsInjector {
    static final String TAG = "SettingsInjector";

    Context mContext;

    public SettingsInjector(Context context) {
        super(context);
        mContext = context;
    }

    /**
     * Adds an injected setting to the root.
     */
    private Preference addServiceSetting(Context prefContext, List<Preference> prefs,
            InjectedSetting info) {
        final PackageManager pm = mContext.getPackageManager();
        Drawable appIcon = null;
        try {
            final PackageItemInfo itemInfo = new PackageItemInfo();
            itemInfo.icon = info.iconId;
            itemInfo.packageName = info.packageName;
            final ApplicationInfo appInfo = pm.getApplicationInfo(info.packageName,
                    PackageManager.GET_META_DATA);
            appIcon = IconDrawableFactory.newInstance(mContext)
                    .getBadgedIcon(itemInfo, appInfo, info.mUserHandle.getIdentifier());
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Can't get ApplicationInfo for " + info.packageName, e);
        }
        Preference pref = TextUtils.isEmpty(info.userRestriction)
                ? new AppPreference(prefContext)
                : new RestrictedAppPreference(prefContext, info.userRestriction);
        pref.setTitle(info.title);
        pref.setSummary(null);
        pref.setIcon(appIcon);
        pref.setOnPreferenceClickListener(new ServiceSettingClickedListener(info));
        prefs.add(pref);
        return pref;
    }

    /**
     * Gets a list of preferences that other apps have injected.
     *
     * @param profileId Identifier of the user/profile to obtain the injected settings for or
     *                  UserHandle.USER_CURRENT for all profiles associated with current user.
     */
    public List<Preference> getInjectedSettings(Context prefContext, final int profileId) {
        final UserManager um = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        final List<UserHandle> profiles = um.getUserProfiles();
        ArrayList<Preference> prefs = new ArrayList<>();
        final int profileCount = profiles.size();
        for (int i = 0; i < profileCount; ++i) {
            final UserHandle userHandle = profiles.get(i);
            if (profileId == UserHandle.USER_CURRENT || profileId == userHandle.getIdentifier()) {
                Iterable<InjectedSetting> settings = getSettings(userHandle);
                for (InjectedSetting setting : settings) {
                    Preference pref = addServiceSetting(prefContext, prefs, setting);
                    mSettings.add(new Setting(setting, pref));
                }
            }
        }

        reloadStatusMessages();

        return prefs;
    }
}
