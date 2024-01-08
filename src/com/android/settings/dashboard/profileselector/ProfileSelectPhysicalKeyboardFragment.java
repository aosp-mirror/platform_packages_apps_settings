/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.dashboard.profileselector;

import android.hardware.input.InputDeviceIdentifier;
import android.os.Bundle;
import android.provider.Settings;

import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.inputmethod.NewKeyboardLayoutEnabledLocalesFragment;

/**
 * When current user has work profile, this fragment used following fragments to represent the
 * enabled IMEs keyboard layout settings page.
 *
 * <p>{@link NewKeyboardLayoutEnabledLocalesFragment} used to show both of personal/work user
 * enabled IMEs and their physical keyboard layouts.</p>
 */
public final class ProfileSelectPhysicalKeyboardFragment extends ProfileSelectFragment {

    private InputDeviceIdentifier mInputDeviceIdentifier;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Bundle arguments = getArguments();
        mInputDeviceIdentifier =
                arguments.getParcelable(Settings.EXTRA_INPUT_DEVICE_IDENTIFIER);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.keyboard_settings_enabled_locales_list;
    }

    @Override
    public Fragment[] getFragments() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Settings.EXTRA_INPUT_DEVICE_IDENTIFIER, mInputDeviceIdentifier);
        return ProfileSelectFragment.getFragments(
                getContext(),
                bundle,
                NewKeyboardLayoutEnabledLocalesFragment::new,
                NewKeyboardLayoutEnabledLocalesFragment::new,
                NewKeyboardLayoutEnabledLocalesFragment::new);
    }
}
