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
import android.view.KeyEvent;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ModifierKeysPreferenceController extends BasePreferenceController {

    private static String KEY_TAG = "modifier_keys_dialog_tag";
    private static String KEY_RESTORE_PREFERENCE = "modifier_keys_restore";

    private static final String KEY_PREFERENCE_CAPS_LOCK = "modifier_keys_caps_lock";
    private static final String KEY_PREFERENCE_CTRL = "modifier_keys_ctrl";
    private static final String KEY_PREFERENCE_META = "modifier_keys_meta";
    private static final String KEY_PREFERENCE_ALT = "modifier_keys_alt";

    private Fragment mParent;
    private FragmentManager mFragmentManager;
    private final InputManager mIm;

    private final List<Integer> mRemappableKeys = new ArrayList<>(
            Arrays.asList(
                    KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT,
                    KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT,
                    KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT,
                    KeyEvent.KEYCODE_CAPS_LOCK));

    private String[] mKeyNames = new String[] {
            mContext.getString(R.string.modifier_keys_ctrl),
            mContext.getString(R.string.modifier_keys_ctrl),
            mContext.getString(R.string.modifier_keys_meta),
            mContext.getString(R.string.modifier_keys_meta),
            mContext.getString(R.string.modifier_keys_alt),
            mContext.getString(R.string.modifier_keys_alt),
            mContext.getString(R.string.modifier_keys_caps_lock)};

    public ModifierKeysPreferenceController(Context context, String key) {
        super(context, key);
        mIm = context.getSystemService(InputManager.class);
        Objects.requireNonNull(mIm, "InputManager service cannot be null");
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

        for (Map.Entry<Integer, Integer> entry : mIm.getModifierKeyRemapping().entrySet()) {
            int fromKey = entry.getKey();
            int toKey = entry.getValue();
            int index = mRemappableKeys.indexOf(toKey);

            if (isCtrl(fromKey) && mRemappableKeys.contains(toKey)) {
                Preference preference = screen.findPreference(KEY_PREFERENCE_CTRL);
                preference.setSummary(changeSummaryColor(mKeyNames[index]));
            }

            if (isMeta(fromKey) && mRemappableKeys.contains(toKey)) {
                Preference preference = screen.findPreference(KEY_PREFERENCE_META);
                preference.setSummary(changeSummaryColor(mKeyNames[index]));
            }

            if (isAlt(fromKey) && mRemappableKeys.contains(toKey)) {
                Preference preference = screen.findPreference(KEY_PREFERENCE_ALT);
                preference.setSummary(changeSummaryColor(mKeyNames[index]));
            }

            if (isCapLock(fromKey) && mRemappableKeys.contains(toKey)) {
                Preference preference = screen.findPreference(KEY_PREFERENCE_CAPS_LOCK);
                preference.setSummary(changeSummaryColor(mKeyNames[index]));
            }
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
                new ModifierKeysPickerDialogFragment(preference, mIm);
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

    private Spannable changeSummaryColor(String summary) {
        Spannable spannableSummary = new SpannableString(summary);
        spannableSummary.setSpan(
                new ForegroundColorSpan(getColorOfColorAccentPrimaryVariant()),
                0, spannableSummary.length(), 0);
        return spannableSummary;
    }

    private int getColorOfColorAccentPrimaryVariant() {
        return Utils.getColorAttrDefaultColor(
                mContext, com.android.internal.R.attr.materialColorPrimaryContainer);
    }

    private static boolean isCtrl(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_CTRL_LEFT || keyCode == KeyEvent.KEYCODE_CTRL_RIGHT;
    }

    private static boolean isMeta(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_META_LEFT || keyCode == KeyEvent.KEYCODE_META_RIGHT;
    }

    private static boolean isAlt(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT;
    }

    private static boolean isCapLock(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_CAPS_LOCK;
    }
}
