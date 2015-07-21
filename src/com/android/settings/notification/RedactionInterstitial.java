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

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

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

    /**
     * Create an intent for launching RedactionInterstitial.
     * @return An intent to launch the activity is if is available, @null if the activity is not
     * available to be launched.
     */
    public static Intent createStartIntent(Context ctx) {
        if (isSecureNotificationsDisabled(ctx)) {
            // If there is no choices for the user, we should not start the activity.
            return null;
        } else {
            return new Intent(ctx, RedactionInterstitial.class)
                    .putExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, true)
                    .putExtra(EXTRA_PREFS_SET_BACK_TEXT, (String) null)
                    .putExtra(EXTRA_PREFS_SET_NEXT_TEXT, ctx.getString(
                            R.string.app_notifications_dialog_done))
                    .putExtra(EXTRA_SHOW_FRAGMENT_TITLE_RESID,
                            R.string.lock_screen_notifications_interstitial_title);
        }
    }

    private static boolean isSecureNotificationsDisabled(Context context) {
        final DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm != null && (dpm.getKeyguardDisabledFeatures(null)
                & DevicePolicyManager.KEYGUARD_DISABLE_SECURE_NOTIFICATIONS) != 0;
    }

    private static boolean isUnredactedNotificationsDisabled(Context context) {
        final DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm != null && (dpm.getKeyguardDisabledFeatures(null)
                & DevicePolicyManager.KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS) != 0;
    }

    public static class RedactionInterstitialFragment extends SettingsPreferenceFragment
            implements RadioGroup.OnCheckedChangeListener {

        private RadioGroup mRadioGroup;
        private RadioButton mShowAllButton;
        private RadioButton mRedactSensitiveButton;

        @Override
        protected int getMetricsCategory() {
            return MetricsLogger.NOTIFICATION_REDACTION;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return inflater.inflate(R.layout.redaction_interstitial, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mRadioGroup = (RadioGroup) view.findViewById(R.id.radio_group);
            mShowAllButton = (RadioButton) view.findViewById(R.id.show_all);
            mRedactSensitiveButton = (RadioButton) view.findViewById(R.id.redact_sensitive);

            mRadioGroup.setOnCheckedChangeListener(this);

            // Disable buttons according to policy.
            if (isSecureNotificationsDisabled(getActivity())) {
                mShowAllButton.setEnabled(false);
                mRedactSensitiveButton.setEnabled(false);
            } else if (isUnredactedNotificationsDisabled(getActivity())) {
                mShowAllButton.setEnabled(false);
            }
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

            int checkedButtonId = R.id.hide_all;
            if (enabled) {
                if (show && mShowAllButton.isEnabled()) {
                    checkedButtonId = R.id.show_all;
                } else if (mRedactSensitiveButton.isEnabled()) {
                    checkedButtonId = R.id.redact_sensitive;
                }
            }

            mRadioGroup.check(checkedButtonId);
        }

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            final boolean show = (checkedId == R.id.show_all);
            final boolean enabled = (checkedId != R.id.hide_all);

            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, show ? 1 : 0);
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, enabled ? 1 : 0);
        }
    }
}
