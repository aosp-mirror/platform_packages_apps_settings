/**
 * Copyright (C) 2024 The Android Open Source Project
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

import android.content.Context;
import android.os.LocaleList;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.app.LocaleCollectorBase;
import com.android.internal.app.LocaleHelper;
import com.android.internal.app.LocaleStore;
import com.android.settings.core.BasePreferenceController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** A base controller for handling locale controllers. */
public abstract class LocalePickerBaseListPreferenceController extends
        BasePreferenceController implements LocaleListSearchCallback {

    private static final String TAG = "LocalePickerBaseListPreference";
    private static final String KEY_SUGGESTED = "suggested";

    private PreferenceCategory mPreferenceCategory;
    private LocaleList mExplicitLocales;
    private Set<LocaleStore.LocaleInfo> mLocaleList;
    private List<LocaleStore.LocaleInfo> mLocaleOptions;
    private Map<String, Preference> mPreferences;
    private String mPackageName;
    private boolean mCountryMode;

    public LocalePickerBaseListPreferenceController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
        // TODO: Should get extra from fragment.
//        if (isDeviceDemoMode()) {
//            Bundle bundle = preference.getExtras();
//            mExplicitLocales = bundle == null
//                    ? null
//                    : bundle.getParcelable(Settings.EXTRA_EXPLICIT_LOCALES, LocaleList.class);
//            Log.d(TAG, "Has explicit locales : " + mExplicitLocales);
//        }
        mLocaleList = getLocaleCollectorController(context).getSupportedLocaleList(null,
                false, false);
        mLocaleOptions = new ArrayList<>(mLocaleList.size());
        mPreferences = new ArrayMap<>();
    }

    private boolean isDeviceDemoMode() {
        return Settings.Global.getInt(
                mContext.getContentResolver(), Settings.Global.DEVICE_DEMO_MODE, 0) == 1;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceCategory = screen.findPreference(getPreferenceCategoryKey());
        updatePreferences();
    }

    private void updatePreferences() {
        if (mPreferenceCategory == null) {
            Log.d(TAG, "updatePreferences, mPreferenceCategory is null");
            return;
        }

        List<LocaleStore.LocaleInfo> result;
        result = getSortedLocaleList(
                getPreferenceCategoryKey().contains(KEY_SUGGESTED) ? getSuggestedLocaleList()
                        : getSupportedLocaleList());

        final Map<String, Preference> existingPreferences = mPreferences;
        mPreferences = new ArrayMap<>();
        setupPreference(result, existingPreferences);

        for (Preference pref : existingPreferences.values()) {
            mPreferenceCategory.removePreference(pref);
        }
    }

    @Override
    public void onSearchListChanged(@NonNull List<LocaleStore.LocaleInfo> newList) {
        mPreferenceCategory.removeAll();
        mPreferences.clear();
        final Map<String, Preference> existingPreferences = mPreferences;
        if (getPreferenceCategoryKey().contains(KEY_SUGGESTED)) {
            newList = getSortedSuggestedLocaleFromSearchList(
                    newList, getSuggestedLocaleList());
        }
        if (!newList.isEmpty()) {
            mPreferenceCategory.setVisible(true);
            setupPreference(newList, existingPreferences);
        } else {
            mPreferenceCategory.setVisible(false);
        }
    }

    private List<LocaleStore.LocaleInfo> getSortedSuggestedLocaleFromSearchList(
            List<LocaleStore.LocaleInfo> listOptions,
            List<LocaleStore.LocaleInfo> listSuggested) {
        List<LocaleStore.LocaleInfo> searchItem = new ArrayList<>();
        for (LocaleStore.LocaleInfo option : listOptions) {
            for (LocaleStore.LocaleInfo suggested : listSuggested) {
                if (suggested.toString().contains(option.toString())) {
                    searchItem.add(suggested);
                }
            }
        }
        searchItem = getSortedLocaleList(searchItem);
        return searchItem;
    }

    private void setupPreference(List<LocaleStore.LocaleInfo> localeInfoList,
            Map<String, Preference> existingPreferences) {
        localeInfoList.stream().forEach(locale ->
        {
            Preference pref = existingPreferences.remove(locale.getId());
            if (pref == null) {
                pref = new Preference(mContext);
                mPreferenceCategory.addPreference(pref);
            }
            String localeName =
                    mCountryMode ? locale.getFullCountryNameNative() : locale.getFullNameNative();
            pref.setTitle(localeName);
            pref.setKey(locale.toString());
            pref.setOnPreferenceClickListener(clickedPref -> {
                // TODO: Click locale to show region or numbering system page if needed.
                return true;
            });
            mPreferences.put(locale.getId(), pref);
        });
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    protected abstract String getPreferenceCategoryKey();

    protected abstract LocaleCollectorBase getLocaleCollectorController(Context context);

    protected String getPackageName() {
        return mPackageName;
    }

    protected LocaleList getExplicitLocaleList() {
        return mExplicitLocales;
    }

    protected List<LocaleStore.LocaleInfo> getSuggestedLocaleList() {
        mLocaleOptions.clear();
        if (mLocaleList != null && !mLocaleList.isEmpty()) {
            mLocaleOptions.addAll(mLocaleList.stream()
                    .filter(localeInfo -> localeInfo.isSuggested())
                    .collect(Collectors.toList()));
        } else {
            Log.d(TAG, "Can not get suggested locales because the locale list is null or empty.");
        }

        return mLocaleOptions;
    }

    protected List<LocaleStore.LocaleInfo> getSupportedLocaleList() {
        if (mLocaleList != null && !mLocaleList.isEmpty()) {
            mLocaleOptions.addAll(mLocaleList.stream()
                    .filter(localeInfo -> !localeInfo.isSuggested())
                    .collect(Collectors.toList()));
        } else {
            Log.d(TAG, "Can not get supported locales because the locale list is null or empty.");
        }

        return mLocaleOptions;
    }

    private List<LocaleStore.LocaleInfo> getSortedLocaleList(
            List<LocaleStore.LocaleInfo> localeInfos) {
        final Locale sortingLocale = Locale.getDefault();
        final LocaleHelper.LocaleInfoComparator comp =
                new LocaleHelper.LocaleInfoComparator(sortingLocale, mCountryMode);
        Collections.sort(localeInfos, comp);
        return localeInfos;
    }
}
