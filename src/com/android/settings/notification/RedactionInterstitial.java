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

package com.android.settings.notification;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

public class RedactionInterstitial extends SettingsActivity {

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, RedactionInterstitialFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return RedactionInterstitialFragment.class.getName().equals(fragmentName);
    }

    public static Intent createStartIntent(Context ctx) {
        return new Intent(ctx, RedactionInterstitial.class)
                .putExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, true)
                .putExtra(EXTRA_PREFS_SET_BACK_TEXT, (String) null)
                .putExtra(EXTRA_PREFS_SET_NEXT_TEXT, ctx.getString(
                        R.string.app_notifications_dialog_done));
    }

    public static class RedactionInterstitialFragment extends SettingsPreferenceFragment
            implements View.OnClickListener {

        private RadioButton mShowAllButton;
        private RadioButton mRedactSensitiveButton;
        private RadioButton mHideAllButton;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.redaction_interstitial, container, false);
            mShowAllButton = (RadioButton) view.findViewById(R.id.show_all);
            mRedactSensitiveButton = (RadioButton) view.findViewById(R.id.redact_sensitive);
            mHideAllButton = (RadioButton) view.findViewById(R.id.hide_all);

            mShowAllButton.setOnClickListener(this);
            mRedactSensitiveButton.setOnClickListener(this);
            mHideAllButton.setOnClickListener(this);
            return view;
        }

        @Override
        public void onResume() {
            super.onResume();
            loadFromSettings();
        }

        private void loadFromSettings() {
            final boolean enabled = Settings.Secure.getInt(getContentResolver(),
                        Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 0) != 0;
            final boolean show = Settings.Secure.getInt(getContentResolver(),
                        Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 1) != 0;
            mShowAllButton.setChecked(enabled && show);
            mRedactSensitiveButton.setChecked(enabled && !show);
            mHideAllButton.setChecked(!enabled);
        }

        @Override
        public void onClick(View v) {
            final boolean show = (v == mShowAllButton);
            final boolean enabled = (v != mHideAllButton);

            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, show ? 1 : 0);
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, enabled ? 1 : 0);
        }
    }
}
