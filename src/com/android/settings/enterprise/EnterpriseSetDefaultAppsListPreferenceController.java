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
 * limitations under the License
 */

package com.android.settings.enterprise;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.UserHandle;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.applications.EnterpriseDefaultApps;
import com.android.settings.applications.UserAppInfo;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.users.UserFeatureProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;


/**
 * PreferenceController that builds a dynamic list of default apps set by device or profile owner.
 */
public class EnterpriseSetDefaultAppsListPreferenceController extends
        AbstractPreferenceController implements PreferenceControllerMixin {
    private final PackageManager mPm;
    private final SettingsPreferenceFragment mParent;
    private final ApplicationFeatureProvider mApplicationFeatureProvider;
    private final EnterprisePrivacyFeatureProvider mEnterprisePrivacyFeatureProvider;
    private final UserFeatureProvider mUserFeatureProvider;

    private List<UserInfo> mUsers = Collections.emptyList();
    private List<EnumMap<EnterpriseDefaultApps, List<ApplicationInfo>>> mApps =
            Collections.emptyList();

    public EnterpriseSetDefaultAppsListPreferenceController(Context context,
            SettingsPreferenceFragment parent, PackageManager packageManager) {
        super(context);
        mPm = packageManager;
        mParent = parent;
        final FeatureFactory factory = FeatureFactory.getFactory(context);
        mApplicationFeatureProvider = factory.getApplicationFeatureProvider(context);
        mEnterprisePrivacyFeatureProvider = factory.getEnterprisePrivacyFeatureProvider(context);
        mUserFeatureProvider = factory.getUserFeatureProvider(context);
        buildAppList();
    }

    /**
     * Builds data for UI. Updates mUsers and mApps so that they contain non-empty list.
     */
    private void buildAppList() {
        mUsers = new ArrayList<>();
        mApps = new ArrayList<>();
        for (UserHandle user : mUserFeatureProvider.getUserProfiles()) {
            boolean hasDefaultsForUser = false;
            EnumMap<EnterpriseDefaultApps, List<ApplicationInfo>> userMap = null;

            for (EnterpriseDefaultApps typeOfDefault : EnterpriseDefaultApps.values()) {
                List<UserAppInfo> apps = mApplicationFeatureProvider.
                        findPersistentPreferredActivities(user.getIdentifier(),
                                typeOfDefault.getIntents());
                if (apps.isEmpty()) {
                    continue;
                }
                if (!hasDefaultsForUser) {
                    hasDefaultsForUser = true;
                    mUsers.add(apps.get(0).userInfo);
                    userMap = new EnumMap<>(EnterpriseDefaultApps.class);
                    mApps.add(userMap);
                }
                ArrayList<ApplicationInfo> applicationInfos = new ArrayList<>();
                for (UserAppInfo userAppInfo : apps) {
                    applicationInfos.add(userAppInfo.appInfo);
                }
                userMap.put(typeOfDefault, applicationInfos);
            }
        }
        ThreadUtils.postOnMainThread(() -> updateUi());
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return null;
    }

    private void updateUi() {
        final Context prefContext = mParent.getPreferenceManager().getContext();
        final PreferenceScreen screen = mParent.getPreferenceScreen();
        if (screen == null) {
            return;
        }
        if (!mEnterprisePrivacyFeatureProvider.isInCompMode() && mUsers.size() == 1) {
            createPreferences(prefContext, screen, mApps.get(0));
        } else {
            for (int i = 0; i < mUsers.size(); i++) {
                final UserInfo userInfo = mUsers.get(i);
                final PreferenceCategory category = new PreferenceCategory(prefContext);
                screen.addPreference(category);
                if (userInfo.isManagedProfile()) {
                    category.setTitle(R.string.category_work);
                } else {
                    category.setTitle(R.string.category_personal);
                }
                category.setOrder(i);
                createPreferences(prefContext, category, mApps.get(i));
            }
        }
    }

    private void createPreferences(Context prefContext, PreferenceGroup group,
            EnumMap<EnterpriseDefaultApps, List<ApplicationInfo>> apps) {
        if (group == null) {
            return;
        }
        for (EnterpriseDefaultApps typeOfDefault : EnterpriseDefaultApps.values()) {
            final List<ApplicationInfo> appsForCategory = apps.get(typeOfDefault);
            if (appsForCategory == null || appsForCategory.isEmpty()) {
                continue;
            }
            final Preference preference = new Preference(prefContext);
            preference.setTitle(getTitle(prefContext, typeOfDefault, appsForCategory.size()));
            preference.setSummary(buildSummaryString(prefContext, appsForCategory));
            preference.setOrder(typeOfDefault.ordinal());
            preference.setSelectable(false);
            group.addPreference(preference);
        }
    }

    private CharSequence buildSummaryString(Context context, List<ApplicationInfo> apps) {
        final CharSequence[] appNames = new String[apps.size()];
        for (int i = 0; i < apps.size(); i++) {
            appNames[i] = apps.get(i).loadLabel(mPm);
        }
        if (apps.size() == 1) {
            return appNames[0];
        } else if (apps.size() == 2) {
            return context.getString(R.string.app_names_concatenation_template_2, appNames[0],
                    appNames[1]);
        } else {
            return context.getString(R.string.app_names_concatenation_template_3, appNames[0],
                    appNames[1], appNames[2]);
        }
    }

    private String getTitle(Context context, EnterpriseDefaultApps typeOfDefault, int appCount) {
        switch (typeOfDefault) {
            case BROWSER:
                return context.getString(R.string.default_browser_title);
            case CALENDAR:
                return context.getString(R.string.default_calendar_app_title);
            case CONTACTS:
                return context.getString(R.string.default_contacts_app_title);
            case PHONE:
                return context.getResources()
                        .getQuantityString(R.plurals.default_phone_app_title, appCount);
            case MAP:
                return context.getString(R.string.default_map_app_title);
            case EMAIL:
                return context.getResources()
                        .getQuantityString(R.plurals.default_email_app_title, appCount);
            case CAMERA:
                return context.getResources()
                        .getQuantityString(R.plurals.default_camera_app_title, appCount);
            default:
                throw new IllegalStateException("Unknown type of default " + typeOfDefault);
        }
    }

}
