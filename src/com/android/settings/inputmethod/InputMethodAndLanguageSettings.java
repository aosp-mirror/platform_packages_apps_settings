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

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.input.InputDeviceIdentifier;
import android.hardware.input.InputManager;
import android.hardware.input.KeyboardLayout;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.System;
import android.speech.tts.TtsEngines;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceCategory;
import android.view.InputDevice;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.view.textservice.SpellCheckerInfo;
import android.view.textservice.TextServicesManager;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.Settings.KeyboardLayoutPickerActivity;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SubSettings;
import com.android.settings.UserDictionarySettings;
import com.android.settings.Utils;
import com.android.settings.VoiceInputOutputSettings;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.localepicker.LocaleFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class InputMethodAndLanguageSettings extends SettingsPreferenceFragment
        implements InputManager.InputDeviceListener,
        KeyboardLayoutDialogFragment.OnSetupKeyboardLayoutsListener, Indexable {

    private static final String KEY_SPELL_CHECKERS = "spellcheckers_settings";
    private static final String KEY_PHONE_LANGUAGE = "phone_language";
    private static final String KEY_USER_DICTIONARY_SETTINGS = "key_user_dictionary_settings";

    private PreferenceCategory mGameControllerCategory;
    private Preference mLanguagePref;
    private InputManager mIm;
    private Intent mIntentWaitingForResult;
    private InputMethodSettingValuesWrapper mInputMethodSettingValues;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.INPUTMETHOD_LANGUAGE;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.language_settings);

        final Activity activity = getActivity();
        mInputMethodSettingValues = InputMethodSettingValuesWrapper.getInstance(activity);

        if (activity.getAssets().getLocales().length == 1) {
            // No "Select language" pref if there's only one system locale available.
            getPreferenceScreen().removePreference(findPreference(KEY_PHONE_LANGUAGE));
        } else {
            mLanguagePref = findPreference(KEY_PHONE_LANGUAGE);
        }

        new VoiceInputOutputSettings(this).onCreate();

        mGameControllerCategory = (PreferenceCategory)findPreference(
                "game_controller_settings_category");

        // Build hard keyboard and game controller preference categories.
        mIm = (InputManager)activity.getSystemService(Context.INPUT_SERVICE);
        updateInputDevices();

        // Spell Checker
        final Preference spellChecker = findPreference(KEY_SPELL_CHECKERS);
        if (spellChecker != null) {
            // Note: KEY_SPELL_CHECKERS preference is marked as persistent="false" in XML.
            InputMethodAndSubtypeUtil.removeUnnecessaryNonPersistentPreference(spellChecker);
            final Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClass(activity, SubSettings.class);
            intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT,
                    SpellCheckersSettings.class.getName());
            intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID,
                    R.string.spellcheckers_settings_title);
            spellChecker.setIntent(intent);
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
                                    targetFragment.getCanonicalName(), -1, -1, extras);
                            return true;
                        }
                    });
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mIm.registerInputDeviceListener(this, null);

        final Preference spellChecker = findPreference(KEY_SPELL_CHECKERS);
        if (spellChecker != null) {
            final TextServicesManager tsm = (TextServicesManager) getSystemService(
                    Context.TEXT_SERVICES_MANAGER_SERVICE);
            if (!tsm.isSpellCheckerEnabled()) {
                spellChecker.setSummary(R.string.switch_off_text);
            } else {
                final SpellCheckerInfo sci = tsm.getCurrentSpellChecker();
                if (sci != null) {
                    spellChecker.setSummary(sci.loadLabel(getPackageManager()));
                } else {
                    spellChecker.setSummary(R.string.spell_checker_not_selected);
                }
            }
        }

        if (mLanguagePref != null) {
            final String localeNames = FeatureFactory.getFactory(getContext())
                    .getLocaleFeatureProvider().getLocaleNames();
            mLanguagePref.setSummary(localeNames);
        }

        updateUserDictionaryPreference(findPreference(KEY_USER_DICTIONARY_SETTINGS));

        updateInputDevices();

        // Refresh internal states in mInputMethodSettingValues to keep the latest
        // "InputMethodInfo"s and "InputMethodSubtype"s
        mInputMethodSettingValues.refreshAllInputMethodAndSubtypes();
    }

    @Override
    public void onPause() {
        super.onPause();

        mIm.unregisterInputDeviceListener(this);

        // TODO: Consolidate the logic to InputMethodSettingsWrapper
        InputMethodAndSubtypeUtil.saveInputMethodSubtypeList(
                this, getContentResolver(), mInputMethodSettingValues.getInputMethodList(),
                false /* hasHardKeyboard */);
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
    public boolean onPreferenceTreeClick(Preference preference) {
        // Input Method stuff
        if (Utils.isMonkeyRunning()) {
            return false;
        }
        if (preference instanceof SwitchPreference) {
            final SwitchPreference pref = (SwitchPreference) preference;
            if (pref == mGameControllerCategory.findPreference("vibrate_input_devices")) {
                System.putInt(getContentResolver(), Settings.System.VIBRATE_INPUT_DEVICES,
                        pref.isChecked() ? 1 : 0);
                return true;
            }
        }
        return super.onPreferenceTreeClick(preference);
    }

    private void updateInputDevices() {
        updateGameControllers();
    }

    private void showKeyboardLayoutDialog(InputDeviceIdentifier inputDeviceIdentifier) {
        KeyboardLayoutDialogFragment fragment = (KeyboardLayoutDialogFragment)
                getFragmentManager().findFragmentByTag("keyboardLayout");
        if (fragment == null) {
            fragment = new KeyboardLayoutDialogFragment(inputDeviceIdentifier);
            fragment.setTargetFragment(this, 0);
            fragment.show(getActivity().getFragmentManager(), "keyboardLayout");
        }
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

            SwitchPreference pref = (SwitchPreference)
                    mGameControllerCategory.findPreference("vibrate_input_devices");
            pref.setChecked(System.getInt(getContentResolver(),
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

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final SummaryLoader mSummaryLoader;
        private LocaleFeatureProvider mLocaleFeatureProvider;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
            mLocaleFeatureProvider = FeatureFactory.getFactory(context).getLocaleFeatureProvider();
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                String localeNames = mLocaleFeatureProvider.getLocaleNames();
                mSummaryLoader.setSummary(this, localeNames);
            }
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> indexables = new ArrayList<>();

            final String screenTitle = context.getString(R.string.language_keyboard_settings_title);

            // Locale picker.
            if (context.getAssets().getLocales().length > 1) {
                String localeNames = FeatureFactory.getFactory(context).getLocaleFeatureProvider()
                        .getLocaleNames();
                SearchIndexableRaw indexable = new SearchIndexableRaw(context);
                indexable.key = KEY_PHONE_LANGUAGE;
                indexable.title = context.getString(R.string.phone_language);
                indexable.summaryOn = localeNames;
                indexable.summaryOff = localeNames;
                indexable.screenTitle = screenTitle;
                indexables.add(indexable);
            }

            // Spell checker.
            SearchIndexableRaw indexable = new SearchIndexableRaw(context);
            indexable.key = KEY_SPELL_CHECKERS;
            indexable.title = context.getString(R.string.spellcheckers_settings_title);
            indexable.screenTitle = screenTitle;
            indexable.keywords = context.getString(R.string.keywords_spell_checker);
            indexables.add(indexable);

            // User dictionary.
            if (UserDictionaryList.getUserDictionaryLocalesSet(context) != null) {
                indexable = new SearchIndexableRaw(context);
                indexable.key = "user_dict_settings";
                indexable.title = context.getString(R.string.user_dict_settings_title);
                indexable.screenTitle = screenTitle;
                indexables.add(indexable);
            }

            // Keyboard settings.
            indexable = new SearchIndexableRaw(context);
            indexable.key = "keyboard_settings";
            indexable.title = context.getString(R.string.keyboard_settings_category);
            indexable.screenTitle = screenTitle;
            indexable.keywords = context.getString(R.string.keywords_keyboard_and_ime);
            indexables.add(indexable);

            InputMethodSettingValuesWrapper immValues = InputMethodSettingValuesWrapper
                    .getInstance(context);
            immValues.refreshAllInputMethodAndSubtypes();

            InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(
                    Context.INPUT_METHOD_SERVICE);

            // All other IMEs.
            List<InputMethodInfo> inputMethods = immValues.getInputMethodList();
            final int inputMethodCount = (inputMethods == null ? 0 : inputMethods.size());
            for (int i = 0; i < inputMethodCount; ++i) {
                InputMethodInfo inputMethod = inputMethods.get(i);
                List<InputMethodSubtype> subtypes = inputMethodManager
                        .getEnabledInputMethodSubtypeList(inputMethod, true);
                String summary = InputMethodAndSubtypeUtil.getSubtypeLocaleNameListAsSentence(
                        subtypes, context, inputMethod);

                ServiceInfo serviceInfo = inputMethod.getServiceInfo();
                ComponentName componentName = new ComponentName(serviceInfo.packageName,
                        serviceInfo.name);

                indexable = new SearchIndexableRaw(context);
                indexable.key = componentName.flattenToString();
                indexable.title = inputMethod.loadLabel(context.getPackageManager()).toString();
                indexable.summaryOn = summary;
                indexable.summaryOff = summary;
                indexable.screenTitle = screenTitle;
                indexables.add(indexable);
            }

            // Hard keyboards
            InputManager inputManager = (InputManager) context.getSystemService(
                    Context.INPUT_SERVICE);
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
                indexable.key = device.getName();
                indexable.title = device.getName();
                indexable.summaryOn = summary;
                indexable.summaryOff = summary;
                indexable.screenTitle = screenTitle;
                indexables.add(indexable);
            }

            if (hasHardKeyboards) {
                // Hard keyboard category.
                indexable = new SearchIndexableRaw(context);
                indexable.key = "builtin_keyboard_settings";
                indexable.title = context.getString(R.string.builtin_keyboard_settings_title);
                indexable.screenTitle = screenTitle;
                indexables.add(indexable);
            }

            // Text-to-speech.
            TtsEngines ttsEngines = new TtsEngines(context);
            if (!ttsEngines.getEngines().isEmpty()) {
                indexable = new SearchIndexableRaw(context);
                indexable.key = "tts_settings";
                indexable.title = context.getString(R.string.tts_settings_title);
                indexable.screenTitle = screenTitle;
                indexable.keywords = context.getString(R.string.keywords_text_to_speech_output);
                indexables.add(indexable);
            }

            // Pointer settings.
            indexable = new SearchIndexableRaw(context);
            indexable.key = "pointer_settings_category";
            indexable.title = context.getString(R.string.pointer_settings_category);
            indexable.screenTitle = screenTitle;
            indexables.add(indexable);

            indexable = new SearchIndexableRaw(context);
            indexable.key = "pointer_speed";
            indexable.title = context.getString(R.string.pointer_speed);
            indexable.screenTitle = screenTitle;
            indexables.add(indexable);

            // Game controllers.
            if (haveInputDeviceWithVibrator()) {
                indexable = new SearchIndexableRaw(context);
                indexable.key = "vibrate_input_devices";
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
