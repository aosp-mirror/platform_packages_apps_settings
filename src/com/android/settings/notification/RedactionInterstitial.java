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

import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_SECURE_NOTIFICATIONS;
import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS;
import static android.app.admin.DevicePolicyResources.Strings.Settings.LOCK_SCREEN_HIDE_WORK_NOTIFICATION_CONTENT;
import static android.app.admin.DevicePolicyResources.Strings.Settings.LOCK_SCREEN_SHOW_WORK_NOTIFICATION_CONTENT;
import static android.provider.Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS;
import static android.provider.Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.RestrictedRadioButton;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SetupRedactionInterstitial;
import com.android.settings.SetupWizardUtils;
import com.android.settings.Utils;
import com.android.settingslib.RestrictedLockUtilsInternal;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.util.ThemeHelper;

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

    @Override
    protected void onCreate(Bundle savedInstance) {
        setTheme(SetupWizardUtils.getTheme(this, getIntent()));
        ThemeHelper.trySetDynamicColor(this);
        super.onCreate(savedInstance);
        findViewById(R.id.content_parent).setFitsSystemWindows(false);
    }

    @Override
    protected boolean isToolbarEnabled() {
        return false;
    }

    /**
     * Create an intent for launching RedactionInterstitial.
     *
     * @return An intent to launch the activity is if is available, @null if the activity is not
     * available to be launched.
     */
    public static Intent createStartIntent(Context ctx, int userId) {
        return new Intent(ctx, RedactionInterstitial.class)
                .putExtra(EXTRA_SHOW_FRAGMENT_TITLE_RESID,
                        UserManager.get(ctx).isManagedProfile(userId)
                                ? R.string.lock_screen_notifications_interstitial_title_profile
                                : R.string.lock_screen_notifications_interstitial_title)
                .putExtra(Intent.EXTRA_USER_ID, userId);
    }

    public static class RedactionInterstitialFragment extends SettingsPreferenceFragment
            implements RadioGroup.OnCheckedChangeListener {

        private RadioGroup mRadioGroup;
        private RestrictedRadioButton mShowAllButton;
        private RestrictedRadioButton mRedactSensitiveButton;
        private int mUserId;

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.NOTIFICATION_REDACTION;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return inflater.inflate(R.layout.redaction_interstitial, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            DevicePolicyManager devicePolicyManager = getSystemService(DevicePolicyManager.class);
            mRadioGroup = (RadioGroup) view.findViewById(R.id.radio_group);
            mShowAllButton = (RestrictedRadioButton) view.findViewById(R.id.show_all);
            mRedactSensitiveButton =
                    (RestrictedRadioButton) view.findViewById(R.id.redact_sensitive);

            mRadioGroup.setOnCheckedChangeListener(this);
            mUserId = Utils.getUserIdFromBundle(
                    getContext(), getActivity().getIntent().getExtras());
            if (UserManager.get(getContext()).isManagedProfile(mUserId)) {
                ((TextView) view.findViewById(R.id.sud_layout_description))
                        .setText(R.string.lock_screen_notifications_interstitial_message_profile);
                mShowAllButton.setText(devicePolicyManager
                        .getResources().getString(LOCK_SCREEN_SHOW_WORK_NOTIFICATION_CONTENT,
                                () -> getString(
                                        R.string.lock_screen_notifications_summary_show_profile)));
                mRedactSensitiveButton
                        .setText(devicePolicyManager.getResources().getString(
                                LOCK_SCREEN_HIDE_WORK_NOTIFICATION_CONTENT,
                                () -> getString(
                                        R.string.lock_screen_notifications_summary_hide_profile)));

                ((RadioButton) view.findViewById(R.id.hide_all)).setVisibility(View.GONE);
            }

            final GlifLayout layout = view.findViewById(R.id.setup_wizard_layout);
            final FooterBarMixin mixin = layout.getMixin(FooterBarMixin.class);
            mixin.setPrimaryButton(
                    new FooterButton.Builder(getContext())
                            .setText(R.string.app_notifications_dialog_done)
                            .setListener(this::onDoneButtonClicked)
                            .setButtonType(FooterButton.ButtonType.NEXT)
                            .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                            .build()
            );
        }

        private void onDoneButtonClicked(View view) {
            // If the activity starts by Setup Wizard, then skip disable component which avoids the
            // framework force closing all activities on the same task when the system is busy.
            if (!WizardManagerHelper.isAnySetupWizard(getIntent())) {
                SetupRedactionInterstitial.setEnabled(getContext(), false);
            }
            final RedactionInterstitial activity = (RedactionInterstitial) getActivity();
            if (activity != null) {
                activity.setResult(RESULT_CANCELED, null);
                finish();
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            // Disable buttons according to policy.

            checkNotificationFeaturesAndSetDisabled(mShowAllButton,
                    KEYGUARD_DISABLE_SECURE_NOTIFICATIONS |
                            KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS);
            checkNotificationFeaturesAndSetDisabled(mRedactSensitiveButton,
                    KEYGUARD_DISABLE_SECURE_NOTIFICATIONS);
            loadFromSettings();
        }

        private void checkNotificationFeaturesAndSetDisabled(RestrictedRadioButton button,
                int keyguardNotifications) {
            EnforcedAdmin admin = RestrictedLockUtilsInternal.checkIfKeyguardFeaturesDisabled(
                    getActivity(), keyguardNotifications, mUserId);
            button.setDisabledByAdmin(admin);
        }

        private void loadFromSettings() {
            final boolean showUnRedactedDefault = getContext().getResources().getBoolean(
                    R.bool.default_allow_sensitive_lockscreen_content);
            final boolean managedProfile = UserManager.get(getContext()).isManagedProfile(mUserId);
            // Hiding all notifications is device-wide setting, managed profiles can only set
            // whether their notifications are show in full or redacted.
            final boolean showNotifications = managedProfile || Settings.Secure.getIntForUser(
                    getContentResolver(), LOCK_SCREEN_SHOW_NOTIFICATIONS, 0, mUserId) != 0;
            final boolean showUnredacted = Settings.Secure.getIntForUser(
                    getContentResolver(), LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS,
                    showUnRedactedDefault ? 1 : 0, mUserId) != 0;

            int checkedButtonId = R.id.hide_all;
            if (showNotifications) {
                if (showUnredacted && !mShowAllButton.isDisabledByAdmin()) {
                    checkedButtonId = R.id.show_all;
                } else if (!mRedactSensitiveButton.isDisabledByAdmin()) {
                    checkedButtonId = R.id.redact_sensitive;
                }
            }

            mRadioGroup.check(checkedButtonId);
        }

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            final boolean show = (checkedId == R.id.show_all);
            final boolean enabled = (checkedId != R.id.hide_all);

            Settings.Secure.putIntForUser(getContentResolver(),
                    LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, show ? 1 : 0, mUserId);
            Settings.Secure.putIntForUser(getContentResolver(),
                    LOCK_SCREEN_SHOW_NOTIFICATIONS, enabled ? 1 : 0, mUserId);

        }
    }
}
