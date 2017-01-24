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
import android.content.Context;
import android.content.Intent;
import android.hardware.input.InputDeviceIdentifier;
import android.speech.tts.TtsEngines;
import android.support.v7.preference.Preference;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.Settings.KeyboardLayoutPickerActivity;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.language.PhoneLanguagePreferenceController;
import com.android.settings.language.TtsPreferenceController;
import com.android.settings.language.UserDictionaryPreferenceController;
import com.android.settings.localepicker.LocaleFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;

/**
 * @deprecated New features should use {@code InputAndGestureSettings} and
 * {@code LanguageAndRegionSettings} instead.
 */
@Deprecated
public class InputMethodAndLanguageSettings extends DashboardFragment
        implements KeyboardLayoutDialogFragment.OnSetupKeyboardLayoutsListener, Indexable {

    private static final String TAG = "IMEAndLanguageSetting";

    private Intent mIntentWaitingForResult;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.INPUTMETHOD_LANGUAGE;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.language_settings;
    }

    @Override
    protected List<PreferenceController> getPreferenceControllers(Context context) {
        final GameControllerPreferenceController gameControllerPreferenceController =
                new GameControllerPreferenceController(context);
        getLifecycle().addObserver(gameControllerPreferenceController);

        final List<PreferenceController> list = new ArrayList<>();
        list.add(gameControllerPreferenceController);
        list.add(new PhoneLanguagePreferenceController(context));
        list.add(new SpellCheckerPreferenceController(context));
        list.add(new UserDictionaryPreferenceController(context));
        list.add(new TtsPreferenceController(context, new TtsEngines(context)));
        return list;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        // Input Method stuff
        if (Utils.isMonkeyRunning()) {
            return false;
        }
        return super.onPreferenceTreeClick(preference);
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

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {

        private final SummaryLoader mSummaryLoader;
        private LocaleFeatureProvider mLocaleFeatureProvider;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
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
            final PhoneLanguagePreferenceController mLanguagePrefController =
                    new PhoneLanguagePreferenceController(context);
            if (mLanguagePrefController.isAvailable()) {
                String localeNames = FeatureFactory.getFactory(context).getLocaleFeatureProvider()
                        .getLocaleNames();
                SearchIndexableRaw indexable = new SearchIndexableRaw(context);
                indexable.key = mLanguagePrefController.getPreferenceKey();
                indexable.title = context.getString(R.string.phone_language);
                indexable.summaryOn = localeNames;
                indexable.summaryOff = localeNames;
                indexable.screenTitle = screenTitle;
                indexables.add(indexable);
            }

            // Spell checker.
            SearchIndexableRaw indexable = new SearchIndexableRaw(context);
            indexable.key = SpellCheckerPreferenceController.KEY_SPELL_CHECKERS;
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

            if (!PhysicalKeyboardFragment.getPhysicalFullKeyboards().isEmpty()) {
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
            if (!new GameControllerPreferenceController(context).isAvailable()) {
                indexable = new SearchIndexableRaw(context);
                indexable.key = GameControllerPreferenceController.PREF_KEY;
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
