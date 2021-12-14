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

    private boolean mCreated = false;
    private AppLocaleDetailsHelper mAppLocaleDetailsHelper;

    private PreferenceGroup mGroupOfSuggestedLocales;
    private PreferenceGroup mGroupOfSupportedLocales;
    private LayoutPreference mPrefOfDescription;

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
    }

    @Override
    protected boolean refreshUi() {
        if (mAppLocaleDetailsHelper.getSupportedLocales().isEmpty()) {
            Log.d(TAG, "No supported language.");
            mGroupOfSuggestedLocales.setVisible(false);
            mGroupOfSupportedLocales.setVisible(false);
            mPrefOfDescription.setVisible(true);
            TextView description = (TextView) mPrefOfDescription.findViewById(R.id.description);
            Locale locale = mAppLocaleDetailsHelper.getCurrentSystemLocales().get(0);
            description.setText(getContext().getString(R.string.no_multiple_language_supported,
                    locale.getDisplayName(locale)));
            return true;
        }

        mGroupOfSuggestedLocales.removeAll();
        mGroupOfSupportedLocales.removeAll();
        Locale appLocale = AppLocaleDetailsHelper.getAppDefaultLocale(getContext(), mPackageName);
        setLanguagesPreference(mGroupOfSuggestedLocales,
                mAppLocaleDetailsHelper.getSuggestedLocales(), appLocale);
        setLanguagesPreference(mGroupOfSupportedLocales,
                mAppLocaleDetailsHelper.getSupportedLocales(), appLocale);
        return true;
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
        mAppLocaleDetailsHelper.setAppDefaultLocale(pref.getKey());
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
        return appLocale == null ? "" : appLocale.getDisplayName(appLocale);
    }

    private void setLanguagesPreference(PreferenceGroup group,
            Collection<Locale> locales, Locale appLocale) {
        if (locales == null) {
            return;
        }

        for (Locale locale : locales) {
            RadioButtonPreference pref = new RadioButtonPreference(getContext());
            pref.setTitle(locale.getDisplayName(locale));
            pref.setKey(locale.toLanguageTag());
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

        private Collection<Locale> mSuggestedLocales = new ArrayList<>();;
        private Collection<Locale> mSupportedLocales = new ArrayList<>();;

        AppLocaleDetailsHelper(Context context, String packageName) {
            mContext = context;
            mPackageName = packageName;
            mTelephonyManager = context.getSystemService(TelephonyManager.class);
            mLocaleManager = context.getSystemService(LocaleManager.class);
        }

        /** Handle suggested and supported locales for UI display. */
        public void handleAllLocalesData() {
            clearLocalesData();
            handleSuggestedLocales();
            handleSupportedLocales();
        }

        /** Gets suggested locales in the app. */
        public Collection<Locale> getSuggestedLocales() {
            return mSuggestedLocales;
        }

        /** Gets supported locales in the app. */
        public Collection<Locale> getSupportedLocales() {
            return mSupportedLocales;
        }

        @VisibleForTesting
        void handleSuggestedLocales() {
            LocaleList currentSystemLocales = getCurrentSystemLocales();
            Locale simLocale = mTelephonyManager.getSimLocale();
            Locale appLocale = getAppDefaultLocale(mContext, mPackageName);
            // 1st locale in suggested languages group.
            if (appLocale != null) {
                mSuggestedLocales.add(appLocale);
            }
            // 2nd locale in suggested languages group.
            if (simLocale != null && !simLocale.equals(appLocale)) {
                mSuggestedLocales.add(simLocale);
            }
            // Other locales in suggested languages group.
            for (int i = 0; i < currentSystemLocales.size(); i++) {
                Locale locale = currentSystemLocales.get(i);
                if (!locale.equals(appLocale) && !locale.equals(simLocale)) {
                    mSuggestedLocales.add(locale);
                }
            }
        }

        @VisibleForTesting
        void handleSupportedLocales() {
            //TODO Waiting for PackageManager api
            String[] languages = getAssetSystemLocales();

            for (String language : languages) {
                mSupportedLocales.add(Locale.forLanguageTag(language));
            }
            if (mSuggestedLocales != null || !mSuggestedLocales.isEmpty()) {
                mSupportedLocales.removeAll(mSuggestedLocales);
            }
        }

        private void clearLocalesData() {
            mSuggestedLocales.clear();
            mSupportedLocales.clear();
        }

        /** Gets per app's default locale */
        public static Locale getAppDefaultLocale(Context context, String packageName) {
            LocaleManager localeManager = context.getSystemService(LocaleManager.class);
            LocaleList localeList = (localeManager == null)
                    ? new LocaleList() : localeManager.getApplicationLocales(packageName);
            return localeList.isEmpty() ? null : localeList.get(0);
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
        LocaleList getCurrentSystemLocales() {
            return Resources.getSystem().getConfiguration().getLocales();
        }

        @VisibleForTesting
        String[] getAssetSystemLocales() {
            try {
                PackageManager packageManager = mContext.getPackageManager();
                return packageManager.getResourcesForApplication(
                        packageManager.getPackageInfo(mPackageName, PackageManager.MATCH_ALL)
                                .applicationInfo).getAssets().getNonSystemLocales();
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Can not found the package name : " + e);
            }
            return new String[0];
        }
    }
}
