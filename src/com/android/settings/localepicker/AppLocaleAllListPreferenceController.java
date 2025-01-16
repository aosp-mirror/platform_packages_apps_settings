/**
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.localepicker;

import static com.android.settings.localepicker.RegionAndNumberingSystemPickerFragment.EXTRA_APP_PACKAGE_NAME;
import static com.android.settings.localepicker.RegionAndNumberingSystemPickerFragment.EXTRA_IS_NUMBERING_SYSTEM;
import static com.android.settings.localepicker.RegionAndNumberingSystemPickerFragment.EXTRA_TARGET_LOCALE;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.app.AppLocaleCollector;
import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.applications.manageapplications.ManageApplicationsUtil;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.spa.SpaActivity;
import com.android.settingslib.core.instrumentation.Instrumentable;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** A controller for handling supported locale of app. */
public class AppLocaleAllListPreferenceController extends
        BasePreferenceController implements LocaleListSearchCallback {
    private static final String TAG = "AppLocaleAllListPreferenceController";
    private static final String KEY_PREFERENCE_CATEGORY_APP_LANGUAGE_ALL_SUPPORTED =
            "app_language_all_supported_category";
    private static final String KEY_PREFERENCE_APP_LOCALE_LIST = "app_locale_list";
    private static final String KEY_PREFERENCE_CATEGORY_ADD_LANGUAGE_ALL_SUPPORTED =
            "system_language_all_supported_category";

    @SuppressWarnings("NullAway")
    private PreferenceCategory mPreferenceCategory;
    private Set<LocaleStore.LocaleInfo> mLocaleList;
    private List<LocaleStore.LocaleInfo> mLocaleOptions;
    private Map<String, Preference> mSupportedPreferences;
    private boolean mIsCountryMode;
    private boolean mIsNumberingSystemMode;
    @Nullable
    private LocaleStore.LocaleInfo mParentLocale;
    private AppLocaleCollector mAppLocaleCollector;
    @SuppressWarnings("NullAway")
    private String mPackageName;

    @SuppressWarnings("NullAway")
    public AppLocaleAllListPreferenceController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @SuppressWarnings("NullAway")
    public AppLocaleAllListPreferenceController(@NonNull Context context,
            @NonNull String preferenceKey, @Nullable String packageName,
            boolean isNumberingSystemMode, @NonNull LocaleStore.LocaleInfo parentLocale) {
        super(context, preferenceKey);
        mPackageName = packageName;
        mIsNumberingSystemMode = isNumberingSystemMode;
        mParentLocale = parentLocale;
        mIsCountryMode = mParentLocale != null;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceCategory = screen.findPreference(
                (mIsNumberingSystemMode || mIsCountryMode)
                        ? KEY_PREFERENCE_CATEGORY_ADD_LANGUAGE_ALL_SUPPORTED
                        : KEY_PREFERENCE_CATEGORY_APP_LANGUAGE_ALL_SUPPORTED);

        mAppLocaleCollector = new AppLocaleCollector(mContext, mPackageName);
        mSupportedPreferences = new ArrayMap<>();
        mLocaleOptions = new ArrayList<>();
        updatePreferences();
    }

    private void updatePreferences() {
        if (mPreferenceCategory == null) {
            Log.d(TAG, "updatePreferences, mPreferenceCategory is null");
            return;
        }

        List<LocaleStore.LocaleInfo> result = LocaleUtils.getSortedLocaleList(
                getSupportedLocaleList(), mIsCountryMode);
        if (mIsCountryMode) {
            mPreferenceCategory.setTitle(
                    mContext.getString(R.string.all_supported_locales_regions_title));
        }
        final Map<String, Preference> existingSupportedPreferences = mSupportedPreferences;
        mSupportedPreferences = new ArrayMap<>();
        setupSupportedPreference(result, existingSupportedPreferences);
        for (Preference pref : existingSupportedPreferences.values()) {
            mPreferenceCategory.removePreference(pref);
        }
    }

    @Override
    public void onSearchListChanged(@NonNull List<LocaleStore.LocaleInfo> newList,
            @Nullable CharSequence prefix) {
        mPreferenceCategory.removeAll();
        mSupportedPreferences.clear();
        final Map<String, Preference> existingSupportedPreferences = mSupportedPreferences;
        List<LocaleStore.LocaleInfo> sortedList = getSupportedLocaleList();
        newList = LocaleUtils.getSortedLocaleFromSearchList(newList, sortedList, mIsCountryMode);
        setupSupportedPreference(newList, existingSupportedPreferences);
    }

    private void setupSupportedPreference(List<LocaleStore.LocaleInfo> localeInfoList,
            Map<String, Preference> existingSupportedPreferences) {
        if (mIsNumberingSystemMode) {
            mPreferenceCategory.setTitle("");
        }

        for (LocaleStore.LocaleInfo locale : localeInfoList) {
            Preference pref = existingSupportedPreferences.remove(locale.getId());
            if (pref == null) {
                pref = new Preference(mContext);
                mPreferenceCategory.addPreference(pref);
                setupPreference(pref, locale);
            }
        }
        mPreferenceCategory.setVisible(mPreferenceCategory.getPreferenceCount() > 0);
    }

    private void setupPreference(Preference pref, LocaleStore.LocaleInfo locale) {
        String localeName = mIsCountryMode ? locale.getFullCountryNameNative()
                : locale.getFullNameNative();
        pref.setTitle(localeName);
        pref.setKey(locale.toString());
        pref.setOnPreferenceClickListener(clickedPref -> {
            // TODO: b/388199937 - Switch to correct fragment.
            Log.d(TAG, "setupPreference: mIsCountryMode = " + mIsCountryMode);
            switchFragment(mContext, locale, shouldShowAppLanguage(locale));
            return true;
        });
        mSupportedPreferences.put(locale.getId(), pref);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    private void switchFragment(Context context, LocaleStore.LocaleInfo localeInfo,
            boolean shouldShowAppLanguage) {
        if (shouldShowAppLanguage) {
            LocaleUtils.onLocaleSelected(mContext, localeInfo, mPackageName);
        } else {
            String extraKey = EXTRA_TARGET_LOCALE;
            String fragmentName = RegionAndNumberingSystemPickerFragment.class.getCanonicalName();
            final Bundle extra = new Bundle();
            extra.putSerializable(extraKey, localeInfo);
            extra.putBoolean(EXTRA_IS_NUMBERING_SYSTEM, localeInfo.hasNumberingSystems());
            extra.putString(EXTRA_APP_PACKAGE_NAME, mPackageName);
            new SubSettingLauncher(context)
                    .setDestination(fragmentName)
                    .setSourceMetricsCategory(Instrumentable.METRICS_CATEGORY_UNKNOWN)
                    .setArguments(extra)
                    .launch();
        }
        ((Activity) mContext).finish();
    }

    private boolean shouldShowAppLanguage(LocaleStore.LocaleInfo localeInfo) {
        boolean isSystemLocale = localeInfo.isSystemLocale();
        boolean isRegionLocale = localeInfo.getParent() != null;
        boolean mayHaveDifferentNumberingSystem = localeInfo.hasNumberingSystems();
        mLocaleList = mAppLocaleCollector.getSupportedLocaleList(localeInfo,
                false, (localeInfo != null));
        Log.d(TAG,
                "shouldShowAppLanguage: isSystemLocale = " + isSystemLocale + ", isRegionLocale = "
                        + isRegionLocale + ", mayHaveDifferentNumberingSystem = "
                        + mayHaveDifferentNumberingSystem + ", isNumberingMode = "
                        + mIsNumberingSystemMode);

        return mLocaleList.size() == 1 || isSystemLocale || mIsNumberingSystemMode
                || (isRegionLocale && !mayHaveDifferentNumberingSystem);
    }

    protected List<LocaleStore.LocaleInfo> getSupportedLocaleList() {
        setupLocaleList();
        if (mLocaleList != null && !mLocaleList.isEmpty()) {
            mLocaleOptions.addAll(
                    mLocaleList.stream().filter(localeInfo -> !localeInfo.isSuggested()).collect(
                            Collectors.toList()));
        } else {
            Log.d(TAG, "Can not get supported locales because the locale list is null or empty.");
        }
        return mLocaleOptions;
    }

    private void setupLocaleList() {
        mLocaleList = mAppLocaleCollector.getSupportedLocaleList(mParentLocale,
                false, mIsCountryMode);
        mLocaleOptions.clear();
    }

    @Override
    public @NonNull String getPreferenceKey() {
        return KEY_PREFERENCE_APP_LOCALE_LIST;
    }
}
