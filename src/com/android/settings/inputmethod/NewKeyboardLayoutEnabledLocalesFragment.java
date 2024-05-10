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
import android.os.UserManager;
import android.util.Log;
import android.view.InputDevice;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settings.inputmethod.NewKeyboardSettingsUtils.KeyboardInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NewKeyboardLayoutEnabledLocalesFragment extends DashboardFragment
        implements InputManager.InputDeviceListener {

    private static final String TAG = "NewKeyboardLayoutEnabledLocalesFragment";

    private InputManager mIm;
    private InputMethodManager mImm;
    private InputDeviceIdentifier mInputDeviceIdentifier;
    private int mUserId;
    private int mInputDeviceId;
    private Context mContext;
    private ArrayList<KeyboardInfo> mKeyboardInfoList = new ArrayList<>();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mContext = context;
        final int profileType = getArguments().getInt(ProfileSelectFragment.EXTRA_PROFILE);
        final int currentUserId = UserHandle.myUserId();
        final int newUserId;
        final UserManager userManager = mContext.getSystemService(UserManager.class);

        switch (profileType) {
            case ProfileSelectFragment.ProfileType.WORK: {
                // If the user is a managed profile user, use currentUserId directly. Or get the
                // managed profile userId instead.
                newUserId = userManager.isManagedProfile()
                        ? currentUserId : Utils.getManagedProfileId(userManager, currentUserId);
                break;
            }
            case ProfileSelectFragment.ProfileType.PRIVATE: {
                // If the user is a private profile user, use currentUserId directly. Or get the
                // private profile userId instead.
                newUserId = userManager.isPrivateProfile()
                        ? currentUserId
                        : Utils.getCurrentUserIdOfType(
                                userManager, ProfileSelectFragment.ProfileType.PRIVATE);
                break;
            }
            case ProfileSelectFragment.ProfileType.PERSONAL: {
                final UserHandle primaryUser = userManager.getPrimaryUser().getUserHandle();
                newUserId = primaryUser.getIdentifier();
                break;
            }
            default:
                newUserId = currentUserId;
        }

        mUserId = newUserId;
        mIm = mContext.getSystemService(InputManager.class);
        mImm = mContext.getSystemService(InputMethodManager.class);
        mInputDeviceId = -1;
    }

    @Override
    public void onActivityCreated(final Bundle icicle) {
        super.onActivityCreated(icicle);
        Bundle arguments = getArguments();
        mInputDeviceIdentifier =
                arguments.getParcelable(NewKeyboardSettingsUtils.EXTRA_INPUT_DEVICE_IDENTIFIER);
        if (mInputDeviceIdentifier == null) {
            Log.e(TAG, "The inputDeviceIdentifier should not be null");
            return;
        }
        InputDevice inputDevice =
                NewKeyboardSettingsUtils.getInputDevice(mIm, mInputDeviceIdentifier);
        if (inputDevice == null) {
            Log.e(TAG, "inputDevice is null");
            return;
        }
        final String title = inputDevice.getName();
        getActivity().setTitle(title);
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
    public void onResume() {
        super.onResume();
        updateCheckedState();
    }

    @Override
    public void onStop() {
        super.onStop();
        mIm.unregisterInputDeviceListener(this);
        mInputDeviceId = -1;
    }

    private void updateCheckedState() {
        if (NewKeyboardSettingsUtils.getInputDevice(mIm, mInputDeviceIdentifier) == null) {
            return;
        }

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removeAll();
        List<InputMethodInfo> infoList =
                mImm.getEnabledInputMethodListAsUser(UserHandle.of(mUserId));
        Collections.sort(infoList, new Comparator<InputMethodInfo>() {
            public int compare(InputMethodInfo o1, InputMethodInfo o2) {
                String s1 = o1.loadLabel(mContext.getPackageManager()).toString();
                String s2 = o2.loadLabel(mContext.getPackageManager()).toString();
                return s1.compareTo(s2);
            }
        });

        for (InputMethodInfo info : infoList) {
            mKeyboardInfoList.clear();
            List<InputMethodSubtype> subtypes =
                    mImm.getEnabledInputMethodSubtypeListAsUser(info.getId(), true,
                            UserHandle.of(mUserId));
            for (InputMethodSubtype subtype : subtypes) {
                if (subtype.isSuitableForPhysicalKeyboardLayoutMapping()) {
                    mapLanguageWithLayout(info, subtype);
                }
            }
            updatePreferenceLayout(preferenceScreen, info);
        }
    }

    private void mapLanguageWithLayout(InputMethodInfo info, InputMethodSubtype subtype) {
        CharSequence subtypeLabel = getSubtypeLabel(mContext, info, subtype);
        KeyboardLayout[] keyboardLayouts =
                NewKeyboardSettingsUtils.getKeyboardLayouts(
                        mIm, mUserId, mInputDeviceIdentifier, info, subtype);
        String layout = NewKeyboardSettingsUtils.getKeyboardLayout(
                mIm, mUserId, mInputDeviceIdentifier, info, subtype);
        if (layout != null) {
            for (int i = 0; i < keyboardLayouts.length; i++) {
                if (keyboardLayouts[i].getDescriptor().equals(layout)) {
                    KeyboardInfo keyboardInfo = new KeyboardInfo(
                            subtypeLabel,
                            keyboardLayouts[i].getLabel(),
                            info,
                            subtype);
                    mKeyboardInfoList.add(keyboardInfo);
                    break;
                }
            }
        } else {
            // if there is no auto-selected layout, we should show "Default"
            KeyboardInfo keyboardInfo = new KeyboardInfo(
                    subtypeLabel,
                    mContext.getString(R.string.keyboard_default_layout),
                    info,
                    subtype);
            mKeyboardInfoList.add(keyboardInfo);
        }
    }

    private void updatePreferenceLayout(PreferenceScreen preferenceScreen, InputMethodInfo info) {
        if (mKeyboardInfoList.isEmpty()) {
            return;
        }
        PreferenceCategory preferenceCategory = new PreferenceCategory(mContext);
        preferenceCategory.setTitle(info.loadLabel(mContext.getPackageManager()));
        preferenceCategory.setKey(info.getPackageName());
        preferenceScreen.addPreference(preferenceCategory);
        Collections.sort(mKeyboardInfoList, new Comparator<KeyboardInfo>() {
            public int compare(KeyboardInfo o1, KeyboardInfo o2) {
                String s1 = o1.getSubtypeLabel().toString();
                String s2 = o2.getSubtypeLabel().toString();
                return s1.compareTo(s2);
            }
        });

        for (KeyboardInfo keyboardInfo : mKeyboardInfoList) {
            final Preference pref = new Preference(mContext);
            pref.setKey(keyboardInfo.getPrefId());
            pref.setTitle(keyboardInfo.getSubtypeLabel());
            pref.setSummary(keyboardInfo.getLayout());
            pref.setOnPreferenceClickListener(
                    preference -> {
                        showKeyboardLayoutPicker(
                                keyboardInfo.getSubtypeLabel(),
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
            CharSequence subtypeLabel,
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
        arguments.putCharSequence(NewKeyboardSettingsUtils.EXTRA_TITLE, subtypeLabel);
        new SubSettingLauncher(mContext)
                .setSourceMetricsCategory(getMetricsCategory())
                .setDestination(NewKeyboardLayoutPickerFragment.class.getName())
                .setArguments(arguments)
                .launch();
    }

    private CharSequence getSubtypeLabel(
            Context context, InputMethodInfo info, InputMethodSubtype subtype) {
        return subtype.getDisplayName(
                context, info.getPackageName(), info.getServiceInfo().applicationInfo);
    }
}
