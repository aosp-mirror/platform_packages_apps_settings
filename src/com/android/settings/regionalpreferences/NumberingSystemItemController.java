/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.icu.text.NumberingSystem;
import android.icu.util.ULocale;
import android.os.Bundle;
import android.os.LocaleList;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.app.LocaleHelper;
import com.android.internal.app.LocalePicker;
import com.android.internal.app.LocaleStore;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.TickButtonPreference;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.Locale;

/** Uses to control the preference UI of numbering system page. */
public class NumberingSystemItemController extends BasePreferenceController {
    private static final String TAG = NumberingSystemItemController.class.getSimpleName();
    private static final String DISPLAY_KEYWORD_NUMBERING_SYSTEM = "numbers";

    static final String ARG_VALUE_NUMBERING_SYSTEM_SELECT = "arg_value_numbering_system_select";
    static final String ARG_VALUE_LANGUAGE_SELECT = "arg_value_language_select";
    static final String KEY_SELECTED_LANGUAGE = "key_selected_language";

    private final MetricsFeatureProvider mMetricsFeatureProvider;

    private String mOption = "";
    private String mSelectedLanguage = "";
    private DashboardFragment mParentFragment;
    private PreferenceScreen mPreferenceScreen;

    public NumberingSystemItemController(Context context, Bundle argument) {
        super(context, "no_key");
        // Initialize the supported languages to LocaleInfos
        LocaleStore.fillCache(context);
        mOption = argument.getString(
                RegionalPreferencesEntriesFragment.ARG_KEY_REGIONAL_PREFERENCE, "");
        mSelectedLanguage = argument.getString(
                NumberingSystemItemController.KEY_SELECTED_LANGUAGE, "");
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    /**
     * Displays preference in this controller.
     */
    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceScreen = screen;
        if (mOption.equals(ARG_VALUE_LANGUAGE_SELECT)) {
            initLanguageOptionsUi(screen);
        } else if (mOption.equals(ARG_VALUE_NUMBERING_SYSTEM_SELECT)) {
            initNumberingSystemOptionsUi(screen, Locale.forLanguageTag(mSelectedLanguage));
        }
    }

    /**
     * Sets the parent fragment and attaches this controller to the settings lifecycle.
     *
     * @param fragment the fragment to use as the parent
     */
    public void setParentFragment(DashboardFragment fragment) {
        mParentFragment = fragment;
    }

