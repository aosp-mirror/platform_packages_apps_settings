/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.hardware.input.InputDeviceIdentifier;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.view.InputDevice;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;

public class NewKeyboardLayoutEnabledLocalesFragment extends DashboardFragment
        implements InputManager.InputDeviceListener {

    private static final String TAG = "NewKeyboardLayoutEnabledLocalesFragment";
    private static final String PREF_KEY_ENABLED_LOCALES = "enabled_locales_keyboard_layout";

    static final String EXTRA_KEYBOARD_DEVICE_NAME = "extra_keyboard_device_name";

    private InputManager mIm;
    private InputDeviceIdentifier mInputDeviceIdentifier;
    private int mInputDeviceId;
    private Context mContext;

    @Override
    public void onActivityCreated(final Bundle icicle) {
        super.onActivityCreated(icicle);

        Bundle arguments = getArguments();
        final String title = arguments.getString(EXTRA_KEYBOARD_DEVICE_NAME);
        mInputDeviceIdentifier = arguments.getParcelable(
                KeyboardLayoutPickerFragment.EXTRA_INPUT_DEVICE_IDENTIFIER);
        getActivity().setTitle(title);
        final PreferenceCategory category = findPreference(PREF_KEY_ENABLED_LOCALES);

        // TODO(b/252816846): Need APIs to get the available keyboards from Inputmanager.
        // For example: InputMethodManager.getEnabledInputMethodLocales()
        //              InputManager.getKeyboardLayoutForLocale()
        // Hardcode the default value for demo purpose
        String[] keyboardLanguages = {"English (US)", "German (Germany)", "Spanish (Spain)"};
        String[] keyboardLayouts = {"English (US)", "German", "Spanish"};
        for (int i = 0; i < keyboardLanguages.length; i++) {
            final Preference pref = new Preference(mContext);
            String key = "keyboard_language_label_" + String.valueOf(i);
            String keyboardLanguageTitle = keyboardLanguages[i];
            String keyboardLanguageSummary = keyboardLayouts[i];
            // TODO: Waiting for new API to use a prefix with special number to setKey
            pref.setKey(key);
            pref.setTitle(keyboardLanguageTitle);
            pref.setSummary(keyboardLanguageSummary);
            pref.setOnPreferenceClickListener(
                    preference -> {
                        showKeyboardLayoutPicker(
                                keyboardLanguageTitle,
                                keyboardLanguageSummary,
                                mInputDeviceIdentifier);
                        return true;
                    });
            category.addPreference(pref);
        }
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        // Do nothing.
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        if (mInputDeviceId >= 0 && deviceId == mInputDeviceId) {
            getActivity().finish();
        }
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        if (mInputDeviceId >= 0 && deviceId == mInputDeviceId) {
            // TODO(b/252816846): Need APIs to update the available keyboards.
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
        mIm = mContext.getSystemService(InputManager.class);
        mInputDeviceId = -1;
    }

    @Override
    public void onStart() {
        super.onStart();
        mIm.registerInputDeviceListener(this, null);
        final InputDevice inputDevice =
                mIm.getInputDeviceByDescriptor(mInputDeviceIdentifier.getDescriptor());
        if (inputDevice == null) {
            getActivity().finish();
            return;
        }
        mInputDeviceId = inputDevice.getId();
    }

    @Override
    public void onStop() {
        super.onStop();
        mIm.unregisterInputDeviceListener(this);
        mInputDeviceId = -1;
    }

    @Override
    public void onResume() {
        super.onResume();
        // TODO(b/252816846): Need APIs to get the available keyboards from Inputmanager.
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_KEYBOARDS_ENABLED_LOCALES;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.keyboard_settings_enabled_locales_list;
    }

    private void showKeyboardLayoutPicker(String language, String layout,
            InputDeviceIdentifier inputDeviceIdentifier) {
        Bundle arguments = new Bundle();
        arguments.putParcelable(KeyboardLayoutPickerFragment.EXTRA_INPUT_DEVICE_IDENTIFIER,
                inputDeviceIdentifier);
        arguments.putString(NewKeyboardLayoutPickerFragment.EXTRA_TITLE, language);
        arguments.putString(NewKeyboardLayoutPickerFragment.EXTRA_KEYBOARD_LAYOUT, layout);
        new SubSettingLauncher(mContext)
                .setSourceMetricsCategory(getMetricsCategory())
                .setDestination(NewKeyboardLayoutPickerFragment.class.getName())
                .setArguments(arguments)
                .launch();
    }
}
