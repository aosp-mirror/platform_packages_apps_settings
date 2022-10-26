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

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

public class ModifierKeysPreferenceController extends BasePreferenceController {

    private static String KEY_TAG = "modifier_keys_dialog_tag";
    private static String KEY_RESTORE_PREFERENCE = "modifier_keys_restore";

    private Fragment mParent;
    private FragmentManager mFragmentManager;

    public ModifierKeysPreferenceController(Context context, String key) {
        super(context, key);
    }

    public void setFragment(Fragment parent) {
        mParent = parent;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        // TODO: getModifierKeyRemapping()
        // setTitle
        // setSummary
        if (mParent == null) {
            return;
        }
        // The dialog screen depends on the previous selected key's fragment.
        // In the rotation scenario, we should remove the previous dialog screen first.
        clearPreviousDialog();
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (preference.getKey().equals(KEY_RESTORE_PREFERENCE)) {
            return false;
        }
        showModifierKeysDialog(preference);
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    private void showModifierKeysDialog(Preference preference) {
        ModifierKeysPickerDialogFragment fragment =
                new ModifierKeysPickerDialogFragment(preference);
        fragment.setTargetFragment(mParent, 0);
        fragment.show(mFragmentManager, KEY_TAG);
    }

    private void clearPreviousDialog() {
        mFragmentManager = mParent.getFragmentManager();
        DialogFragment preKeysDialogFragment =
                (DialogFragment) mFragmentManager.findFragmentByTag(KEY_TAG);
        if (preKeysDialogFragment != null) {
            preKeysDialogFragment.dismiss();
        }
    }
}
