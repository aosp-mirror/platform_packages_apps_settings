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
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.input.InputDeviceIdentifier;
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
import android.speech.RecognitionService;
import android.speech.tts.TtsEngines;
import android.text.TextUtils;
import android.view.InputDevice;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

public class InputMethodAndLanguageSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener, InputManager.InputDeviceListener,
        KeyboardLayoutDialogFragment.OnSetupKeyboardLayoutsListener, Indexable {

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
                String localeName = getLocaleName(getResources());
                mLanguagePref.setSummary(localeName);
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

    private static String getLocaleName(Resources resources) {
        Configuration conf = resources.getConfiguration();
        String language = conf.locale.getLanguage();
        String localeName;
        // TODO: This is not an accurate way to display the locale, as it is
        // just working around the fact that we support limited dialects
        // and want to pretend that the language is valid for all locales.
        // We need a way to support languages that aren't tied to a particular
        // locale instead of hiding the locale qualifier.
        if (language.equals("zz")) {
            String country = conf.locale.getCountry();
            if (country.equals("ZZ")) {
                localeName = "[Developer] Accented English (zz_ZZ)";
            } else if (country.equals("ZY")) {
                localeName = "[Developer] Fake Bi-Directional (zz_ZY)";
            } else {
                localeName = "";
            }
        } else if (hasOnlyOneLanguageInstance(language,
                Resources.getSystem().getAssets().getLocales())) {
            localeName = conf.locale.getDisplayLanguage(conf.locale);
        } else {
            localeName = conf.locale.getDisplayName(conf.locale);
        }

        if (localeName.length() > 1) {
            localeName = Character.toUpperCase(localeName.charAt(0))
                    + localeName.substring(1);
        }

        return localeName;
    }

    private static boolean hasOnlyOneLanguageInstance(String languageCode, String[] locales) {
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
                    final InputDeviceIdentifier identifier = device.getIdentifier();
                    final String keyboardLayoutDescriptor =
                            mIm.getCurrentKeyboardLayoutForInputDevice(identifier);
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
                            showKeyboardLayoutDialog(identifier);
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

    private void showKeyboardLayoutDialog(InputDeviceIdentifier inputDeviceIdentifier) {
        KeyboardLayoutDialogFragment fragment =
                new KeyboardLayoutDialogFragment(inputDeviceIdentifier);
        fragment.setTargetFragment(this, 0);
        fragment.show(getActivity().getFragmentManager(), "keyboardLayout");
    }

    @Override
    public void onSetupKeyboardLayouts(InputDeviceIdentifier inputDeviceIdentifier) {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClass(getActivity(), KeyboardLayoutPickerActivity.class);
        intent.putExtra(KeyboardLayoutPickerFragment.EXTRA_INPUT_DEVICE_IDENTIFIER,
                inputDeviceIdentifier);
        mIntentWaitingForResult = intent;
        startActivityForResult(intent, 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (mIntentWaitingForResult != null) {
            InputDeviceIdentifier inputDeviceIdentifier = mIntentWaitingForResult
                    .getParcelableExtra(KeyboardLayoutPickerFragment.EXTRA_INPUT_DEVICE_IDENTIFIER);
            mIntentWaitingForResult = null;
            showKeyboardLayoutDialog(inputDeviceIdentifier);
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

    private static boolean haveInputDeviceWithVibrator() {
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

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> indexables = new ArrayList<SearchIndexableRaw>();

            Resources resources = context.getResources();
            String screenTitle = context.getString(R.string.language_keyboard_settings_title);

            // Locale picker.
            if (context.getAssets().getLocales().length > 1) {
                String localeName = getLocaleName(resources);
                SearchIndexableRaw indexable = new SearchIndexableRaw(context);
                indexable.title = context.getString(R.string.phone_language);
                indexable.summaryOn = localeName;
                indexable.summaryOff = localeName;
                indexable.screenTitle = screenTitle;
                indexables.add(indexable);
            }

            // Spell checker.
            SearchIndexableRaw indexable = new SearchIndexableRaw(context);
            indexable.title = context.getString(R.string.spellcheckers_settings_title);
            indexable.screenTitle = screenTitle;
            indexables.add(indexable);

            // User dictionary.
            if (UserDictionaryList.getUserDictionaryLocalesSet(context) != null) {
                indexable = new SearchIndexableRaw(context);
                indexable.title = context.getString(R.string.user_dict_settings_title);
                indexable.screenTitle = screenTitle;
                indexables.add(indexable);
            }

            // Keyboard settings.
            indexable = new SearchIndexableRaw(context);
            indexable.title = context.getString(R.string.keyboard_settings_category);
            indexable.screenTitle = screenTitle;
            indexables.add(indexable);

            InputMethodSettingValuesWrapper immValues = InputMethodSettingValuesWrapper
                    .getInstance(context);
            immValues.refreshAllInputMethodAndSubtypes();

            // Current IME.
            String currImeName = immValues.getCurrentInputMethodName(context).toString();
            indexable = new SearchIndexableRaw(context);
            indexable.title = context.getString(R.string.current_input_method);
            indexable.summaryOn = currImeName;
            indexable.summaryOff = currImeName;
            indexable.screenTitle = screenTitle;
            indexables.add(indexable);

            InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(
                    Context.INPUT_METHOD_SERVICE);

            // All other IMEs.
            List<InputMethodInfo> inputMethods = immValues.getInputMethodList();
            final int inputMethodCount = (inputMethods == null ? 0 : inputMethods.size());
            for (int i = 0; i < inputMethodCount; ++i) {
                InputMethodInfo inputMethod = inputMethods.get(i);

                StringBuilder builder = new StringBuilder();
                List<InputMethodSubtype> subtypes = inputMethodManager
                        .getEnabledInputMethodSubtypeList(inputMethod, true);
                final int subtypeCount = subtypes.size();
                for (int j = 0; j < subtypeCount; j++) {
                    InputMethodSubtype subtype = subtypes.get(j);
                    if (builder.length() > 0) {
                        builder.append(',');
                    }
                    CharSequence subtypeLabel = subtype.getDisplayName(context,
                            inputMethod.getPackageName(), inputMethod.getServiceInfo()
                                    .applicationInfo);
                    builder.append(subtypeLabel);
                }
                String summary = builder.toString();

                indexable = new SearchIndexableRaw(context);
                indexable.title = inputMethod.loadLabel(context.getPackageManager()).toString();
                indexable.summaryOn = summary;
                indexable.summaryOff = summary;
                indexable.screenTitle = screenTitle;
                indexables.add(indexable);
            }

            // Hard keyboards
            InputManager inputManager = (InputManager) context.getSystemService(
                    Context.INPUT_SERVICE);
            if (resources.getConfiguration().keyboard == Configuration.KEYBOARD_QWERTY) {
                boolean hasHardKeyboards = false;

                final int[] devices = InputDevice.getDeviceIds();
                for (int i = 0; i < devices.length; i++) {
                    InputDevice device = InputDevice.getDevice(devices[i]);
                    if (device == null || device.isVirtual() || !device.isFullKeyboard()) {
                        continue;
                    }

                    hasHardKeyboards = true;

                    InputDeviceIdentifier identifier = device.getIdentifier();
                    String keyboardLayoutDescriptor =
                            inputManager.getCurrentKeyboardLayoutForInputDevice(identifier);
                    KeyboardLayout keyboardLayout = keyboardLayoutDescriptor != null ?
                            inputManager.getKeyboardLayout(keyboardLayoutDescriptor) : null;

                    String summary;
                    if (keyboardLayout != null) {
                        summary = keyboardLayout.toString();
                    } else {
                        summary = context.getString(R.string.keyboard_layout_default_label);
                    }

                    indexable = new SearchIndexableRaw(context);
                    indexable.title = device.getName();
                    indexable.summaryOn = summary;
                    indexable.summaryOff = summary;
                    indexable.screenTitle = screenTitle;
                    indexables.add(indexable);
                }

                if (hasHardKeyboards) {
                    // Hard keyboard category.
                    indexable = new SearchIndexableRaw(context);
                    indexable.title = context.getString(
                            R.string.builtin_keyboard_settings_title);
                    indexable.screenTitle = screenTitle;
                    indexables.add(indexable);

                    // Auto replace.
                    indexable = new SearchIndexableRaw(context);
                    indexable.title = context.getString(R.string.auto_replace);
                    indexable.summaryOn = context.getString(R.string.auto_replace_summary);
                    indexable.summaryOff = context.getString(R.string.auto_replace_summary);
                    indexable.screenTitle = screenTitle;
                    indexables.add(indexable);

                    // Auto caps.
                    indexable = new SearchIndexableRaw(context);
                    indexable.title = context.getString(R.string.auto_caps);
                    indexable.summaryOn = context.getString(R.string.auto_caps_summary);
                    indexable.summaryOff = context.getString(R.string.auto_caps_summary);
                    indexable.screenTitle = screenTitle;
                    indexables.add(indexable);

                    // Auto punctuate.
                    indexable = new SearchIndexableRaw(context);
                    indexable.title = context.getString(R.string.auto_punctuate);
                    indexable.summaryOn = context.getString(R.string.auto_punctuate_summary);
                    indexable.summaryOff = context.getString(R.string.auto_punctuate_summary);
                    indexable.screenTitle = screenTitle;
                    indexables.add(indexable);
                }
            }

            // Voice recognizers.
            List<ResolveInfo> recognizers = context.getPackageManager()
                    .queryIntentServices(new Intent(RecognitionService.SERVICE_INTERFACE),
                            PackageManager.GET_META_DATA);

            final int recognizerCount = recognizers.size();

            // Recognizer settings.
            if (recognizerCount > 0) {
                indexable = new SearchIndexableRaw(context);
                indexable.title = context.getString(R.string.recognizer_settings_title);
                indexable.screenTitle = screenTitle;
                indexables.add(indexable);
            }

            if (recognizerCount > 1) {
                // Recognizer chooser.
                indexable = new SearchIndexableRaw(context);
                indexable.title = context.getString(R.string.recognizer_title);
                indexable.screenTitle = screenTitle;
                indexables.add(indexable);
            }

            for (int i = 0; i < recognizerCount; i++) {
                ResolveInfo recognizer = recognizers.get(i);
                indexable = new SearchIndexableRaw(context);
                indexable.title = recognizer.loadLabel(context.getPackageManager()).toString();
                indexable.screenTitle = screenTitle;
                indexables.add(indexable);
            }

            // Text-to-speech.
            TtsEngines ttsEngines = new TtsEngines(context);
            if (!ttsEngines.getEngines().isEmpty()) {
                indexable = new SearchIndexableRaw(context);
                indexable.title = context.getString(R.string.tts_settings_title);
                indexable.screenTitle = screenTitle;
                indexables.add(indexable);
            }

            // Pointer settings.
            indexable = new SearchIndexableRaw(context);
            indexable.title = context.getString(R.string.pointer_settings_category);
            indexable.screenTitle = screenTitle;
            indexables.add(indexable);

            indexable = new SearchIndexableRaw(context);
            indexable.title = context.getString(R.string.pointer_speed);
            indexable.screenTitle = screenTitle;
            indexables.add(indexable);

            // Game controllers.
            if (haveInputDeviceWithVibrator()) {
                indexable = new SearchIndexableRaw(context);
                indexable.title = context.getString(R.string.vibrate_input_devices);
                indexable.summaryOn = context.getString(R.string.vibrate_input_devices_summary);
                indexable.summaryOff = context.getString(R.string.vibrate_input_devices_summary);
                indexable.screenTitle = screenTitle;
                indexables.add(indexable);
            }

            return indexables;
        }
    };
}
