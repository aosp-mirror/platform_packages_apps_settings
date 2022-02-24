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
import android.app.LocaleConfig;
import android.app.LocaleManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.LocaleList;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.widget.LayoutPreference;
import com.android.settingslib.widget.RadioButtonPreference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

/**
 * A fragment to show the current app locale info and help the user to select the expected locale.
 */
public class AppLocaleDetails extends AppInfoBase implements RadioButtonPreference.OnClickListener {
    private static final String TAG = "AppLocaleDetails";

    private static final String CATEGORY_KEY_SUGGESTED_LANGUAGES =
            "category_key_suggested_languages";
    private static final String CATEGORY_KEY_ALL_LANGUAGES =
            "category_key_all_languages";
    private static final String KEY_APP_DESCRIPTION = "app_locale_description";
    @VisibleForTesting
    static final String KEY_SYSTEM_DEFAULT_LOCALE = "system_default_locale";

    private boolean mCreated = false;
    @VisibleForTesting
    AppLocaleDetailsHelper mAppLocaleDetailsHelper;

    private PreferenceGroup mGroupOfSuggestedLocales;
    private PreferenceGroup mGroupOfSupportedLocales;
    private LayoutPreference mPrefOfDescription;
    private RadioButtonPreference mDefaultPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.app_locale_details);
        mAppLocaleDetailsHelper = new AppLocaleDetailsHelper(getContext(), mPackageName);

        mGroupOfSuggestedLocales =
                getPreferenceScreen().findPreference(CATEGORY_KEY_SUGGESTED_LANGUAGES);
        mGroupOfSupportedLocales =
                getPreferenceScreen().findPreference(CATEGORY_KEY_ALL_LANGUAGES);
        mPrefOfDescription = getPreferenceScreen().findPreference(KEY_APP_DESCRIPTION);

        mDefaultPreference = (RadioButtonPreference) getPreferenceScreen()
                .findPreference(KEY_SYSTEM_DEFAULT_LOCALE);
        mDefaultPreference.setOnClickListener(this);
    }

    // Override here so we don't have an empty screen
    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        // if we don't have a package info, show a page saying this is unsupported
        if (mPackageInfo == null) {
            return inflater.inflate(R.layout.manage_applications_apps_unsupported, null);
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        // Update Locales first, before refresh ui.
        mAppLocaleDetailsHelper.handleAllLocalesData();
        super.onResume();
        mDefaultPreference.setSummary(Locale.getDefault().getDisplayName(Locale.getDefault()));
    }

    @Override
    protected boolean refreshUi() {
        refreshUiInternal();
        return true;
    }

    @VisibleForTesting
    void refreshUiInternal() {
        if (mAppLocaleDetailsHelper.getSupportedLocales().isEmpty()) {
            Log.d(TAG, "No supported language.");
            mGroupOfSuggestedLocales.setVisible(false);
            mGroupOfSupportedLocales.setVisible(false);
            mPrefOfDescription.setVisible(true);
            TextView description = (TextView) mPrefOfDescription.findViewById(R.id.description);
            description.setText(getContext().getString(R.string.no_multiple_language_supported,
                    Locale.getDefault().getDisplayName(Locale.getDefault())));
            return;
        }
        resetLocalePreferences();
        Locale appLocale = AppLocaleDetailsHelper.getAppDefaultLocale(getContext(), mPackageName);
        // Sets up default locale preference.
        mGroupOfSuggestedLocales.addPreference(mDefaultPreference);
        mDefaultPreference.setChecked(appLocale == null);
        // Sets up suggested locales of per app.
        setLanguagesPreference(mGroupOfSuggestedLocales,
                mAppLocaleDetailsHelper.getSuggestedLocales(), appLocale);
        // Sets up supported locales of per app.
        setLanguagesPreference(mGroupOfSupportedLocales,
                mAppLocaleDetailsHelper.getSupportedLocales(), appLocale);
    }

    private void resetLocalePreferences() {
        mGroupOfSuggestedLocales.removeAll();
        mGroupOfSupportedLocales.removeAll();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.APPS_LOCALE_LIST;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference pref) {
        String key = pref.getKey();
        if (KEY_SYSTEM_DEFAULT_LOCALE.equals(key)) {
            mAppLocaleDetailsHelper.setAppDefaultLocale(LocaleList.forLanguageTags(""));
        } else {
            mAppLocaleDetailsHelper.setAppDefaultLocale(key);
        }
        refreshUi();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mCreated) {
            Log.w(TAG, "onActivityCreated: ignoring duplicate call");
            return;
        }
        mCreated = true;
        if (mPackageInfo == null) {
            return;
        }
        // Creates a head icon button of app on this page.
        final Activity activity = getActivity();
        final Preference pref = EntityHeaderController
                .newInstance(activity, this, null /* header */)
                .setRecyclerView(getListView(), getSettingsLifecycle())
                .setIcon(Utils.getBadgedIcon(getContext(), mPackageInfo.applicationInfo))
                .setLabel(mPackageInfo.applicationInfo.loadLabel(mPm))
                .setIsInstantApp(AppUtils.isInstant(mPackageInfo.applicationInfo))
                .setPackageName(mPackageName)
                .setUid(mPackageInfo.applicationInfo.uid)
                .setHasAppInfoLink(true)
                .setButtonActions(ActionType.ACTION_NONE, ActionType.ACTION_NONE)
                .done(activity, getPrefContext());
        getPreferenceScreen().addPreference(pref);
    }

    /**
     * TODO (b209962418) Do a performance test to low end device.
     * @return Return the summary to show the current app's language.
     */
    public static CharSequence getSummary(Context context, String packageName) {
        Locale appLocale =
                AppLocaleDetailsHelper.getAppDefaultLocale(context, packageName);
        if (appLocale == null) {
            Locale systemLocale = Locale.getDefault();
            return context.getString(R.string.preference_of_system_locale_summary,
                    systemLocale.getDisplayName(systemLocale));
        } else {
            return appLocale.getDisplayName(appLocale);
        }
    }

    private void setLanguagesPreference(PreferenceGroup group,
            Collection<Locale> locales, Locale appLocale) {
        if (locales == null) {
            return;
        }

        for (Locale locale : locales) {
            if (locale == null) {
                continue;
            }

            RadioButtonPreference pref = new RadioButtonPreference(getContext());
            pref.setTitle(locale.getDisplayName(locale));
            pref.setKey(locale.toLanguageTag());
            // Will never be checked if appLocale is null
            // aka if there is no per-app locale
            pref.setChecked(locale.equals(appLocale));
            pref.setOnClickListener(this);
            group.addPreference(pref);
        }
    }

    @VisibleForTesting
    static class AppLocaleDetailsHelper {
        private String mPackageName;
        private Context mContext;
        private TelephonyManager mTelephonyManager;
        private LocaleManager mLocaleManager;

        private Collection<Locale> mProcessedSuggestedLocales = new ArrayList<>();
        private Collection<Locale> mProcessedSupportedLocales = new ArrayList<>();

        private Collection<Locale> mAppSupportedLocales = new ArrayList<>();

        AppLocaleDetailsHelper(Context context, String packageName) {
            mContext = context;
            mPackageName = packageName;
            mTelephonyManager = context.getSystemService(TelephonyManager.class);
            mLocaleManager = context.getSystemService(LocaleManager.class);
            mAppSupportedLocales = getAppSupportedLocales();
        }

        /** Handle suggested and supported locales for UI display. */
        public void handleAllLocalesData() {
            clearLocalesData();
            handleSuggestedLocales();
            handleSupportedLocales();
        }

        /** Gets suggested locales in the app. */
        public Collection<Locale> getSuggestedLocales() {
            return mProcessedSuggestedLocales;
        }

        /** Gets supported locales in the app. */
        public Collection<Locale> getSupportedLocales() {
            return mProcessedSupportedLocales;
        }

        @VisibleForTesting
        void handleSuggestedLocales() {
            Locale appLocale = getAppDefaultLocale(mContext, mPackageName);
            // 1st locale in suggested languages group.
            for (Locale supportedlocale : mAppSupportedLocales) {
                if (compareLocale(supportedlocale, appLocale)) {
                    mProcessedSuggestedLocales.add(appLocale);
                    break;
                }
            }

            // 2nd and 3rd locale in suggested languages group.
            String simCountry = mTelephonyManager.getSimCountryIso().toUpperCase(Locale.US);
            String networkCountry = mTelephonyManager.getNetworkCountryIso().toUpperCase(Locale.US);
            mAppSupportedLocales.forEach(supportedlocale -> {
                String localeCountry = supportedlocale.getCountry().toUpperCase(Locale.US);
                if (!compareLocale(supportedlocale, appLocale)
                        && isCountrySuggestedLocale(localeCountry, simCountry, networkCountry)) {
                    mProcessedSuggestedLocales.add(supportedlocale);
                }
            });

            // Other locales in suggested languages group.
            Collection<Locale> supportedSystemLocales = new ArrayList<>();
            getCurrentSystemLocales().forEach(systemLocale -> {
                mAppSupportedLocales.forEach(supportedLocale -> {
                    if (compareLocale(systemLocale, supportedLocale)) {
                        supportedSystemLocales.add(supportedLocale);
                    }
                });
            });
            supportedSystemLocales.removeAll(mProcessedSuggestedLocales);
            mProcessedSuggestedLocales.addAll(supportedSystemLocales);
        }

        @VisibleForTesting
        static boolean compareLocale(Locale source, Locale target) {
            if (source == null && target == null) {
                return true;
            } else if (source != null && target != null) {
                return LocaleList.matchesLanguageAndScript(source, target);
            } else {
                return false;
            }
        }

        private static boolean isCountrySuggestedLocale(String localeCountry,
                String simCountry,
                String networkCountry) {
            return ((!simCountry.isEmpty() && simCountry.equals(localeCountry))
                    || (!networkCountry.isEmpty() && networkCountry.equals(localeCountry)));
        }

        @VisibleForTesting
        void handleSupportedLocales() {
            mProcessedSupportedLocales.addAll(mAppSupportedLocales);
            if (mProcessedSuggestedLocales != null || !mProcessedSuggestedLocales.isEmpty()) {
                mProcessedSuggestedLocales.retainAll(mProcessedSupportedLocales);
                mProcessedSupportedLocales.removeAll(mProcessedSuggestedLocales);
            }
        }

        private void clearLocalesData() {
            mProcessedSuggestedLocales.clear();
            mProcessedSupportedLocales.clear();
        }

        private Collection<Locale> getAppSupportedLocales() {
            Collection<Locale> appSupportedLocales = new ArrayList<>();
            LocaleList localeList = getPackageLocales();

            if (localeList != null && localeList.size() > 0) {
                for (int i = 0; i < localeList.size(); i++) {
                    appSupportedLocales.add(localeList.get(i));
                }
            } else {
                String[] languages = getAssetLocales();
                for (String language : languages) {
                    appSupportedLocales.add(Locale.forLanguageTag(language));
                }
            }
            return appSupportedLocales;
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

        /** Sets per app's default language to system. */
        public void setAppDefaultLocale(String languageTag) {
            if (languageTag.isEmpty()) {
                Log.w(TAG, "[setAppDefaultLocale] No language tag.");
                return;
            }
            setAppDefaultLocale(LocaleList.forLanguageTags(languageTag));
        }

        /** Sets per app's default language to system. */
        public void setAppDefaultLocale(LocaleList localeList) {
            if (mLocaleManager == null) {
                Log.w(TAG, "LocaleManager is null, and cannot set the app locale up.");
                return;
            }
            mLocaleManager.setApplicationLocales(mPackageName, localeList);
        }

        @VisibleForTesting
        Collection<Locale> getCurrentSystemLocales() {
            LocaleList localeList = Resources.getSystem().getConfiguration().getLocales();
            Collection<Locale> systemLocales = new ArrayList<>();
            for (int i = 0; i < localeList.size(); i++) {
                systemLocales.add(localeList.get(i));
            }
            return systemLocales;
        }

        @VisibleForTesting
        String[] getAssetLocales() {
            try {
                PackageManager packageManager = mContext.getPackageManager();
                String[] locales = packageManager.getResourcesForApplication(
                        packageManager.getPackageInfo(mPackageName, PackageManager.MATCH_ALL)
                                .applicationInfo).getAssets().getNonSystemLocales();
                if (locales == null) {
                    Log.i(TAG, "[" + mPackageName + "] locales are null.");
                }
                if (locales.length <= 0) {
                    Log.i(TAG, "[" + mPackageName + "] locales length is 0.");
                    return new String[0];
                }
                String locale = locales[0];
                Log.i(TAG, "First asset locale - [" + mPackageName + "] " + locale);
                return locales;
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Can not found the package name : " + mPackageName + " / " + e);
            }
            return new String[0];
        }

        @VisibleForTesting
        LocaleList getPackageLocales() {
            try {
                LocaleConfig localeConfig =
                        new LocaleConfig(mContext.createPackageContext(mPackageName, 0));
                if (localeConfig.getStatus() == LocaleConfig.STATUS_SUCCESS) {
                    return localeConfig.getSupportedLocales();
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Can not found the package name : " + mPackageName + " / " + e);
            }
            return null;
        }
    }
}
