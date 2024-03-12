/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static com.android.settings.widget.EntityHeaderController.ActionType;

import android.app.Activity;
import android.app.LocaleManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.UserHandle;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.preference.Preference;

import com.android.internal.app.LocaleHelper;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.applications.AppLocaleUtil;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.widget.LayoutPreference;

import java.util.Locale;

/**
 * TODO(b/223503670): Implement the unittest.
 * A fragment to show the current app locale info.
 */
public class AppLocaleDetails extends SettingsPreferenceFragment {
    private static final String TAG = "AppLocaleDetails";

    private static final String KEY_APP_DESCRIPTION = "app_locale_description";
    private static final String KEY_APP_DISCLAIMER = "app_locale_disclaimer";

    private boolean mCreated = false;
    private String mPackageName;
    private LayoutPreference mPrefOfDescription;
    private Preference mPrefOfDisclaimer;
    private ApplicationInfo mApplicationInfo;

    /**
     * Create a instance of AppLocaleDetails.
     * @param packageName Indicates which application need to show the locale picker.
     * @param uid User id.
     */
    public static AppLocaleDetails newInstance(String packageName, int uid) {
        AppLocaleDetails appLocaleDetails = new AppLocaleDetails();
        Bundle bundle = new Bundle();
        bundle.putString(AppInfoBase.ARG_PACKAGE_NAME, packageName);
        bundle.putInt(AppInfoBase.ARG_PACKAGE_UID, uid);
        appLocaleDetails.setArguments(bundle);
        return appLocaleDetails;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        mPackageName = bundle.getString(AppInfoBase.ARG_PACKAGE_NAME, "");
        if (mPackageName.isEmpty()) {
            Log.d(TAG, "There is no package name.");
            finish();
        }
        int uid = bundle.getInt(AppInfoBase.ARG_PACKAGE_UID, getContext().getUserId());

        addPreferencesFromResource(R.xml.app_locale_details);
        mPrefOfDescription = getPreferenceScreen().findPreference(KEY_APP_DESCRIPTION);
        mPrefOfDisclaimer = getPreferenceScreen().findPreference(KEY_APP_DISCLAIMER);
        mApplicationInfo = getApplicationInfo(mPackageName, uid);
        setDisclaimerPreference();
    }

    // Override here so we don't have an empty screen
    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        // if we don't have a package, show a page saying this is unsupported
        if (mPackageName.isEmpty()) {
            return inflater.inflate(R.layout.manage_applications_apps_unsupported, null);
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUi();
    }

    private void refreshUi() {
        setDescription();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.APPS_LOCALE_LIST;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mCreated) {
            Log.w(TAG, "onActivityCreated: ignoring duplicate call");
            return;
        }
        mCreated = true;
        if (mPackageName == null) {
            return;
        }
        // Creates a head icon button of app on this page.
        final Activity activity = getActivity();
        final Preference pref = EntityHeaderController
                .newInstance(activity, this, null /* header */)
                .setIcon(Utils.getBadgedIcon(getContext(), mApplicationInfo))
                .setLabel(mApplicationInfo.loadLabel(getContext().getPackageManager()))
                .setIsInstantApp(AppUtils.isInstant(mApplicationInfo))
                .setPackageName(mPackageName)
                .setUid(mApplicationInfo.uid)
                .setHasAppInfoLink(true)
                .setButtonActions(ActionType.ACTION_NONE, ActionType.ACTION_NONE)
                .setOrder(10)
                .done(getPrefContext());
        getPreferenceScreen().addPreference(pref);
    }

    private void setDisclaimerPreference() {
        if (FeatureFlagUtils.isEnabled(
                getContext(), FeatureFlagUtils.SETTINGS_APP_LOCALE_OPT_IN_ENABLED)) {
            mPrefOfDisclaimer.setVisible(false);
        }
    }

    private void setDescription() {
        int res = getAppDescription();
        if (res != -1) {
            mPrefOfDescription.setVisible(true);
            TextView description = (TextView) mPrefOfDescription.findViewById(R.id.description);
            description.setText(getContext().getString(res));
        }
    }

    private ApplicationInfo getApplicationInfo(String packageName, int userId) {
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = getContext().getPackageManager()
                    .getApplicationInfoAsUser(packageName, /* flags= */ 0, userId);
            return applicationInfo;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Application info not found for: " + packageName);
            return null;
        }
    }

    private int getAppDescription() {
        LocaleList packageLocaleList = AppLocaleUtil.getPackageLocales(getContext(), mPackageName);
        String[] assetLocaleList = AppLocaleUtil.getAssetLocales(getContext(), mPackageName);
        // TODO add apended url string, "Learn more", to these both sentenses.
        if ((packageLocaleList != null && packageLocaleList.isEmpty())
                || (packageLocaleList == null && assetLocaleList.length == 0)) {
            return R.string.desc_no_available_supported_locale;
        }
        return -1;
    }

    /** Gets per app's default locale */
    public static Locale getAppDefaultLocale(Context context, String packageName) {
        LocaleManager localeManager = context.getSystemService(LocaleManager.class);
        try {
            LocaleList localeList = (localeManager == null)
                    ? null : localeManager.getApplicationLocales(packageName);
            return localeList == null ? null : localeList.get(0);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "package name : " + packageName + " is not correct. " + e);
        }
        return null;
    }

    /**
     * TODO (b209962418) Do a performance test to low end device.
     * @return Return the summary to show the current app's language.
     */
    public static CharSequence getSummary(Context context, ApplicationInfo app) {
        final UserHandle userHandle = UserHandle.getUserHandleForUid(app.uid);
        final Context contextAsUser = context.createContextAsUser(userHandle, 0);
        Locale appLocale = getAppDefaultLocale(contextAsUser, app.packageName);
        if (appLocale == null) {
            return context.getString(R.string.preference_of_system_locale_summary);
        } else {
            return LocaleHelper.getDisplayName(appLocale.stripExtensions(), appLocale, true);
        }
    }
}