    /**
     * @return {@link AvailabilityStatus} for the Setting. This status is used to determine if the
     * Setting should be shown or disabled in Settings. Further, it can be used to produce
     * appropriate error / warning Slice in the case of unavailability.
     * </p>
     * The status is used for the convenience methods: {@link #isAvailable()}, {@link
     * #isSupported()}
     * </p>
     * The inherited class doesn't need to check work profile if android:forWork="true" is set in
     * preference xml.
     */
    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (mOption.equals(ARG_VALUE_LANGUAGE_SELECT)) {
            handleLanguageSelect(preference);
        } else if (mOption.equals(ARG_VALUE_NUMBERING_SYSTEM_SELECT)) {
            handleNumberSystemSelect(preference);
        }
        return true;
    }

    private void initLanguageOptionsUi(PreferenceScreen screen) {
        // Get current system language list to show on screen.
        LocaleList localeList = LocaleList.getDefault();
        for (int i = 0; i < localeList.size(); i++) {
            Locale locale = localeList.get(i);
            LocaleStore.LocaleInfo localeInfo = LocaleStore.getLocaleInfo(locale);
            if (!localeInfo.hasNumberingSystems()) {
                continue;
            }
            Preference pref = new Preference(mContext);
            pref.setTitle(LocaleHelper.getDisplayName(locale.stripExtensions(), locale, true));
            pref.setKey(locale.toLanguageTag());
            pref.setSummary(getNumberingSystem(locale));
            screen.addPreference(pref);
        }
    }

    private void initNumberingSystemOptionsUi(PreferenceScreen screen, Locale targetLocale) {
        String[] locales = LocalePicker.getSupportedLocales(mContext);
        for (String localeTag : locales) {
            Locale supportedLocale = Locale.forLanguageTag(localeTag);
            if (isSameBaseLocale(targetLocale, supportedLocale)) {
                TickButtonPreference pref = new TickButtonPreference(mContext);
                String numberingName = getNumberingSystem(supportedLocale);
                pref.setTitle(numberingName);
                String key = supportedLocale.getUnicodeLocaleType(
                        ExtensionTypes.NUMBERING_SYSTEM);
                pref.setKey(key == null ? RegionalPreferencesDataUtils.DEFAULT_VALUE : key);
                pref.setSelected(isSameNumberingSystem(targetLocale, supportedLocale));
                screen.addPreference(pref);
            }
        }
    }

    private void handleLanguageSelect(Preference preference) {
        String selectedLanguage = preference.getKey();
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_CHOOSE_LANGUAGE_FOR_NUMBERS_PREFERENCES);
        final Bundle extra = new Bundle();
        extra.putString(RegionalPreferencesEntriesFragment.ARG_KEY_REGIONAL_PREFERENCE,
                ARG_VALUE_NUMBERING_SYSTEM_SELECT);
        extra.putString(KEY_SELECTED_LANGUAGE, selectedLanguage);
        new SubSettingLauncher(preference.getContext())
                .setDestination(NumberingPreferencesFragment.class.getName())
                .setSourceMetricsCategory(
                        SettingsEnums.NUMBERING_SYSTEM_LANGUAGE_SELECTION_PREFERENCE)
                .setArguments(extra)
                .launch();
    }

    private void handleNumberSystemSelect(Preference preference) {
        for (int i = 0; i < mPreferenceScreen.getPreferenceCount(); i++) {
            TickButtonPreference pref = (TickButtonPreference) mPreferenceScreen.getPreference(i);
            Log.i(TAG, "[onPreferenceClick] key is " + pref.getKey());
            if (pref.getKey().equals(preference.getKey())) {
                String numberingSystem = pref.getKey();
                pref.setSelected(true);
                Locale updatedLocale =
                        saveNumberingSystemToLocale(Locale.forLanguageTag(mSelectedLanguage),
                                numberingSystem);
                mMetricsFeatureProvider.action(mContext,
                        SettingsEnums.ACTION_SET_NUMBERS_PREFERENCES);
                // After updated locale to framework, this fragment will recreate,
                // so it needs to update the argument of selected language.
                Bundle bundle = new Bundle();
                bundle.putString(RegionalPreferencesEntriesFragment.ARG_KEY_REGIONAL_PREFERENCE,
                        ARG_VALUE_NUMBERING_SYSTEM_SELECT);
                bundle.putString(KEY_SELECTED_LANGUAGE,
                        updatedLocale != null ? updatedLocale.toLanguageTag() : "");
                mParentFragment.setArguments(bundle);
                continue;
            }
            pref.setSelected(false);
        }
    }

    private Locale saveNumberingSystemToLocale(Locale targetLocale, String value) {
        LocaleList localeList = LocalePicker.getLocales();
        Locale[] locales = new Locale[localeList.size()];
        Locale updatedLocale = null;
        for (int i = 0; i < localeList.size(); i++) {
            Locale locale = localeList.get(i);
            if (targetLocale.equals(locale)) {
                if (RegionalPreferencesDataUtils.DEFAULT_VALUE.equals(value)) {
                    value = null;
                }
                updatedLocale = new Locale.Builder()
                        .setLocale(locale)
                        .setUnicodeLocaleKeyword(ExtensionTypes.NUMBERING_SYSTEM, value)
                        .build();
                locales[i] = updatedLocale;
                continue;
            }
            locales[i] = localeList.get(i);
        }
        LocalePicker.updateLocales(new LocaleList(locales));
        return updatedLocale;
    }

    private static String getNumberingSystem(Locale locale) {
        ULocale uLocale = new ULocale.Builder()
                .setUnicodeLocaleKeyword(ExtensionTypes.NUMBERING_SYSTEM,
                        NumberingSystem.getInstance(locale).getName())
                .build();
        return uLocale.getDisplayKeywordValue(DISPLAY_KEYWORD_NUMBERING_SYSTEM,
                ULocale.forLocale(locale));
    }

    private static boolean isSameNumberingSystem(Locale locale1, Locale locale2) {
        String name1 = NumberingSystem.getInstance(locale1).getName();
        String name2 = NumberingSystem.getInstance(locale2).getName();
        return TextUtils.equals(name1, name2);
    }

    private static boolean isSameBaseLocale(Locale locale1, Locale locale2) {
        return locale1.stripExtensions().equals(locale2.stripExtensions());
    }
}
