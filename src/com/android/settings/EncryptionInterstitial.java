/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;

import java.util.List;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.RadioButton;
import android.widget.TextView;

public class EncryptionInterstitial extends SettingsActivity {

    protected static final String EXTRA_PASSWORD_QUALITY = "extra_password_quality";
    public static final String EXTRA_REQUIRE_PASSWORD = "extra_require_password";

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, EncryptionInterstitialFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return EncryptionInterstitialFragment.class.getName().equals(fragmentName);
    }

    public static Intent createStartIntent(Context ctx, int quality,
            boolean requirePasswordDefault) {
        return new Intent(ctx, EncryptionInterstitial.class)
                .putExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, true)
                .putExtra(EXTRA_PREFS_SET_BACK_TEXT, (String) null)
                .putExtra(EXTRA_PREFS_SET_NEXT_TEXT, ctx.getString(
                        R.string.encryption_continue_button))
                .putExtra(EXTRA_PASSWORD_QUALITY, quality)
                .putExtra(EXTRA_SHOW_FRAGMENT_TITLE_RESID, R.string.encryption_interstitial_header)
                .putExtra(EXTRA_REQUIRE_PASSWORD, requirePasswordDefault);
    }

    public static class EncryptionInterstitialFragment extends SettingsPreferenceFragment
            implements View.OnClickListener, OnClickListener {

        private static final int ACCESSIBILITY_WARNING_DIALOG = 1;
        private RadioButton mRequirePasswordToDecryptButton;
        private RadioButton mDontRequirePasswordToDecryptButton;
        private TextView mEncryptionMessage;
        private boolean mPasswordRequired;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            final int layoutId = R.layout.encryption_interstitial;
            View view = inflater.inflate(layoutId, container, false);
            mRequirePasswordToDecryptButton =
                    (RadioButton) view.findViewById(R.id.encrypt_require_password);
            mDontRequirePasswordToDecryptButton =
                    (RadioButton) view.findViewById(R.id.encrypt_dont_require_password);
            mEncryptionMessage =
                    (TextView) view.findViewById(R.id.encryption_message);
            int quality = getActivity().getIntent().getIntExtra(EXTRA_PASSWORD_QUALITY, 0);
            final int msgId;
            final int enableId;
            final int disableId;
            switch (quality) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    msgId = R.string.encryption_interstitial_message_pattern;
                    enableId = R.string.encrypt_require_pattern;
                    disableId = R.string.encrypt_dont_require_pattern;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                    msgId = R.string.encryption_interstitial_message_pin;
                    enableId = R.string.encrypt_require_pin;
                    disableId = R.string.encrypt_dont_require_pin;
                    break;
                default:
                    msgId = R.string.encryption_interstitial_message_password;
                    enableId = R.string.encrypt_require_password;
                    disableId = R.string.encrypt_dont_require_password;
                    break;
            }
            mEncryptionMessage.setText(msgId);

            mRequirePasswordToDecryptButton.setOnClickListener(this);
            mRequirePasswordToDecryptButton.setText(enableId);

            mDontRequirePasswordToDecryptButton.setOnClickListener(this);
            mDontRequirePasswordToDecryptButton.setText(disableId);

            setRequirePasswordState(getActivity().getIntent().getBooleanExtra(
                    EXTRA_REQUIRE_PASSWORD, true));
            return view;
        }

        @Override
        public void onClick(View v) {
            if (v == mRequirePasswordToDecryptButton) {
                final boolean accEn = AccessibilityManager.getInstance(getActivity()).isEnabled();
                if (accEn && !mPasswordRequired) {
                    setRequirePasswordState(false); // clear the UI state
                    showDialog(ACCESSIBILITY_WARNING_DIALOG);
                } else {
                    setRequirePasswordState(true);
                }
            } else {
                setRequirePasswordState(false);
            }
        }

        @Override
        public Dialog onCreateDialog(int dialogId) {
            switch(dialogId) {
                case ACCESSIBILITY_WARNING_DIALOG: {
                    final int quality = new LockPatternUtils(getActivity())
                            .getKeyguardStoredPasswordQuality();
                    final int titleId;
                    final int messageId;
                    switch (quality) {
                        case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                            titleId = R.string.encrypt_talkback_dialog_require_pattern;
                            messageId = R.string.encrypt_talkback_dialog_message_pattern;
                            break;
                        case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                        case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                            titleId = R.string.encrypt_talkback_dialog_require_pin;
                            messageId = R.string.encrypt_talkback_dialog_message_pin;
                            break;
                        default:
                            titleId = R.string.encrypt_talkback_dialog_require_password;
                            messageId = R.string.encrypt_talkback_dialog_message_password;
                            break;
                    }


                    List<AccessibilityServiceInfo> list =
                            AccessibilityManager.getInstance(getActivity())
                            .getEnabledAccessibilityServiceList(
                                    AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
                    final CharSequence exampleAccessibility;
                    if (list.isEmpty()) {
                        // This should never happen.  But we shouldn't crash
                        exampleAccessibility = "";
                    } else {
                        exampleAccessibility = list.get(0).getResolveInfo()
                                .loadLabel(getPackageManager());
                    }
                    return new AlertDialog.Builder(getActivity())
                        .setTitle(titleId)
                        .setMessage(getString(messageId, exampleAccessibility))
                        .setCancelable(true)
                        .setPositiveButton(android.R.string.ok, this)
                        .setNegativeButton(android.R.string.cancel, this)
                        .create();
                }
                default: throw new IllegalArgumentException();
            }
        }

        private void setRequirePasswordState(boolean required) {
            mPasswordRequired = required;
            mRequirePasswordToDecryptButton.setChecked(required);
            mDontRequirePasswordToDecryptButton.setChecked(!required);

            // Updates value returned by SettingsActivity.onActivityResult().
            SettingsActivity sa = (SettingsActivity)getActivity();
            Intent resultIntentData = sa.getResultIntentData();
            if (resultIntentData == null) {
                resultIntentData = new Intent();
                sa.setResultIntentData(resultIntentData);
            }
            resultIntentData.putExtra(EXTRA_REQUIRE_PASSWORD, mPasswordRequired);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                setRequirePasswordState(true);
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                setRequirePasswordState(false);
            }
        }
    }
}
