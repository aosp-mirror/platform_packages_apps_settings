/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.ContentObserver;
import android.hardware.input.InputDeviceIdentifier;
import android.hardware.input.InputManager;
import android.hardware.input.KeyboardLayout;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings.Secure;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceCategory;
import android.support.v14.preference.SwitchPreference;
import android.util.Pair;
import android.view.InputDevice;
import android.view.inputmethod.InputMethodInfo;
import android.widget.Toast;

import com.android.internal.inputmethod.InputMethodUtils;
import com.android.internal.util.Preconditions;
import com.android.settings.R;
import com.android.settings.InstrumentedFragment;
import com.android.settings.Settings;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public final class PhysicalKeyboardFragment extends SettingsPreferenceFragment
        implements LoaderManager.LoaderCallbacks<KeyboardLayoutDialogFragment.Keyboards>,
        InputManager.InputDeviceListener {

    private static final int USER_SYSTEM = 0;
    private static final String KEYBOARD_ASSISTANCE_CATEGORY = "keyboard_assistance_category";
    private static final String SHOW_VIRTUAL_KEYBOARD_SWITCH = "show_virtual_keyboard_switch";
    private static final String KEYBOARD_SHORTCUTS_HELPER = "keyboard_shortcuts_helper";

    private final ArrayList<PreferenceCategory> mHardKeyboardPreferenceList = new ArrayList<>();
    private final HashMap<Integer, Pair<InputDeviceIdentifier, PreferenceCategory>> mLoaderReference
            = new HashMap<>();
    private InputManager mIm;
    private PreferenceCategory mKeyboardAssistanceCategory;
    private SwitchPreference mShowVirtualKeyboardSwitch;
    private InputMethodUtils.InputMethodSettings mSettings;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        Activity activity = Preconditions.checkNotNull(getActivity());
        addPreferencesFromResource(R.xml.physical_keyboard_settings);
        mIm = Preconditions.checkNotNull(activity.getSystemService(InputManager.class));
        mSettings = new InputMethodUtils.InputMethodSettings(
                activity.getResources(),
                getContentResolver(),
                new HashMap<String, InputMethodInfo>(),
                new ArrayList<InputMethodInfo>(),
                USER_SYSTEM);
        mKeyboardAssistanceCategory = Preconditions.checkNotNull(
                (PreferenceCategory) findPreference(KEYBOARD_ASSISTANCE_CATEGORY));
        mShowVirtualKeyboardSwitch = Preconditions.checkNotNull(
                (SwitchPreference) mKeyboardAssistanceCategory.findPreference(
                        SHOW_VIRTUAL_KEYBOARD_SWITCH));
        findPreference(KEYBOARD_SHORTCUTS_HELPER).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        toggleKeyboardShortcutsMenu();
                        return true;
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateHardKeyboards();
        mIm.registerInputDeviceListener(this, null);
        mShowVirtualKeyboardSwitch.setOnPreferenceChangeListener(
                mShowVirtualKeyboardSwitchPreferenceChangeListener);
        registerShowVirtualKeyboardSettingsObserver();
    }

    @Override
    public void onPause() {
        super.onPause();
        clearHardKeyboardsData();
        mIm.unregisterInputDeviceListener(this);
        mShowVirtualKeyboardSwitch.setOnPreferenceChangeListener(null);
        unregisterShowVirtualKeyboardSettingsObserver();
    }

    @Override
    public Loader<KeyboardLayoutDialogFragment.Keyboards> onCreateLoader(int id, Bundle args) {
        InputDeviceIdentifier deviceId = mLoaderReference.get(id).first;
        return new KeyboardLayoutDialogFragment.KeyboardLayoutLoader(
                getActivity().getBaseContext(), deviceId);
    }

    @Override
    public void onLoadFinished(
            final Loader<KeyboardLayoutDialogFragment.Keyboards> loader,
            KeyboardLayoutDialogFragment.Keyboards data) {
        // TODO: Investigate why this is being called twice.
        final InputDeviceIdentifier deviceId = mLoaderReference.get(loader.getId()).first;
        final PreferenceCategory category = mLoaderReference.get(loader.getId()).second;
        category.removeAll();
        for (KeyboardLayout layout : data.keyboardLayouts) {
            if (layout != null) {
                Preference pref = new Preference(getPrefContext(), null);
                pref.setTitle(layout.getLabel());
                pref.setSummary(layout.getCollection());
                pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showKeyboardLayoutScreen(deviceId);
                        return true;
                    }
                });
                category.addPreference(pref);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<KeyboardLayoutDialogFragment.Keyboards> loader) {}

    @Override
    public void onInputDeviceAdded(int deviceId) {
        updateHardKeyboards();
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        updateHardKeyboards();
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        updateHardKeyboards();
    }

    @Override
    protected int getMetricsCategory() {
        return InstrumentedFragment.PHYSICAL_KEYBOARDS;
    }

    private void updateHardKeyboards() {
        clearHardKeyboardsData();
        final int[] devices = InputDevice.getDeviceIds();
        for (int deviceIndex = 0; deviceIndex < devices.length; deviceIndex++) {
            InputDevice device = InputDevice.getDevice(devices[deviceIndex]);
            if (device != null
                    && !device.isVirtual()
                    && device.isFullKeyboard()) {
                final InputDeviceIdentifier deviceId = device.getIdentifier();
                final String keyboardLayoutDescriptor =
                        mIm.getCurrentKeyboardLayoutForInputDevice(deviceId);
                final KeyboardLayout keyboardLayout = keyboardLayoutDescriptor != null ?
                        mIm.getKeyboardLayout(keyboardLayoutDescriptor) : null;

                final PreferenceCategory category = new PreferenceCategory(getPrefContext(), null);
                category.setTitle(device.getName());
                if (keyboardLayout != null) {
                    category.setSummary(keyboardLayout.toString());
                } else {
                    category.setSummary(R.string.keyboard_layout_default_label);
                }
                mLoaderReference.put(deviceIndex, new Pair(deviceId, category));
                mHardKeyboardPreferenceList.add(category);
            }
        }

        Collections.sort(mHardKeyboardPreferenceList);
        final int count = mHardKeyboardPreferenceList.size();
        for (int i = 0; i < count; i++) {
            final PreferenceCategory category = mHardKeyboardPreferenceList.get(i);
            category.setOrder(i);
            getPreferenceScreen().addPreference(category);
        }
        mKeyboardAssistanceCategory.setOrder(count);
        getPreferenceScreen().addPreference(mKeyboardAssistanceCategory);

        for (int deviceIndex : mLoaderReference.keySet()) {
            getLoaderManager().initLoader(deviceIndex, null, this);
        }
        updateShowVirtualKeyboardSwitch();
    }

    private void showKeyboardLayoutScreen(InputDeviceIdentifier inputDeviceIdentifier) {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClass(getActivity(), Settings.KeyboardLayoutPickerActivity.class);
        intent.putExtra(KeyboardLayoutPickerFragment.EXTRA_INPUT_DEVICE_IDENTIFIER,
                inputDeviceIdentifier);
        startActivity(intent);
    }

    private void clearHardKeyboardsData() {
        getPreferenceScreen().removeAll();
        for (int index = 0; index < mLoaderReference.size(); index++) {
            getLoaderManager().destroyLoader(index);
        }
        mLoaderReference.clear();
        mHardKeyboardPreferenceList.clear();
    }

    private void registerShowVirtualKeyboardSettingsObserver() {
        unregisterShowVirtualKeyboardSettingsObserver();
        getActivity().getContentResolver().registerContentObserver(
                Secure.getUriFor(Secure.SHOW_IME_WITH_HARD_KEYBOARD),
                false,
                mContentObserver,
                USER_SYSTEM);
        updateShowVirtualKeyboardSwitch();
    }

    private void unregisterShowVirtualKeyboardSettingsObserver() {
        getActivity().getContentResolver().unregisterContentObserver(mContentObserver);
    }

    private void updateShowVirtualKeyboardSwitch() {
        mShowVirtualKeyboardSwitch.setChecked(mSettings.isShowImeWithHardKeyboardEnabled());
    }

    private void toggleKeyboardShortcutsMenu() {
        // TODO: Implement.
        Toast.makeText(getActivity(), "toggleKeyboardShortcutsMenu", Toast.LENGTH_SHORT).show();
    }

    private final OnPreferenceChangeListener mShowVirtualKeyboardSwitchPreferenceChangeListener =
            new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    mSettings.setShowImeWithHardKeyboard((Boolean) newValue);
                    return false;
                }
            };

    private final ContentObserver mContentObserver = new ContentObserver(new Handler(true)) {
        @Override
        public void onChange(boolean selfChange) {
            updateShowVirtualKeyboardSwitch();
        }
    };
}
