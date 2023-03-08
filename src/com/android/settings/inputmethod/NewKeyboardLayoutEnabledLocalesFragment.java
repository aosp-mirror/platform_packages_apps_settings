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
import android.hardware.input.KeyboardLayout;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.InputDevice;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.inputmethod.NewKeyboardSettingsUtils.KeyboardInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NewKeyboardLayoutEnabledLocalesFragment extends DashboardFragment
        implements InputManager.InputDeviceListener {

    private static final String TAG = "NewKeyboardLayoutEnabledLocalesFragment";

    private InputManager mIm;
    private InputMethodManager mImm;
    private InputDeviceIdentifier mInputDeviceIdentifier;
    private int mUserId;
    private int mInputDeviceId;
    private Context mContext;
    private Map<String, KeyboardInfo> mKeyboardLanguageLayouts = new HashMap<>();

    @Override
    public void onActivityCreated(final Bundle icicle) {
        super.onActivityCreated(icicle);
        Bundle arguments = getArguments();
        final String title =
                arguments.getString(NewKeyboardSettingsUtils.EXTRA_KEYBOARD_DEVICE_NAME);
        mInputDeviceIdentifier =
                arguments.getParcelable(NewKeyboardSettingsUtils.EXTRA_INPUT_DEVICE_IDENTIFIER);
        getActivity().setTitle(title);
        updateCheckedState();
    }

    private void updateCheckedState() {
        if (NewKeyboardSettingsUtils.getInputDevice(mIm, mInputDeviceIdentifier) == null) {
            return;
        }
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removeAll();
        List<InputMethodInfo> infoList = mImm.getEnabledInputMethodListAsUser(mUserId);
        for (InputMethodInfo info : infoList) {
            mKeyboardLanguageLayouts.clear();
            List<InputMethodSubtype> subtypes =
                    mImm.getEnabledInputMethodSubtypeList(info, true);
            for (InputMethodSubtype subtype : subtypes) {
                if (subtype.isSuitableForPhysicalKeyboardLayoutMapping()) {
                    mapLanguageWithLayout(info, subtype);
                }
            }
            updatePreferenceLayout(preferenceScreen, info);
        }
    }

    private void mapLanguageWithLayout(InputMethodInfo info, InputMethodSubtype subtype) {
        KeyboardLayout[] keyboardLayouts = getKeyboardLayouts(info, subtype);
        String layout = getKeyboardLayout(info, subtype);
        String language = getLanguage(info, subtype);
        if (layout != null) {
            for (int i = 0; i < keyboardLayouts.length; i++) {
                if (keyboardLayouts[i].getDescriptor().equals(layout)) {
                    KeyboardInfo keyboardInfo = new KeyboardInfo(
                            language,
                            keyboardLayouts[i].getLabel(),
                            info,
                            subtype);
                    mKeyboardLanguageLayouts.put(subtype.getLanguageTag(), keyboardInfo);
                    break;
                }
            }
        } else {
            // if there is no auto-selected layout, we should show "Default"
            KeyboardInfo keyboardInfo = new KeyboardInfo(
                    language,
                    mContext.getString(R.string.keyboard_default_layout),
                    info,
                    subtype);
            mKeyboardLanguageLayouts.put(subtype.getLanguageTag(), keyboardInfo);
        }
    }

    private void updatePreferenceLayout(PreferenceScreen preferenceScreen, InputMethodInfo info) {
        if (mKeyboardLanguageLayouts.isEmpty()) {
            return;
        }
        PreferenceCategory preferenceCategory = new PreferenceCategory(mContext);
        preferenceCategory.setTitle(info.loadLabel(mContext.getPackageManager()).toString());
        preferenceCategory.setKey(info.getPackageName());
        preferenceScreen.addPreference(preferenceCategory);
        for (Map.Entry<String, KeyboardInfo> entry : mKeyboardLanguageLayouts.entrySet()) {
            final Preference pref = new Preference(mContext);
            String key = "keyboard_language_" + entry.getKey();
            NewKeyboardSettingsUtils.KeyboardInfo keyboardInfo = entry.getValue();
            pref.setKey(key);
            pref.setTitle(keyboardInfo.getLanguage());
            pref.setSummary(keyboardInfo.getLayout());
            pref.setOnPreferenceClickListener(
                    preference -> {
                        showKeyboardLayoutPicker(
                                keyboardInfo.getLanguage(),
                                keyboardInfo.getLayout(),
                                mInputDeviceIdentifier,
                                mUserId,
                                keyboardInfo.getInputMethodInfo(),
                                keyboardInfo.getInputMethodSubtype());
                        return true;
                    });
            preferenceCategory.addPreference(pref);
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
            updateCheckedState();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
        mIm = mContext.getSystemService(InputManager.class);
        mImm = mContext.getSystemService(InputMethodManager.class);
        mInputDeviceId = -1;
        mUserId = UserHandle.myUserId();
    }

    @Override
    public void onStart() {
        super.onStart();
        mIm.registerInputDeviceListener(this, null);
        InputDevice inputDevice =
                NewKeyboardSettingsUtils.getInputDevice(mIm, mInputDeviceIdentifier);
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
        updateCheckedState();
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

    private void showKeyboardLayoutPicker(
            String language,
            String layout,
            InputDeviceIdentifier inputDeviceIdentifier,
            int userId,
            InputMethodInfo inputMethodInfo,
            InputMethodSubtype inputMethodSubtype) {
        Bundle arguments = new Bundle();
        arguments.putParcelable(
                NewKeyboardSettingsUtils.EXTRA_INPUT_DEVICE_IDENTIFIER, inputDeviceIdentifier);
        arguments.putParcelable(
                NewKeyboardSettingsUtils.EXTRA_INPUT_METHOD_INFO, inputMethodInfo);
        arguments.putParcelable(
                NewKeyboardSettingsUtils.EXTRA_INPUT_METHOD_SUBTYPE, inputMethodSubtype);
        arguments.putInt(NewKeyboardSettingsUtils.EXTRA_USER_ID, userId);
        arguments.putString(NewKeyboardSettingsUtils.EXTRA_TITLE, language);
        arguments.putString(NewKeyboardSettingsUtils.EXTRA_KEYBOARD_LAYOUT, layout);
        new SubSettingLauncher(mContext)
                .setSourceMetricsCategory(getMetricsCategory())
                .setDestination(NewKeyboardLayoutPickerFragment.class.getName())
                .setArguments(arguments)
                .launch();
    }

    private KeyboardLayout[] getKeyboardLayouts(InputMethodInfo info, InputMethodSubtype subtype) {
        return mIm.getKeyboardLayoutListForInputDevice(
                mInputDeviceIdentifier, mUserId, info, subtype);
    }

    private String getKeyboardLayout(InputMethodInfo info, InputMethodSubtype subtype) {
        return mIm.getKeyboardLayoutForInputDevice(
                mInputDeviceIdentifier, mUserId, info, subtype);
    }

    private String getLanguage(InputMethodInfo info, InputMethodSubtype subtype) {
        String language;
        if (subtype.getLanguageTag().isEmpty()) {
            language = subtype.getDisplayName(
                    mContext,
                    info.getPackageName(),
                    info.getServiceInfo().applicationInfo).toString();
        } else {
            language = Locale.forLanguageTag(subtype.getLanguageTag()).getDisplayName();
        }
        return language;
    }
}
