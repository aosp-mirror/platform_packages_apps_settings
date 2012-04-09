/*
 * Copyright (C) 2008 The Android Open Source Project
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

import com.android.settings.R;
import com.android.settings.Settings.KeyboardLayoutPickerActivity;
import com.android.settings.Settings.SpellCheckersSettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.VoiceInputOutputSettings;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.hardware.input.InputManager;
import android.hardware.input.KeyboardLayout;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.view.InputDevice;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class InputMethodAndLanguageSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener{

    private static final String KEY_PHONE_LANGUAGE = "phone_language";
    private static final String KEY_CURRENT_INPUT_METHOD = "current_input_method";
    private static final String KEY_INPUT_METHOD_SELECTOR = "input_method_selector";
    private static final String KEY_USER_DICTIONARY_SETTINGS = "key_user_dictionary_settings";
    // false: on ICS or later
    private static final boolean SHOW_INPUT_METHOD_SWITCHER_SETTINGS = false;

    private static final String[] sSystemSettingNames = {
        System.TEXT_AUTO_REPLACE, System.TEXT_AUTO_CAPS, System.TEXT_AUTO_PUNCTUATE,
    };

    private static final String[] sHardKeyboardKeys = {
        "auto_replace", "auto_caps", "auto_punctuate",
    };

    private int mDefaultInputMethodSelectorVisibility = 0;
    private ListPreference mShowInputMethodSelectorPref;
    private PreferenceCategory mKeyboardSettingsCategory;
    private PreferenceCategory mHardKeyboardCategory;
    private Preference mLanguagePref;
    private final ArrayList<InputMethodPreference> mInputMethodPreferenceList =
            new ArrayList<InputMethodPreference>();
    private final ArrayList<PreferenceScreen> mHardKeyboardPreferenceList =
            new ArrayList<PreferenceScreen>();
    private InputMethodManager mImm;
    private List<InputMethodInfo> mImis;
    private boolean mIsOnlyImeSettings;
    private Handler mHandler;
    @SuppressWarnings("unused")
    private SettingsObserver mSettingsObserver;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.language_settings);

        try {
            mDefaultInputMethodSelectorVisibility = Integer.valueOf(
                    getString(R.string.input_method_selector_visibility_default_value));
        } catch (NumberFormatException e) {
        }

        if (getActivity().getAssets().getLocales().length == 1) {
            // No "Select language" pref if there's only one system locale available.
            getPreferenceScreen().removePreference(findPreference(KEY_PHONE_LANGUAGE));
        } else {
            mLanguagePref = findPreference(KEY_PHONE_LANGUAGE);
        }
        if (SHOW_INPUT_METHOD_SWITCHER_SETTINGS) {
            mShowInputMethodSelectorPref = (ListPreference)findPreference(
                    KEY_INPUT_METHOD_SELECTOR);
            mShowInputMethodSelectorPref.setOnPreferenceChangeListener(this);
            // TODO: Update current input method name on summary
            updateInputMethodSelectorSummary(loadInputMethodSelectorVisibility());
        }

        new VoiceInputOutputSettings(this).onCreate();

        // Get references to dynamically constructed categories.
        mHardKeyboardCategory = (PreferenceCategory)findPreference("hard_keyboard");
        mKeyboardSettingsCategory = (PreferenceCategory)findPreference(
                "keyboard_settings_category");

        // Filter out irrelevant features if invoked from IME settings button.
        mIsOnlyImeSettings = Settings.ACTION_INPUT_METHOD_SETTINGS.equals(
                getActivity().getIntent().getAction());
        getActivity().getIntent().setAction(null);
        if (mIsOnlyImeSettings) {
            getPreferenceScreen().removeAll();
            getPreferenceScreen().addPreference(mHardKeyboardCategory);
            if (SHOW_INPUT_METHOD_SWITCHER_SETTINGS) {
                getPreferenceScreen().addPreference(mShowInputMethodSelectorPref);
            }
            getPreferenceScreen().addPreference(mKeyboardSettingsCategory);
        }

        // Build IME preference category.
        mImm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mImis = mImm.getInputMethodList();

        mKeyboardSettingsCategory.removeAll();
        if (!mIsOnlyImeSettings) {
            final PreferenceScreen currentIme = new PreferenceScreen(getActivity(), null);
            currentIme.setKey(KEY_CURRENT_INPUT_METHOD);
            currentIme.setTitle(getResources().getString(R.string.current_input_method));
            mKeyboardSettingsCategory.addPreference(currentIme);
        }

        mInputMethodPreferenceList.clear();
        final int N = (mImis == null ? 0 : mImis.size());
        for (int i = 0; i < N; ++i) {
            final InputMethodInfo imi = mImis.get(i);
            final InputMethodPreference pref = getInputMethodPreference(imi, N);
            mInputMethodPreferenceList.add(pref);
        }

        if (!mInputMethodPreferenceList.isEmpty()) {
            Collections.sort(mInputMethodPreferenceList);
            for (int i = 0; i < N; ++i) {
                mKeyboardSettingsCategory.addPreference(mInputMethodPreferenceList.get(i));
            }
        }

        // Build hard keyboard preference category.
        updateHardKeyboards();

        // Spell Checker
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClass(getActivity(), SpellCheckersSettingsActivity.class);
        final SpellCheckersPreference scp = ((SpellCheckersPreference)findPreference(
                "spellcheckers_settings"));
        if (scp != null) {
            scp.setFragmentIntent(this, intent);
        }

        mHandler = new Handler();
        mSettingsObserver = new SettingsObserver(mHandler, getActivity());
    }

    private void updateInputMethodSelectorSummary(int value) {
        String[] inputMethodSelectorTitles = getResources().getStringArray(
                R.array.input_method_selector_titles);
        if (inputMethodSelectorTitles.length > value) {
            mShowInputMethodSelectorPref.setSummary(inputMethodSelectorTitles[value]);
            mShowInputMethodSelectorPref.setValue(String.valueOf(value));
        }
    }

    private void updateUserDictionaryPreference(Preference userDictionaryPreference) {
        final Activity activity = getActivity();
        final Set<String> localeList = UserDictionaryList.getUserDictionaryLocalesList(activity);
        if (null == localeList) {
            // The locale list is null if and only if the user dictionary service is
            // not present or disabled. In this case we need to remove the preference.
            getPreferenceScreen().removePreference(userDictionaryPreference);
        } else if (localeList.size() <= 1) {
            final Intent intent =
                    new Intent(UserDictionaryList.USER_DICTIONARY_SETTINGS_INTENT_ACTION);
            userDictionaryPreference.setTitle(R.string.user_dict_single_settings_title);
            userDictionaryPreference.setIntent(intent);
            // If the size of localeList is 0, we don't set the locale parameter in the
            // extras. This will be interpreted by the UserDictionarySettings class as
            // meaning "the current locale".
            // Note that with the current code for UserDictionaryList#getUserDictionaryLocalesList()
            // the locale list always has at least one element, since it always includes the current
            // locale explicitly. @see UserDictionaryList.getUserDictionaryLocalesList().
            if (localeList.size() == 1) {
                final String locale = (String)localeList.toArray()[0];
                userDictionaryPreference.getExtras().putString("locale", locale);
            }
        } else {
            userDictionaryPreference.setTitle(R.string.user_dict_multiple_settings_title);
            userDictionaryPreference.setFragment(UserDictionaryList.class.getName());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mIsOnlyImeSettings) {
            if (mLanguagePref != null) {
                Configuration conf = getResources().getConfiguration();
                String locale = conf.locale.getDisplayName(conf.locale);
                if (locale != null && locale.length() > 1) {
                    locale = Character.toUpperCase(locale.charAt(0)) + locale.substring(1);
                    mLanguagePref.setSummary(locale);
                }
            }

            updateUserDictionaryPreference(findPreference(KEY_USER_DICTIONARY_SETTINGS));
            if (SHOW_INPUT_METHOD_SWITCHER_SETTINGS) {
                mShowInputMethodSelectorPref.setOnPreferenceChangeListener(this);
            }
        }

        // Hard keyboard
        if (!mHardKeyboardPreferenceList.isEmpty()) {
            for (int i = 0; i < sHardKeyboardKeys.length; ++i) {
                CheckBoxPreference chkPref = (CheckBoxPreference)
                        mHardKeyboardCategory.findPreference(sHardKeyboardKeys[i]);
                chkPref.setChecked(
                        System.getInt(getContentResolver(), sSystemSettingNames[i], 1) > 0);
            }
        }

        updateHardKeyboards();

        // IME
        InputMethodAndSubtypeUtil.loadInputMethodSubtypeList(
                this, getContentResolver(), mImis, null);
        updateActiveInputMethodsSummary();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (SHOW_INPUT_METHOD_SWITCHER_SETTINGS) {
            mShowInputMethodSelectorPref.setOnPreferenceChangeListener(null);
        }
        InputMethodAndSubtypeUtil.saveInputMethodSubtypeList(
                this, getContentResolver(), mImis, !mHardKeyboardPreferenceList.isEmpty());
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        // Input Method stuff
        if (Utils.isMonkeyRunning()) {
            return false;
        }
        if (preference instanceof PreferenceScreen) {
            if (preference.getFragment() != null) {
                // Fragment will be handled correctly by the super class.
            } else if (KEY_CURRENT_INPUT_METHOD.equals(preference.getKey())) {
                final InputMethodManager imm = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showInputMethodPicker();
            }
        } else if (preference instanceof CheckBoxPreference) {
            final CheckBoxPreference chkPref = (CheckBoxPreference) preference;
            if (!mHardKeyboardPreferenceList.isEmpty()) {
                for (int i = 0; i < sHardKeyboardKeys.length; ++i) {
                    if (chkPref == mHardKeyboardCategory.findPreference(sHardKeyboardKeys[i])) {
                        System.putInt(getContentResolver(), sSystemSettingNames[i],
                                chkPref.isChecked() ? 1 : 0);
                        return true;
                    }
                }
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void saveInputMethodSelectorVisibility(String value) {
        try {
            int intValue = Integer.valueOf(value);
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.INPUT_METHOD_SELECTOR_VISIBILITY, intValue);
            updateInputMethodSelectorSummary(intValue);
        } catch(NumberFormatException e) {
        }
    }

    private int loadInputMethodSelectorVisibility() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.INPUT_METHOD_SELECTOR_VISIBILITY,
                mDefaultInputMethodSelectorVisibility);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (SHOW_INPUT_METHOD_SWITCHER_SETTINGS) {
            if (preference == mShowInputMethodSelectorPref) {
                if (value instanceof String) {
                    saveInputMethodSelectorVisibility((String)value);
                }
            }
        }
        return false;
    }

    private void updateActiveInputMethodsSummary() {
        for (Preference pref : mInputMethodPreferenceList) {
            if (pref instanceof InputMethodPreference) {
                ((InputMethodPreference)pref).updateSummary();
            }
        }
        updateCurrentImeName();
    }

    private void updateCurrentImeName() {
        final Context context = getActivity();
        if (context == null || mImm == null) return;
        final Preference curPref = getPreferenceScreen().findPreference(KEY_CURRENT_INPUT_METHOD);
        if (curPref != null) {
            final CharSequence curIme = InputMethodAndSubtypeUtil.getCurrentInputMethodName(
                    context, getContentResolver(), mImm, mImis, getPackageManager());
            if (!TextUtils.isEmpty(curIme)) {
                synchronized(this) {
                    curPref.setSummary(curIme);
                }
            }
        }
    }

    private InputMethodPreference getInputMethodPreference(InputMethodInfo imi, int imiSize) {
        final PackageManager pm = getPackageManager();
        final CharSequence label = imi.loadLabel(pm);
        // IME settings
        final Intent intent;
        final String settingsActivity = imi.getSettingsActivity();
        if (!TextUtils.isEmpty(settingsActivity)) {
            intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName(imi.getPackageName(), settingsActivity);
        } else {
            intent = null;
        }

        // Add a check box for enabling/disabling IME
        InputMethodPreference pref = new InputMethodPreference(this, intent, mImm, imi, imiSize);
        pref.setKey(imi.getId());
        pref.setTitle(label);
        return pref;
    }

    private void updateHardKeyboards() {
        mHardKeyboardPreferenceList.clear();
        if (getResources().getConfiguration().keyboard == Configuration.KEYBOARD_QWERTY) {
            final InputManager im =
                    (InputManager)getActivity().getSystemService(Context.INPUT_SERVICE);

            final int[] devices = InputDevice.getDeviceIds();
            for (int i = 0; i < devices.length; i++) {
                InputDevice device = InputDevice.getDevice(devices[i]);
                if (device != null
                        && !device.isVirtual()
                        && (device.getSources() & InputDevice.SOURCE_KEYBOARD) != 0
                        && device.getKeyboardType() == InputDevice.KEYBOARD_TYPE_ALPHABETIC) {
                    final String inputDeviceDescriptor = device.getDescriptor();
                    final String keyboardLayoutDescriptor =
                            im.getKeyboardLayoutForInputDevice(inputDeviceDescriptor);
                    final KeyboardLayout keyboardLayout = keyboardLayoutDescriptor != null ?
                            im.getKeyboardLayout(keyboardLayoutDescriptor) : null;

                    final Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setClass(getActivity(), KeyboardLayoutPickerActivity.class);
                    intent.putExtra(KeyboardLayoutPicker.EXTRA_INPUT_DEVICE_DESCRIPTOR,
                            inputDeviceDescriptor);

                    final PreferenceScreen pref = new PreferenceScreen(getActivity(), null);
                    pref.setTitle(device.getName());
                    if (keyboardLayout != null) {
                        pref.setSummary(keyboardLayout.getLabel());
                    }
                    pref.setIntent(intent);
                    mHardKeyboardPreferenceList.add(pref);
                }
            }
        }

        if (!mHardKeyboardPreferenceList.isEmpty()) {
            for (int i = mHardKeyboardCategory.getPreferenceCount(); i-- > 0; ) {
                final Preference pref = mHardKeyboardCategory.getPreference(i);
                if (pref.getOrder() < 1000) {
                    mHardKeyboardCategory.removePreference(pref);
                }
            }

            Collections.sort(mHardKeyboardPreferenceList);
            final int count = mHardKeyboardPreferenceList.size();
            for (int i = 0; i < count; i++) {
                final Preference pref = mHardKeyboardPreferenceList.get(i);
                pref.setOrder(i);
                mHardKeyboardCategory.addPreference(pref);
            }
        } else {
            getPreferenceScreen().removePreference(mHardKeyboardCategory);
        }
    }

    private class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler, Context context) {
            super(handler);
            final ContentResolver cr = context.getContentResolver();
            cr.registerContentObserver(
                    Settings.Secure.getUriFor(Settings.Secure.DEFAULT_INPUT_METHOD), false, this);
            cr.registerContentObserver(Settings.Secure.getUriFor(
                    Settings.Secure.SELECTED_INPUT_METHOD_SUBTYPE), false, this);
        }

        @Override public void onChange(boolean selfChange) {
            updateCurrentImeName();
        }
    }
}
