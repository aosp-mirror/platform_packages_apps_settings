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
import com.android.settings.UserDictionarySettings;
import com.android.settings.Utils;
import com.android.settings.VoiceInputOutputSettings;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.input.InputManager;
import android.hardware.input.KeyboardLayout;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.view.InputDevice;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

public class InputMethodAndLanguageSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener, InputManager.InputDeviceListener,
        KeyboardLayoutDialogFragment.OnSetupKeyboardLayoutsListener {

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
    private PreferenceCategory mGameControllerCategory;
    private Preference mLanguagePref;
    private final ArrayList<InputMethodPreference> mInputMethodPreferenceList =
            new ArrayList<InputMethodPreference>();
    private final ArrayList<PreferenceScreen> mHardKeyboardPreferenceList =
            new ArrayList<PreferenceScreen>();
    private InputManager mIm;
    private InputMethodManager mImm;
    private boolean mIsOnlyImeSettings;
    private Handler mHandler;
    private SettingsObserver mSettingsObserver;
    private Intent mIntentWaitingForResult;
    private InputMethodSettingValuesWrapper mInputMethodSettingValues;

    private final OnPreferenceChangeListener mOnImePreferenceChangedListener =
            new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference arg0, Object arg1) {
                    InputMethodSettingValuesWrapper.getInstance(
                            arg0.getContext()).refreshAllInputMethodAndSubtypes();
                    ((BaseAdapter)getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();
                    updateInputMethodPreferenceViews();
                    return true;
                }
            };

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
        mGameControllerCategory = (PreferenceCategory)findPreference(
                "game_controller_settings_category");

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
        mInputMethodSettingValues = InputMethodSettingValuesWrapper.getInstance(getActivity());

        mKeyboardSettingsCategory.removeAll();
        if (!mIsOnlyImeSettings) {
            final PreferenceScreen currentIme = new PreferenceScreen(getActivity(), null);
            currentIme.setKey(KEY_CURRENT_INPUT_METHOD);
            currentIme.setTitle(getResources().getString(R.string.current_input_method));
            mKeyboardSettingsCategory.addPreference(currentIme);
        }

        // Build hard keyboard and game controller preference categories.
        mIm = (InputManager)getActivity().getSystemService(Context.INPUT_SERVICE);
        updateInputDevices();

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
        final TreeSet<String> localeSet = UserDictionaryList.getUserDictionaryLocalesSet(activity);
        if (null == localeSet) {
            // The locale list is null if and only if the user dictionary service is
            // not present or disabled. In this case we need to remove the preference.
            getPreferenceScreen().removePreference(userDictionaryPreference);
        } else {
            userDictionaryPreference.setOnPreferenceClickListener(
                    new OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference arg0) {
                            // Redirect to UserDictionarySettings if the user needs only one
                            // language.
                            final Bundle extras = new Bundle();
                            final Class<? extends Fragment> targetFragment;
                            if (localeSet.size() <= 1) {
                                if (!localeSet.isEmpty()) {
                                    // If the size of localeList is 0, we don't set the locale
                                    // parameter in the extras. This will be interpreted by the
                                    // UserDictionarySettings class as meaning
                                    // "the current locale". Note that with the current code for
                                    // UserDictionaryList#getUserDictionaryLocalesSet()
                                    // the locale list always has at least one element, since it
                                    // always includes the current locale explicitly.
                                    // @see UserDictionaryList.getUserDictionaryLocalesSet().
                                    extras.putString("locale", localeSet.first());
                                }
                                targetFragment = UserDictionarySettings.class;
                            } else {
                                targetFragment = UserDictionaryList.class;
                            }
                            startFragment(InputMethodAndLanguageSettings.this,
                                    targetFragment.getCanonicalName(), -1, extras);
                            return true;
                        }
                    });
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mSettingsObserver.resume();
        mIm.registerInputDeviceListener(this, null);

        if (!mIsOnlyImeSettings) {
            if (mLanguagePref != null) {
                Configuration conf = getResources().getConfiguration();
                String language = conf.locale.getLanguage();
                String localeString;
                // TODO: This is not an accurate way to display the locale, as it is
                // just working around the fact that we support limited dialects
                // and want to pretend that the language is valid for all locales.
                // We need a way to support languages that aren't tied to a particular
                // locale instead of hiding the locale qualifier.
                if (language.equals("zz")) {
                    String country = conf.locale.getCountry();
                    if (country.equals("ZZ")) {
                        localeString = "[Developer] Accented English (zz_ZZ)";
                    } else if (country.equals("ZY")) {
                        localeString = "[Developer] Fake Bi-Directional (zz_ZY)";
                    } else {
                        localeString = "";
                    }
                } else if (hasOnlyOneLanguageInstance(language,
                        Resources.getSystem().getAssets().getLocales())) {
                    localeString = conf.locale.getDisplayLanguage(conf.locale);
                } else {
                    localeString = conf.locale.getDisplayName(conf.locale);
                }
                if (localeString.length() > 1) {
                    localeString = Character.toUpperCase(localeString.charAt(0))
                            + localeString.substring(1);
                    mLanguagePref.setSummary(localeString);
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

        updateInputDevices();

        // Refresh internal states in mInputMethodSettingValues to keep the latest
        // "InputMethodInfo"s and "InputMethodSubtype"s
        mInputMethodSettingValues.refreshAllInputMethodAndSubtypes();
        updateInputMethodPreferenceViews();
    }

    @Override
    public void onPause() {
        super.onPause();

        mIm.unregisterInputDeviceListener(this);
        mSettingsObserver.pause();

        if (SHOW_INPUT_METHOD_SWITCHER_SETTINGS) {
            mShowInputMethodSelectorPref.setOnPreferenceChangeListener(null);
        }
        // TODO: Consolidate the logic to InputMethodSettingsWrapper
        InputMethodAndSubtypeUtil.saveInputMethodSubtypeList(
                this, getContentResolver(), mInputMethodSettingValues.getInputMethodList(),
                !mHardKeyboardPreferenceList.isEmpty());
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        updateInputDevices();
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        updateInputDevices();
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        updateInputDevices();
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
            if (chkPref == mGameControllerCategory.findPreference("vibrate_input_devices")) {
                System.putInt(getContentResolver(), Settings.System.VIBRATE_INPUT_DEVICES,
                        chkPref.isChecked() ? 1 : 0);
                return true;
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private boolean hasOnlyOneLanguageInstance(String languageCode, String[] locales) {
        int count = 0;
        for (String localeCode : locales) {
            if (localeCode.length() > 2
                    && localeCode.startsWith(languageCode)) {
                count++;
                if (count > 1) {
                    return false;
                }
            }
        }
        return count == 1;
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

    private void updateInputMethodPreferenceViews() {
        synchronized (mInputMethodPreferenceList) {
            // Clear existing "InputMethodPreference"s
            for (final InputMethodPreference imp : mInputMethodPreferenceList) {
                mKeyboardSettingsCategory.removePreference(imp);
            }
            mInputMethodPreferenceList.clear();
            final List<InputMethodInfo> imis = mInputMethodSettingValues.getInputMethodList();
            final int N = (imis == null ? 0 : imis.size());
            for (int i = 0; i < N; ++i) {
                final InputMethodInfo imi = imis.get(i);
                final InputMethodPreference pref = getInputMethodPreference(imi);
                pref.setOnImePreferenceChangeListener(mOnImePreferenceChangedListener);
                mInputMethodPreferenceList.add(pref);
            }

            if (!mInputMethodPreferenceList.isEmpty()) {
                Collections.sort(mInputMethodPreferenceList);
                for (int i = 0; i < N; ++i) {
                    mKeyboardSettingsCategory.addPreference(mInputMethodPreferenceList.get(i));
                }
            }

            // update views status
            for (Preference pref : mInputMethodPreferenceList) {
                if (pref instanceof InputMethodPreference) {
                    ((InputMethodPreference) pref).updatePreferenceViews();
                }
            }
        }
        updateCurrentImeName();
        // TODO: Consolidate the logic with InputMethodSettingsWrapper
        // CAVEAT: The preference class here does not know about the default value - that is
        // managed by the Input Method Manager Service, so in this case it could save the wrong
        // value. Hence we must update the checkboxes here.
        InputMethodAndSubtypeUtil.loadInputMethodSubtypeList(
                this, getContentResolver(),
                mInputMethodSettingValues.getInputMethodList(), null);
    }

    private void updateCurrentImeName() {
        final Context context = getActivity();
        if (context == null || mImm == null) return;
        final Preference curPref = getPreferenceScreen().findPreference(KEY_CURRENT_INPUT_METHOD);
        if (curPref != null) {
            final CharSequence curIme =
                    mInputMethodSettingValues.getCurrentInputMethodName(context);
            if (!TextUtils.isEmpty(curIme)) {
                synchronized(this) {
                    curPref.setSummary(curIme);
                }
            }
        }
    }

    private InputMethodPreference getInputMethodPreference(InputMethodInfo imi) {
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
        final InputMethodPreference pref =
                new InputMethodPreference(this, intent, mImm, imi);
        pref.setKey(imi.getId());
        pref.setTitle(label);
        return pref;
    }

    private void updateInputDevices() {
        updateHardKeyboards();
        updateGameControllers();
    }

    private void updateHardKeyboards() {
        mHardKeyboardPreferenceList.clear();
        if (getResources().getConfiguration().keyboard == Configuration.KEYBOARD_QWERTY) {
            final int[] devices = InputDevice.getDeviceIds();
            for (int i = 0; i < devices.length; i++) {
                InputDevice device = InputDevice.getDevice(devices[i]);
                if (device != null
                        && !device.isVirtual()
                        && device.isFullKeyboard()) {
                    final String inputDeviceDescriptor = device.getDescriptor();
                    final String keyboardLayoutDescriptor =
                            mIm.getCurrentKeyboardLayoutForInputDevice(inputDeviceDescriptor);
                    final KeyboardLayout keyboardLayout = keyboardLayoutDescriptor != null ?
                            mIm.getKeyboardLayout(keyboardLayoutDescriptor) : null;

                    final PreferenceScreen pref = new PreferenceScreen(getActivity(), null);
                    pref.setTitle(device.getName());
                    if (keyboardLayout != null) {
                        pref.setSummary(keyboardLayout.toString());
                    } else {
                        pref.setSummary(R.string.keyboard_layout_default_label);
                    }
                    pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            showKeyboardLayoutDialog(inputDeviceDescriptor);
                            return true;
                        }
                    });
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

            getPreferenceScreen().addPreference(mHardKeyboardCategory);
        } else {
            getPreferenceScreen().removePreference(mHardKeyboardCategory);
        }
    }

    private void showKeyboardLayoutDialog(String inputDeviceDescriptor) {
        KeyboardLayoutDialogFragment fragment =
                new KeyboardLayoutDialogFragment(inputDeviceDescriptor);
        fragment.setTargetFragment(this, 0);
        fragment.show(getActivity().getFragmentManager(), "keyboardLayout");
    }

    @Override
    public void onSetupKeyboardLayouts(String inputDeviceDescriptor) {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClass(getActivity(), KeyboardLayoutPickerActivity.class);
        intent.putExtra(KeyboardLayoutPickerFragment.EXTRA_INPUT_DEVICE_DESCRIPTOR,
                inputDeviceDescriptor);
        mIntentWaitingForResult = intent;
        startActivityForResult(intent, 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (mIntentWaitingForResult != null) {
            String inputDeviceDescriptor = mIntentWaitingForResult.getStringExtra(
                    KeyboardLayoutPickerFragment.EXTRA_INPUT_DEVICE_DESCRIPTOR);
            mIntentWaitingForResult = null;
            showKeyboardLayoutDialog(inputDeviceDescriptor);
        }
    }

    private void updateGameControllers() {
        if (haveInputDeviceWithVibrator()) {
            getPreferenceScreen().addPreference(mGameControllerCategory);

            CheckBoxPreference chkPref = (CheckBoxPreference)
                    mGameControllerCategory.findPreference("vibrate_input_devices");
            chkPref.setChecked(System.getInt(getContentResolver(),
                    Settings.System.VIBRATE_INPUT_DEVICES, 1) > 0);
        } else {
            getPreferenceScreen().removePreference(mGameControllerCategory);
        }
    }

    private boolean haveInputDeviceWithVibrator() {
        final int[] devices = InputDevice.getDeviceIds();
        for (int i = 0; i < devices.length; i++) {
            InputDevice device = InputDevice.getDevice(devices[i]);
            if (device != null && !device.isVirtual() && device.getVibrator().hasVibrator()) {
                return true;
            }
        }
        return false;
    }

    private class SettingsObserver extends ContentObserver {
        private Context mContext;

        public SettingsObserver(Handler handler, Context context) {
            super(handler);
            mContext = context;
        }

        @Override public void onChange(boolean selfChange) {
            updateCurrentImeName();
        }

        public void resume() {
            final ContentResolver cr = mContext.getContentResolver();
            cr.registerContentObserver(
                    Settings.Secure.getUriFor(Settings.Secure.DEFAULT_INPUT_METHOD), false, this);
            cr.registerContentObserver(Settings.Secure.getUriFor(
                    Settings.Secure.SELECTED_INPUT_METHOD_SUBTYPE), false, this);
        }

        public void pause() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }
    }
}
