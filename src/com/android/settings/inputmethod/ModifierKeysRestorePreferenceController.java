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

import android.content.Context;
import android.hardware.input.InputManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.Utils;

public class ModifierKeysRestorePreferenceController extends BasePreferenceController {

    private static String KEY_TAG = "modifier_keys_dialog_tag";

    private Fragment mParent;
    private FragmentManager mFragmentManager;
    private PreferenceScreen mScreen;
    private final InputManager mIm;

    public ModifierKeysRestorePreferenceController(Context context, String key) {
        super(context, key);
        mIm = context.getSystemService(InputManager.class);
    }

    public void setFragment(Fragment parent) {
        mParent = parent;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (mParent == null) {
            return;
        }
        mScreen = screen;
        // The dialog screen depends on the previous selected key's fragment.
        // In the rotation scenario, we should remove the previous dialog first.
        clearPreviousDialog();
        setResetKeyColor();
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!preference.getKey().equals(getPreferenceKey())) {
            return false;
        }
        showResetDialog();
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    private void showResetDialog() {
        ModifierKeysResetDialogFragment fragment =
                new ModifierKeysResetDialogFragment(mScreen, mIm);
        fragment.setTargetFragment(mParent, 0);
        fragment.show(mFragmentManager, KEY_TAG);
    }

    private void setResetKeyColor() {
        Preference preference = mScreen.findPreference(getPreferenceKey());
        Spannable title = new SpannableString(
                mParent.getActivity().getString(R.string.modifier_keys_reset_title));
        title.setSpan(
                new ForegroundColorSpan(getColorOfColorAccentPrimaryVariant()),
                0, title.length(), 0);
        preference.setTitle(title);
    }

    private int getColorOfColorAccentPrimaryVariant() {
        return Utils.getColorAttrDefaultColor(
                mParent.getActivity(), com.android.internal.R.attr.materialColorPrimaryContainer);
    }

    private void clearPreviousDialog() {
        mFragmentManager = mParent.getFragmentManager();
        DialogFragment preResetDialogFragment =
                (DialogFragment) mFragmentManager.findFragmentByTag(KEY_TAG);
        if (preResetDialogFragment != null) {
            preResetDialogFragment.dismiss();
        }
    }
}
