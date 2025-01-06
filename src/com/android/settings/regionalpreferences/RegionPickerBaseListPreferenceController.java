/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.regionalpreferences;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.Initializer;
import com.android.internal.app.LocaleCollectorBase;
import com.android.internal.app.LocaleHelper;
import com.android.internal.app.LocaleStore;
import com.android.internal.app.LocaleStore.LocaleInfo;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class RegionPickerBaseListPreferenceController extends BasePreferenceController {

    private static final String TAG = "RegionPickerBaseListPreferenceController";
    private static final String KEY_SUGGESTED = "suggested";
    private static final String TAG_DIALOG_CHANGE_REGION = "dialog_change_region";
    private PreferenceCategory mPreferenceCategory;
    private Set<LocaleInfo> mLocaleList;
    private ArrayList<LocaleInfo> mLocaleOptions;
    private Fragment mParent;
    private FragmentManager mFragmentManager;

    public RegionPickerBaseListPreferenceController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mLocaleList = getLocaleCollectorController(context).getSupportedLocaleList(null,
            false, false);
        mLocaleOptions = new ArrayList<>();
        mLocaleOptions.ensureCapacity(mLocaleList.size());
    }

    public void setFragment(@NonNull Fragment parent) {
        mParent = parent;
    }

    @Override
    @Initializer
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
        LocaleStore.LocaleInfo parentLocale = getParentLocale();
        if (parentLocale != null) {
            mLocaleList = getLocaleCollectorController(mContext).getSupportedLocaleList(
                    parentLocale, false, true);
        }
        mLocaleOptions.clear();
        mLocaleOptions.ensureCapacity(mLocaleList.size());
        result = getPreferenceCategoryKey().contains(KEY_SUGGESTED)
                    ? getSuggestedLocaleList()
                    : getSupportedLocaleList();
        if (getPreferenceCategoryKey().contains(KEY_SUGGESTED)) {
            Locale systemLocale = Locale.getDefault();
            LocaleStore.LocaleInfo localeInfo = LocaleStore.getLocaleInfo(systemLocale);
            result.add(localeInfo);
        }
        result = getSortedLocaleList(result);
        setupPreference(result);
    }

    private void setupPreference(List<LocaleStore.LocaleInfo> localeInfoList) {
        localeInfoList.stream().forEach(locale -> {
            SelectorWithWidgetPreference pref = new SelectorWithWidgetPreference(mContext);
            mPreferenceCategory.addPreference(pref);
            pref.setTitle(locale.getFullCountryNameNative());
            pref.setKey(locale.toString());
            if (locale.getLocale().equals(Locale.getDefault())) {
                pref.setChecked(true);
            } else {
                pref.setChecked(false);
            }
            pref.setOnClickListener(v -> {
                switchRegion(locale);
            });
        });
        mPreferenceCategory.setVisible(mPreferenceCategory.getPreferenceCount() > 0);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    protected abstract String getPreferenceCategoryKey();

    protected abstract LocaleCollectorBase getLocaleCollectorController(Context context);

    @Nullable protected abstract LocaleStore.LocaleInfo getParentLocale();

    protected List<LocaleStore.LocaleInfo> getSuggestedLocaleList() {
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
                new LocaleHelper.LocaleInfoComparator(sortingLocale, true);
        Collections.sort(localeInfos, comp);
        return localeInfos;
    }

    private void switchRegion(LocaleStore.LocaleInfo localeInfo) {
        if (localeInfo.getLocale().equals(Locale.getDefault())) {
            return;
        }

        mFragmentManager = mParent.getChildFragmentManager();
        Bundle args = new Bundle();
        args.putInt(RegionDialogFragment.ARG_DIALOG_TYPE,
                RegionDialogFragment.DIALOG_CHANGE_LOCALE_REGION);
        args.putSerializable(RegionDialogFragment.ARG_TARGET_LOCALE, localeInfo);
        RegionDialogFragment regionDialogFragment = RegionDialogFragment.newInstance();
        regionDialogFragment.setArguments(args);
        regionDialogFragment.show(mFragmentManager, TAG_DIALOG_CHANGE_REGION);
    }
}
