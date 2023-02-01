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

import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.Utils;

public class ModifierKeysResetDialogFragment extends DialogFragment {
    private static final String MODIFIER_KEYS_CAPS_LOCK = "modifier_keys_caps_lock";
    private static final String MODIFIER_KEYS_CTRL = "modifier_keys_ctrl";
    private static final String MODIFIER_KEYS_META = "modifier_keys_meta";
    private static final String MODIFIER_KEYS_ALT = "modifier_keys_alt";

    private PreferenceScreen mScreen;
    private InputManager mIm;
    private String[] mKeys = {
            MODIFIER_KEYS_CAPS_LOCK,
            MODIFIER_KEYS_CTRL,
            MODIFIER_KEYS_META,
            MODIFIER_KEYS_ALT};

    public ModifierKeysResetDialogFragment() {
    }

    public ModifierKeysResetDialogFragment(PreferenceScreen screen, InputManager inputManager) {
        mScreen = screen;
        mIm = inputManager;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        Context mContext = getActivity();
        View dialoglayout =
                LayoutInflater.from(mContext).inflate(R.layout.modifier_key_reset_dialog, null);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
        dialogBuilder.setView(dialoglayout);
        AlertDialog modifierKeyResetDialog = dialogBuilder.create();

        Button restoreButton = dialoglayout.findViewById(R.id.modifier_key_reset_restore_button);
        restoreButton.setOnClickListener(v -> {
            resetToDefault();
            modifierKeyResetDialog.dismiss();
        });

        Button cancelButton = dialoglayout.findViewById(R.id.modifier_key_reset_cancel_button);
        cancelButton.setOnClickListener(v -> {
            modifierKeyResetDialog.dismiss();
        });

        final Window window = modifierKeyResetDialog.getWindow();
        window.setType(TYPE_SYSTEM_DIALOG);

        return modifierKeyResetDialog;
    }

    private void resetToDefault() {
        Context mContext = getActivity();
        for (int i = 0; i < mKeys.length; i++) {
            Preference preference = mScreen.findPreference(mKeys[i]);
            Spannable title = new SpannableString(
                    mContext.getString(R.string.modifier_keys_default_summary));
            title.setSpan(
                    new ForegroundColorSpan(getColorOfTextColorSecondary()),
                    0, title.length(), 0);
            preference.setSummary(title);
        }

        if (mIm != null) {
            mIm.clearAllModifierKeyRemappings();
        }
    }

    private int getColorOfTextColorSecondary() {
        return Utils.getColorAttrDefaultColor(getActivity(), android.R.attr.textColorSecondary);
    }
}
