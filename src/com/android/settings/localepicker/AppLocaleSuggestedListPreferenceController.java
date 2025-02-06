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

import static com.android.settings.applications.manageapplications.ManageApplications.LIST_TYPE_APPS_LOCALE;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
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
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** A controller for handling suggested locale of app. */
public class AppLocaleSuggestedListPreferenceController extends
        BasePreferenceController implements LocaleListSearchCallback {
    private static final String TAG = "AppLocaleSuggestedListPreferenceController";
    private static final String KEY_PREFERENCE_CATEGORY_APP_LANGUAGE_SUGGESTED =
            "app_language_suggested_category";
    private static final String KEY_PREFERENCE_APP_LOCALE_SUGGESTED_LIST =
            "app_locale_suggested_list";
    private static final String KEY_PREFERENCE_CATEGORY_ADD_A_LANGUAGE_SUGGESTED =
            "system_language_suggested_category";

    @SuppressWarnings("NullAway")
    private PreferenceCategory mPreferenceCategory;
    private Set<LocaleStore.LocaleInfo> mLocaleList;
    private List<LocaleStore.LocaleInfo> mLocaleOptions;
    private Map<String, Preference> mSuggestedPreferences;
    private boolean mIsCountryMode;
    @Nullable private LocaleStore.LocaleInfo mParentLocale;
    private AppLocaleCollector mAppLocaleCollector;
    @SuppressWarnings("NullAway")
    private String mPackageName;
    private boolean mIsNumberingSystemMode;

    @SuppressWarnings("NullAway")
    public AppLocaleSuggestedListPreferenceController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @SuppressWarnings("NullAway")
    public AppLocaleSuggestedListPreferenceController(@NonNull Context context,
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
                        ? KEY_PREFERENCE_CATEGORY_ADD_A_LANGUAGE_SUGGESTED
                        : KEY_PREFERENCE_CATEGORY_APP_LANGUAGE_SUGGESTED);

        mAppLocaleCollector = new AppLocaleCollector(mContext, mPackageName);
        mSuggestedPreferences = new ArrayMap<>();
        mLocaleOptions = new ArrayList<>();
        updatePreferences();
    }

    private void updatePreferences() {
        if (mPreferenceCategory == null) {
            Log.d(TAG, "updatePreferences, mPreferenceCategory is null");
            return;
        }

        List<LocaleStore.LocaleInfo> result = LocaleUtils.getSortedLocaleList(
                getSuggestedLocaleList(), mIsCountryMode);
        final Map<String, Preference> existingSuggestedPreferences = mSuggestedPreferences;
        mSuggestedPreferences = new ArrayMap<>();
        setupSuggestedPreference(result, existingSuggestedPreferences);
        for (Preference pref : existingSuggestedPreferences.values()) {
            mPreferenceCategory.removePreference(pref);
        }
    }

    @Override
    public void onSearchListChanged(@NonNull List<LocaleStore.LocaleInfo> newList,
            @Nullable CharSequence prefix) {
        if (mPreferenceCategory == null) {
            Log.d(TAG, "onSearchListChanged, mPreferenceCategory is null");
            return;
        }

        mPreferenceCategory.removeAll();
        final Map<String, Preference> existingSuggestedPreferences = mSuggestedPreferences;
        List<LocaleStore.LocaleInfo> sortedList = getSuggestedLocaleList();
        newList = LocaleUtils.getSortedLocaleFromSearchList(newList, sortedList, mIsCountryMode);
        setupSuggestedPreference(newList, existingSuggestedPreferences);
    }

    private void setupSuggestedPreference(List<LocaleStore.LocaleInfo> localeInfoList,
            Map<String, Preference> existingSuggestedPreferences) {
        for (LocaleStore.LocaleInfo locale : localeInfoList) {
            if (mIsNumberingSystemMode || mIsCountryMode) {
                Preference pref = existingSuggestedPreferences.remove(locale.getId());
                if (pref == null) {
                    pref = new Preference(mContext);
                    setupPreference(pref, locale);
                    mPreferenceCategory.addPreference(pref);
                }
            } else {
                SelectorWithWidgetPreference pref =
                        (SelectorWithWidgetPreference) existingSuggestedPreferences.remove(
                                locale.getId());
                if (pref == null) {
                    pref = new SelectorWithWidgetPreference(mContext);
                    setupPreference(pref, locale);
                    mPreferenceCategory.addPreference(pref);
                }
            }
        }
        Log.d(TAG, "setupSuggestedPreference, mPreferenceCategory setVisible"
                + (mPreferenceCategory.getPreferenceCount() > 0));
        mPreferenceCategory.setVisible(mPreferenceCategory.getPreferenceCount() > 0);
    }

    private void setupPreference(Preference pref, LocaleStore.LocaleInfo locale) {
        String localeName = mIsCountryMode ? locale.getFullCountryNameNative()
                : locale.getFullNameNative();
        if (pref instanceof SelectorWithWidgetPreference) {
            ((SelectorWithWidgetPreference) pref).setChecked(locale.isAppCurrentLocale());
        }
        pref.setTitle(locale.isSystemLocale()
                ? mContext.getString(R.string.preference_of_system_locale_summary)
                : localeName);
        pref.setKey(locale.toString());
        pref.setOnPreferenceClickListener(clickedPref -> {
            LocaleUtils.onLocaleSelected(mContext, locale, mPackageName);
            ((Activity) mContext).finish();
            return true;
        });
        mSuggestedPreferences.put(locale.getId(), pref);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    protected List<LocaleStore.LocaleInfo> getSuggestedLocaleList() {
        setupLocaleList();
        if (mLocaleList != null && !mLocaleList.isEmpty()) {
            mLocaleOptions.addAll(
                    mLocaleList.stream().filter(localeInfo -> (localeInfo.isSuggested())).collect(
                            Collectors.toList()));
        } else {
            Log.d(TAG, "Can not get suggested locales because the locale list is null or empty.");
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
        return KEY_PREFERENCE_APP_LOCALE_SUGGESTED_LIST;
    }
}
