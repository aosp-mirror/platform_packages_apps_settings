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

package com.android.settings.inputmethod;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.UserDictionary;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;

import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

public class UserDictionaryListPreferenceController extends BasePreferenceController implements
        LifecycleObserver, OnStart {

    public static final String USER_DICTIONARY_SETTINGS_INTENT_ACTION =
            "android.settings.USER_DICTIONARY_SETTINGS";
    private final String KEY_ALL_LANGUAGE = "all_languages";
    private String mLocale;
    private PreferenceScreen mScreen;

    public UserDictionaryListPreferenceController(Context context, String key) {
        super(context, key);
    }

    public void setLocale(String locale) {
        mLocale = locale;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        // This is to make newly inserted languages being sorted alphabetically when updating
        // the existing preferenceScreen, and for "For all languages" to be always on the top.
        screen.setOrderingAsAdded(false);
        mScreen = screen;
    }

    @Override
    public void onStart() {
        createUserDictSettings();
    }

    @NonNull
    public static TreeSet<String> getUserDictionaryLocalesSet(Context context) {
        final Cursor cursor = context.getContentResolver().query(
                UserDictionary.Words.CONTENT_URI, new String[]{UserDictionary.Words.LOCALE},
                null, null, null);
        final TreeSet<String> localeSet = new TreeSet<>();
        if (cursor == null) {
            // The user dictionary service is not present or disabled. Return empty set.
            return localeSet;
        }
        try {
            if (cursor.moveToFirst()) {
                final int columnIndex = cursor.getColumnIndex(UserDictionary.Words.LOCALE);
                do {
                    final String locale = cursor.getString(columnIndex);
                    localeSet.add(null != locale ? locale : "");
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }

        // CAVEAT: Keep this for consistency of the implementation between Keyboard and Settings
        // if (!UserDictionarySettings.IS_SHORTCUT_API_SUPPORTED) {
        //     // For ICS, we need to show "For all languages" in case that the keyboard locale
        //     // is different from the system locale
        //     localeSet.add("");
        // }

        final InputMethodManager imm =
                (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        final List<InputMethodInfo> imis = imm.getEnabledInputMethodList();
        for (final InputMethodInfo imi : imis) {
            final List<InputMethodSubtype> subtypes =
                    imm.getEnabledInputMethodSubtypeList(
                            imi, true /* allowsImplicitlySelectedSubtypes */);
            for (InputMethodSubtype subtype : subtypes) {
                final String locale = subtype.getLocale();
                if (!TextUtils.isEmpty(locale)) {
                    localeSet.add(locale);
                }
            }
        }

        // We come here after we have collected locales from existing user dictionary entries and
        // enabled subtypes. If we already have the locale-without-country version of the system
        // locale, we don't add the system locale to avoid confusion even though it's technically
        // correct to add it.
        if (!localeSet.contains(Locale.getDefault().getLanguage())) {
            localeSet.add(Locale.getDefault().toString());
        }

        return localeSet;
    }

    @VisibleForTesting
    TreeSet<String> getUserDictLocalesSet(Context context) {
        return getUserDictionaryLocalesSet(context);
    }

    /**
     * Creates the entries that allow the user to go into the user dictionary for each locale.
     */
    private void createUserDictSettings() {
        final TreeSet<String> localeSet = getUserDictLocalesSet(mContext);
        final int prefCount = mScreen.getPreferenceCount();
        String prefKey;

        if (mLocale != null) {
            // If the caller explicitly specify empty string as a locale, we'll show "all languages"
            // in the list.
            localeSet.add(mLocale);
        }
        if (localeSet.size() > 1) {
            // Have an "All languages" entry in the languages list if there are two or more active
            // languages
            localeSet.add("");
        }

        // Update the existing preferenceScreen according to the corresponding data set.
        if (prefCount > 0) {
            for (int i = prefCount - 1; i >= 0; i--) {
                prefKey = mScreen.getPreference(i).getKey();
                if (TextUtils.isEmpty(prefKey) || TextUtils.equals(KEY_ALL_LANGUAGE, prefKey)) {
                    continue;
                }
                if (!localeSet.isEmpty() && localeSet.contains(prefKey)) {
                    localeSet.remove(prefKey);
                    continue;
                }
                mScreen.removePreference(mScreen.findPreference(prefKey));
            }
        }

        if (localeSet.isEmpty() && prefCount == 0) {
            mScreen.addPreference(createUserDictionaryPreference(null));
        } else {
            for (String locale : localeSet) {
                final Preference pref = createUserDictionaryPreference(locale);
                if (mScreen.findPreference(pref.getKey()) == null) {
                    mScreen.addPreference(pref);
                }
            }
        }
    }

    /**
     * Create a single User Dictionary Preference object, with its parameters set.
     *
     * @param locale The locale for which this user dictionary is for.
     * @return The corresponding preference.
     */
    private Preference createUserDictionaryPreference(String locale) {
        final String KEY_LOCALE = "locale";
        final Preference newPref = new Preference(mScreen.getContext());
        final Intent intent = new Intent(USER_DICTIONARY_SETTINGS_INTENT_ACTION)
                .setPackage(mContext.getPackageName());
        if (locale == null) {
            newPref.setTitle(Locale.getDefault().getDisplayName());
            newPref.setKey(Locale.getDefault().toString());
        } else {
            if (TextUtils.isEmpty(locale)) {
                newPref.setTitle(mContext.getString(R.string.user_dict_settings_all_languages));
                newPref.setKey(KEY_ALL_LANGUAGE);
                newPref.setOrder(0);
            } else {
                newPref.setTitle(Utils.createLocaleFromString(locale).getDisplayName());
                newPref.setKey(locale);
            }
            intent.putExtra(KEY_LOCALE, locale);
            newPref.getExtras().putString(KEY_LOCALE, locale);
        }
        newPref.setIntent(intent);
        newPref.setFragment(UserDictionarySettings.class.getName());
        return newPref;
    }
}
