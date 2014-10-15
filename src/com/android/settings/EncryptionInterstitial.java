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

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

public class EncryptionInterstitial extends SettingsActivity {

    private static final String EXTRA_PASSWORD_QUALITY = "extra_password_quality";

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

    public static Intent createStartIntent(Context ctx, int quality) {
        return new Intent(ctx, EncryptionInterstitial.class)
                .putExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, true)
                .putExtra(EXTRA_PREFS_SET_BACK_TEXT, (String) null)
                .putExtra(EXTRA_PREFS_SET_NEXT_TEXT, ctx.getString(
                        R.string.encryption_continue_button))
                .putExtra(EXTRA_PASSWORD_QUALITY, quality)
                .putExtra(EXTRA_SHOW_FRAGMENT_TITLE_RESID, R.string.encryption_interstitial_header);
    }

    public static class EncryptionInterstitialFragment extends SettingsPreferenceFragment
            implements View.OnClickListener {

        private RadioButton mRequirePasswordToDecryptButton;
        private RadioButton mDontRequirePasswordToDecryptButton;
        private TextView mEncryptionMessage;

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
            return view;
        }

        @Override
        public void onResume() {
            super.onResume();
            loadFromSettings();
        }

        private void loadFromSettings() {
            final boolean required = Settings.Global.getInt(getContentResolver(),
                    Settings.Global.REQUIRE_PASSWORD_TO_DECRYPT, 1) == 1 ? true : false;
            mRequirePasswordToDecryptButton.setChecked(required);
            mDontRequirePasswordToDecryptButton.setChecked(!required);
        }

        @Override
        public void onClick(View v) {
            final boolean required = (v == mRequirePasswordToDecryptButton);
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.REQUIRE_PASSWORD_TO_DECRYPT, required ? 1 : 0);
        }
    }
}
