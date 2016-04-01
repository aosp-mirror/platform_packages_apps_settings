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

import android.annotation.NonNull;
import android.annotation.Nullable;
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
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.InputDevice;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import com.android.internal.inputmethod.InputMethodUtils;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.util.Preconditions;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.SettingsPreferenceFragment;

import libcore.util.Objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public final class PhysicalKeyboardFragment extends SettingsPreferenceFragment
        implements InputManager.InputDeviceListener {

    private static final int USER_SYSTEM = 0;
    private static final String KEYBOARD_ASSISTANCE_CATEGORY = "keyboard_assistance_category";
    private static final String SHOW_VIRTUAL_KEYBOARD_SWITCH = "show_virtual_keyboard_switch";
    private static final String IM_SUBTYPE_MODE_KEYBOARD = "keyboard";

    @NonNull
    private final ArrayList<HardKeyboardDeviceInfo> mLastHardKeyboards = new ArrayList<>();

    @NonNull
    private final HashSet<Integer> mLoaderIDs = new HashSet<>();
    private int mNextLoaderId = 0;

    private InputManager mIm;
    @NonNull
    private PreferenceCategory mKeyboardAssistanceCategory;
    @NonNull
    private SwitchPreference mShowVirtualKeyboardSwitch;
    @NonNull
    private InputMethodUtils.InputMethodSettings mSettings;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        Activity activity = Preconditions.checkNotNull(getActivity());
        addPreferencesFromResource(R.xml.physical_keyboard_settings);
        mIm = Preconditions.checkNotNull(activity.getSystemService(InputManager.class));
        mSettings = new InputMethodUtils.InputMethodSettings(
                activity.getResources(),
                getContentResolver(),
                new HashMap<>(),
                new ArrayList<>(),
                USER_SYSTEM,
                false /* copyOnWrite */);
        mKeyboardAssistanceCategory = Preconditions.checkNotNull(
                (PreferenceCategory) findPreference(KEYBOARD_ASSISTANCE_CATEGORY));
        mShowVirtualKeyboardSwitch = Preconditions.checkNotNull(
                (SwitchPreference) mKeyboardAssistanceCategory.findPreference(
                        SHOW_VIRTUAL_KEYBOARD_SWITCH));
    }

    @Override
    public void onResume() {
        super.onResume();
        clearLoader();
        mLastHardKeyboards.clear();
        updateHardKeyboards();
        mIm.registerInputDeviceListener(this, null);
        mShowVirtualKeyboardSwitch.setOnPreferenceChangeListener(
                mShowVirtualKeyboardSwitchPreferenceChangeListener);
        registerShowVirtualKeyboardSettingsObserver();
    }

    @Override
    public void onPause() {
        super.onPause();
        clearLoader();
        mLastHardKeyboards.clear();
        mIm.unregisterInputDeviceListener(this);
        mShowVirtualKeyboardSwitch.setOnPreferenceChangeListener(null);
        unregisterShowVirtualKeyboardSettingsObserver();
    }

    public void onLoadFinishedInternal(final int loaderId, @NonNull final Keyboards data,
            @NonNull final PreferenceCategory preferenceCategory) {
        if (!mLoaderIDs.remove(loaderId)) {
            // Already destroyed loader.  Ignore.
            return;
        }

        final InputDeviceIdentifier deviceId = data.mInputDeviceIdentifier;
        preferenceCategory.removeAll();
        for (Keyboards.KeyboardInfo info : data.mKeyboardInfoList) {
            Preference pref = new Preference(getPrefContext(), null);
            final InputMethodInfo imi = info.mImi;
            final InputMethodSubtype imSubtype = info.mImSubtype;
            if (imi != null && imSubtype != null) {
                pref.setTitle(getDisplayName(getContext(), imi, imSubtype));
                KeyboardLayout layout = info.mLayout;
                if (layout != null) {
                    pref.setSummary(layout.getLabel());
                }
                pref.setOnPreferenceClickListener(preference -> {
                    showKeyboardLayoutScreen(deviceId, imi, imSubtype);
                    return true;
                });
                preferenceCategory.addPreference(pref);
            }
        }
    }

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
        return MetricsEvent.PHYSICAL_KEYBOARDS;
    }

    @NonNull
    private static ArrayList<HardKeyboardDeviceInfo> getHardKeyboards() {
        final ArrayList<HardKeyboardDeviceInfo> keyboards = new ArrayList<>();
        final int[] devicesIds = InputDevice.getDeviceIds();
        for (int deviceId : devicesIds) {
            final InputDevice device = InputDevice.getDevice(deviceId);
            if (device != null && !device.isVirtual() && device.isFullKeyboard()) {
                keyboards.add(new HardKeyboardDeviceInfo(device.getName(), device.getIdentifier()));
            }
        }
        return keyboards;
    }

    private void updateHardKeyboards() {
        final ArrayList<HardKeyboardDeviceInfo> newHardKeyboards = getHardKeyboards();
        if (!Objects.equal(newHardKeyboards, mLastHardKeyboards)) {
            clearLoader();
            final PreferenceScreen preferenceScreen = getPreferenceScreen();
            preferenceScreen.removeAll();
            mLastHardKeyboards.clear();
            mLastHardKeyboards.addAll(newHardKeyboards);
            final int N = newHardKeyboards.size();
            for (int i = 0; i < N; ++i) {
                final HardKeyboardDeviceInfo deviceInfo = newHardKeyboards.get(i);
                final PreferenceCategory category = new PreferenceCategory(getPrefContext(), null);
                category.setTitle(deviceInfo.mDeviceName);
                category.setOrder(0);
                getLoaderManager().initLoader(mNextLoaderId, null,
                        new Callbacks(getContext(), this, deviceInfo.mDeviceIdentifier, category));
                mLoaderIDs.add(mNextLoaderId);
                ++mNextLoaderId;
                preferenceScreen.addPreference(category);
            }
            mKeyboardAssistanceCategory.setOrder(1);
            preferenceScreen.addPreference(mKeyboardAssistanceCategory);

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

    private void clearLoader() {
        for (final int loaderId : mLoaderIDs) {
            getLoaderManager().destroyLoader(loaderId);
        }
        mLoaderIDs.clear();
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

    @NonNull
    static String getDisplayName(
            @NonNull Context context, @NonNull InputMethodInfo imi,
            @NonNull InputMethodSubtype imSubtype) {
        final CharSequence imSubtypeName = imSubtype.getDisplayName(
                context, imi.getPackageName(), imi.getServiceInfo().applicationInfo);
        final CharSequence imeName = imi.loadLabel(context.getPackageManager());
        return String.format(
                context.getString(R.string.physical_device_title), imSubtypeName, imeName);
    }

    private static final class Callbacks
            implements LoaderManager.LoaderCallbacks<PhysicalKeyboardFragment.Keyboards> {
        @NonNull
        final Context mContext;
        @NonNull
        final PhysicalKeyboardFragment mPhysicalKeyboardFragment;
        @NonNull
        final InputDeviceIdentifier mInputDeviceIdentifier;
        @NonNull
        final PreferenceCategory mPreferenceCategory;
        public Callbacks(
                @NonNull Context context,
                @NonNull PhysicalKeyboardFragment physicalKeyboardFragment,
                @NonNull InputDeviceIdentifier inputDeviceIdentifier,
                @NonNull PreferenceCategory preferenceCategory) {
            mContext = context;
            mPhysicalKeyboardFragment = physicalKeyboardFragment;
            mInputDeviceIdentifier = inputDeviceIdentifier;
            mPreferenceCategory = preferenceCategory;
        }

        @Override
        public Loader<Keyboards> onCreateLoader(int id, Bundle args) {
            return new KeyboardLayoutLoader(mContext, mInputDeviceIdentifier);
        }

        @Override
        public void onLoadFinished(Loader<Keyboards> loader, Keyboards data) {
            mPhysicalKeyboardFragment.onLoadFinishedInternal(loader.getId(), data,
                    mPreferenceCategory);
        }

        @Override
        public void onLoaderReset(Loader<Keyboards> loader) {
        }
    }

    private static final class KeyboardLayoutLoader extends AsyncTaskLoader<Keyboards> {
        @NonNull
        private final InputDeviceIdentifier mInputDeviceIdentifier;

        public KeyboardLayoutLoader(
                @NonNull Context context,
                @NonNull InputDeviceIdentifier inputDeviceIdentifier) {
            super(context);
            mInputDeviceIdentifier = Preconditions.checkNotNull(inputDeviceIdentifier);
        }

        @Override
        public Keyboards loadInBackground() {
            final ArrayList<Keyboards.KeyboardInfo> keyboardInfoList = new ArrayList<>();
            final InputMethodManager imm = getContext().getSystemService(InputMethodManager.class);
            final InputManager im = getContext().getSystemService(InputManager.class);
            if (imm != null && im != null) {
                for (InputMethodInfo imi : imm.getEnabledInputMethodList()) {
                    for (InputMethodSubtype subtype : imm.getEnabledInputMethodSubtypeList(
                            imi, true /* allowsImplicitlySelectedSubtypes */)) {
                        if (!IM_SUBTYPE_MODE_KEYBOARD.equalsIgnoreCase(subtype.getMode())) {
                            continue;
                        }
                        final KeyboardLayout layout = im.getKeyboardLayoutForInputDevice(
                                mInputDeviceIdentifier, imi, subtype);
                        keyboardInfoList.add(new Keyboards.KeyboardInfo(imi, subtype, layout));
                    }
                }
            }
            return new Keyboards(mInputDeviceIdentifier, keyboardInfoList);
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

    public static final class HardKeyboardDeviceInfo {
        @NonNull
        public final String mDeviceName;
        @NonNull
        public final InputDeviceIdentifier mDeviceIdentifier;

        public HardKeyboardDeviceInfo(
                @Nullable final String deviceName,
                @NonNull final InputDeviceIdentifier deviceIdentifier) {
            mDeviceName = deviceName != null ? deviceName : "";
            mDeviceIdentifier = deviceIdentifier;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (o == null) return false;

            if (!(o instanceof HardKeyboardDeviceInfo)) return false;

            final HardKeyboardDeviceInfo that = (HardKeyboardDeviceInfo) o;
            if (!TextUtils.equals(mDeviceName, that.mDeviceName)) {
                return false;
            }
            if (mDeviceIdentifier.getVendorId() != that.mDeviceIdentifier.getVendorId()) {
                return false;
            }
            if (mDeviceIdentifier.getProductId() != that.mDeviceIdentifier.getProductId()) {
                return false;
            }
            if (!TextUtils.equals(mDeviceIdentifier.getDescriptor(),
                    that.mDeviceIdentifier.getDescriptor())) {
                return false;
            }

            return true;
        }
    }

    public static final class Keyboards {
        @NonNull
        public final InputDeviceIdentifier mInputDeviceIdentifier;
        @NonNull
        public final ArrayList<KeyboardInfo> mKeyboardInfoList;

        public Keyboards(
                @NonNull final InputDeviceIdentifier inputDeviceIdentifier,
                @NonNull final ArrayList<KeyboardInfo> keyboardInfoList) {
            mInputDeviceIdentifier = inputDeviceIdentifier;
            mKeyboardInfoList = keyboardInfoList;
        }

        public static final class KeyboardInfo {
            @NonNull
            public final InputMethodInfo mImi;
            @NonNull
            public final InputMethodSubtype mImSubtype;
            @NonNull
            public final KeyboardLayout mLayout;

            public KeyboardInfo(
                    @NonNull final InputMethodInfo imi,
                    @NonNull final InputMethodSubtype imSubtype,
                    @NonNull final KeyboardLayout layout) {
                mImi = imi;
                mImSubtype = imSubtype;
                mLayout = layout;
            }
        }
    }

}
