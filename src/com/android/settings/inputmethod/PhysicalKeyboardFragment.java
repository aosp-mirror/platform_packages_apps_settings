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
import android.content.AsyncTaskLoader;
import android.content.Context;
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
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import com.android.internal.inputmethod.InputMethodUtils;
import com.android.internal.util.Preconditions;
import com.android.settings.R;
import com.android.settings.InstrumentedFragment;
import com.android.settings.Settings;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PhysicalKeyboardFragment extends SettingsPreferenceFragment
        implements LoaderManager.LoaderCallbacks<PhysicalKeyboardFragment.Keyboards>,
        InputManager.InputDeviceListener {

    private static final int USER_SYSTEM = 0;
    private static final String KEYBOARD_ASSISTANCE_CATEGORY = "keyboard_assistance_category";
    private static final String SHOW_VIRTUAL_KEYBOARD_SWITCH = "show_virtual_keyboard_switch";
    private static final String IM_SUBTYPE_MODE_KEYBOARD = "keyboard";

    private final HashMap<Integer, Pair<InputDeviceIdentifier, PreferenceCategory>> mLoaderReference
            = new HashMap<>();
    private final Map<InputMethodInfo, List<InputMethodSubtype>> mImiSubtypes = new HashMap<>();
    private InputManager mIm;
    private InputMethodManager mImm;
    private PreferenceCategory mKeyboardAssistanceCategory;
    private SwitchPreference mShowVirtualKeyboardSwitch;
    private InputMethodUtils.InputMethodSettings mSettings;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        Activity activity = Preconditions.checkNotNull(getActivity());
        addPreferencesFromResource(R.xml.physical_keyboard_settings);
        mIm = Preconditions.checkNotNull(activity.getSystemService(InputManager.class));
        mImm = Preconditions.checkNotNull(activity.getSystemService(InputMethodManager.class));
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
    public Loader<Keyboards> onCreateLoader(int id, Bundle args) {
        final InputDeviceIdentifier deviceId = mLoaderReference.get(id).first;
        return new KeyboardLayoutLoader(
                getActivity().getBaseContext(), mIm, mImiSubtypes, deviceId);
    }

    @Override
    public void onLoadFinished(Loader<Keyboards> loader, Keyboards data) {
        // TODO: Investigate why this is being called twice.
        final InputDeviceIdentifier deviceId = mLoaderReference.get(loader.getId()).first;
        final PreferenceCategory category = mLoaderReference.get(loader.getId()).second;
        category.removeAll();
        for (Keyboards.KeyboardInfo info : data.mInfos) {
            Preference pref = new Preference(getPrefContext(), null);
            final InputMethodInfo imi = info.mImi;
            final InputMethodSubtype imSubtype = info.mImSubtype;
            if (imi != null && imSubtype != null) {
                pref.setTitle(getDisplayName(getContext(), imi, imSubtype));
                KeyboardLayout layout = info.mLayout;
                if (layout != null) {
                    pref.setSummary(layout.getLabel());
                }
                pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showKeyboardLayoutScreen(deviceId, imi, imSubtype);
                        return true;
                    }
                });
                category.addPreference(pref);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Keyboards> loader) {}

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
        loadInputMethodInfoSubtypes();
        final int[] devices = InputDevice.getDeviceIds();
        for (int deviceIndex = 0; deviceIndex < devices.length; deviceIndex++) {
            InputDevice device = InputDevice.getDevice(devices[deviceIndex]);
            if (device != null
                    && !device.isVirtual()
                    && device.isFullKeyboard()) {
                final PreferenceCategory category = new PreferenceCategory(getPrefContext(), null);
                category.setTitle(device.getName());
                category.setOrder(0);
                mLoaderReference.put(deviceIndex, new Pair(device.getIdentifier(), category));
                getPreferenceScreen().addPreference(category);
            }
        }
        mKeyboardAssistanceCategory.setOrder(1);
        getPreferenceScreen().addPreference(mKeyboardAssistanceCategory);

        for (int deviceIndex : mLoaderReference.keySet()) {
            getLoaderManager().initLoader(deviceIndex, null, this);
        }
        updateShowVirtualKeyboardSwitch();
    }

    private void showKeyboardLayoutScreen(
            InputDeviceIdentifier inputDeviceIdentifier,
            InputMethodInfo imi,
            InputMethodSubtype imSubtype) {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClass(getActivity(), Settings.KeyboardLayoutPickerActivity.class);
        intent.putExtra(KeyboardLayoutPickerFragment2.EXTRA_INPUT_DEVICE_IDENTIFIER,
                inputDeviceIdentifier);
        intent.putExtra(KeyboardLayoutPickerFragment2.EXTRA_INPUT_METHOD_INFO, imi);
        intent.putExtra(KeyboardLayoutPickerFragment2.EXTRA_INPUT_METHOD_SUBTYPE, imSubtype);
        startActivity(intent);
    }

    private void clearHardKeyboardsData() {
        getPreferenceScreen().removeAll();
        for (int index = 0; index < mLoaderReference.size(); index++) {
            getLoaderManager().destroyLoader(index);
        }
        mLoaderReference.clear();
    }

    private void loadInputMethodInfoSubtypes() {
        mImiSubtypes.clear();
        final List<InputMethodInfo> imis = mImm.getEnabledInputMethodList();
        for (InputMethodInfo imi : imis) {
            final List<InputMethodSubtype> subtypes = new ArrayList<>();
            for (InputMethodSubtype subtype : mImm.getEnabledInputMethodSubtypeList(
                    imi, true /* allowsImplicitlySelectedSubtypes */)) {
                if (IM_SUBTYPE_MODE_KEYBOARD.equalsIgnoreCase(subtype.getMode())) {
                    subtypes.add(subtype);
                }
            }
            mImiSubtypes.put(imi, subtypes);
        }
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

    static String getDisplayName(
            Context context, InputMethodInfo imi, InputMethodSubtype imSubtype) {
        CharSequence imSubtypeName =  imSubtype.getDisplayName(
                context, imi.getPackageName(),
                imi.getServiceInfo().applicationInfo);
        CharSequence imeName = imi.loadLabel(context.getPackageManager());
        return String.format(
                context.getString(R.string.physical_device_title), imSubtypeName, imeName);
    }

    private static final class KeyboardLayoutLoader extends AsyncTaskLoader<Keyboards> {

        private final Map<InputMethodInfo, List<InputMethodSubtype>> mImiSubtypes;
        private final InputDeviceIdentifier mInputDeviceIdentifier;
        private final InputManager mIm;

        public KeyboardLayoutLoader(
                Context context,
                InputManager im,
                Map<InputMethodInfo, List<InputMethodSubtype>> imiSubtypes,
                InputDeviceIdentifier inputDeviceIdentifier) {
            super(context);
            mIm = Preconditions.checkNotNull(im);
            mInputDeviceIdentifier = Preconditions.checkNotNull(inputDeviceIdentifier);
            mImiSubtypes = new HashMap<>(imiSubtypes);
        }

        @Override
        public Keyboards loadInBackground() {
            final Keyboards keyboards = new Keyboards();
            for (InputMethodInfo imi : mImiSubtypes.keySet()) {
                for (InputMethodSubtype subtype : mImiSubtypes.get(imi)) {
                    final KeyboardLayout layout = mIm.getKeyboardLayoutForInputDevice(
                            mInputDeviceIdentifier, imi, subtype);
                    keyboards.mInfos.add(new Keyboards.KeyboardInfo(imi, subtype, layout));
                }
            }
            return keyboards;
        }

        @Override
        protected void onStartLoading() {
            super.onStartLoading();
            forceLoad();
        }

        @Override
        protected void onStopLoading() {
            super.onStopLoading();
            cancelLoad();
        }
    }

    public static final class Keyboards {

        public final ArrayList<KeyboardInfo> mInfos = new ArrayList<>();

        public static final class KeyboardInfo {

            public final InputMethodInfo mImi;
            public final InputMethodSubtype mImSubtype;
            public final KeyboardLayout mLayout;

            public KeyboardInfo(
                    InputMethodInfo imi, InputMethodSubtype imSubtype, KeyboardLayout layout) {
                mImi = imi;
                mImSubtype = imSubtype;
                mLayout = layout;
            }
        }
    }

}
