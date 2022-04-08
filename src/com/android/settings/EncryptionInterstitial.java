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

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.core.InstrumentedFragment;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.password.ChooseLockSettingsHelper;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

import java.util.List;

public class EncryptionInterstitial extends SettingsActivity {
    private static final String TAG = EncryptionInterstitial.class.getSimpleName();

    protected static final String EXTRA_PASSWORD_QUALITY = "extra_password_quality";
    protected static final String EXTRA_UNLOCK_METHOD_INTENT = "extra_unlock_method_intent";
    public static final String EXTRA_REQUIRE_PASSWORD = "extra_require_password";
    private static final int CHOOSE_LOCK_REQUEST = 100;

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, EncryptionInterstitialFragment.class.getName());
        return modIntent;
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        resid = SetupWizardUtils.getTheme(getIntent());
        super.onApplyThemeResource(theme, resid, first);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return EncryptionInterstitialFragment.class.getName().equals(fragmentName);
    }

    public static Intent createStartIntent(Context ctx, int quality,
            boolean requirePasswordDefault, Intent unlockMethodIntent) {
        return new Intent(ctx, EncryptionInterstitial.class)
                .putExtra(EXTRA_PASSWORD_QUALITY, quality)
                .putExtra(EXTRA_SHOW_FRAGMENT_TITLE_RESID, R.string.encryption_interstitial_header)
                .putExtra(EXTRA_REQUIRE_PASSWORD, requirePasswordDefault)
                .putExtra(EXTRA_UNLOCK_METHOD_INTENT, unlockMethodIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        findViewById(R.id.content_parent).setFitsSystemWindows(false);
    }

    public static class EncryptionInterstitialFragment extends InstrumentedFragment {

        private boolean mPasswordRequired;
        private Intent mUnlockMethodIntent;
        private int mRequestedPasswordQuality;

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.ENCRYPTION;
        }

        @Override
        public View onCreateView(
                LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.encryption_interstitial, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            final boolean forFingerprint = getActivity().getIntent().getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, false);
            final boolean forFace = getActivity().getIntent()
                    .getBooleanExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FACE, false);
            Intent intent = getActivity().getIntent();
            mRequestedPasswordQuality = intent.getIntExtra(EXTRA_PASSWORD_QUALITY, 0);
            mUnlockMethodIntent = intent.getParcelableExtra(EXTRA_UNLOCK_METHOD_INTENT);
            final int msgId;
            switch (mRequestedPasswordQuality) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    msgId = forFingerprint ?
                            R.string.encryption_interstitial_message_pattern_for_fingerprint :
                            forFace ?
                            R.string.encryption_interstitial_message_pattern_for_face :
                            R.string.encryption_interstitial_message_pattern;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                    msgId = forFingerprint ?
                            R.string.encryption_interstitial_message_pin_for_fingerprint :
                            forFace ?
                            R.string.encryption_interstitial_message_pin_for_face :
                            R.string.encryption_interstitial_message_pin;
                    break;
                default:
                    msgId = forFingerprint ?
                            R.string.encryption_interstitial_message_password_for_fingerprint :
                            forFace ?
                            R.string.encryption_interstitial_message_password_for_face :
                            R.string.encryption_interstitial_message_password;
                    break;
            }
            TextView message = (TextView) getActivity().findViewById(R.id.sud_layout_description);
            message.setText(msgId);

            setRequirePasswordState(getActivity().getIntent().getBooleanExtra(
                    EXTRA_REQUIRE_PASSWORD, true));

            GlifLayout layout = (GlifLayout) view;
            layout.setHeaderText(getActivity().getTitle());

            final FooterBarMixin mixin = layout.getMixin(FooterBarMixin.class);
            mixin.setSecondaryButton(
                    new FooterButton.Builder(getContext())
                            .setText(R.string.encryption_interstitial_no)
                            .setListener(this::onNoButtonClicked)
                            .setButtonType(FooterButton.ButtonType.SKIP)
                            .setTheme(R.style.SudGlifButton_Secondary)
                            .build()
            );

            mixin.setPrimaryButton(
                    new FooterButton.Builder(getContext())
                            .setText(R.string.encryption_interstitial_yes)
                            .setListener(this::onYesButtonClicked)
                            .setButtonType(FooterButton.ButtonType.NEXT)
                            .setTheme(R.style.SudGlifButton_Primary)
                            .build()
            );
        }

        protected void startLockIntent() {
            if (mUnlockMethodIntent != null) {
                mUnlockMethodIntent.putExtra(EXTRA_REQUIRE_PASSWORD, mPasswordRequired);
                startActivityForResult(mUnlockMethodIntent, CHOOSE_LOCK_REQUEST);
            } else {
                Log.wtf(TAG, "no unlock intent to start");
                finish();
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == CHOOSE_LOCK_REQUEST && resultCode != RESULT_CANCELED) {
                getActivity().setResult(resultCode, data);
                finish();
            }
        }

        private void onYesButtonClicked(View view) {
            final boolean accEn = AccessibilityManager.getInstance(getActivity()).isEnabled();
            if (accEn && !mPasswordRequired) {
                setRequirePasswordState(false); // clear the UI state
                AccessibilityWarningDialogFragment.newInstance(mRequestedPasswordQuality)
                        .show(
                                getChildFragmentManager(),
                                AccessibilityWarningDialogFragment.TAG);
            } else {
                setRequirePasswordState(true);
                startLockIntent();
            }
        }

        private void onNoButtonClicked(View view) {
            setRequirePasswordState(false);
            startLockIntent();
        }

        private void setRequirePasswordState(boolean required) {
            mPasswordRequired = required;
        }

        public void finish() {
            Activity activity = getActivity();
            if (activity == null) return;
            if (getFragmentManager().getBackStackEntryCount() > 0) {
                getFragmentManager().popBackStack();
            } else {
                activity.finish();
            }
        }
    }

    public static class AccessibilityWarningDialogFragment extends InstrumentedDialogFragment
            implements DialogInterface.OnClickListener {

        public static final String TAG = "AccessibilityWarningDialog";

        public static AccessibilityWarningDialogFragment newInstance(int passwordQuality) {
            AccessibilityWarningDialogFragment fragment = new AccessibilityWarningDialogFragment();
            Bundle args = new Bundle(1);
            args.putInt(EXTRA_PASSWORD_QUALITY, passwordQuality);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final int titleId;
            final int messageId;
            switch (getArguments().getInt(EXTRA_PASSWORD_QUALITY)) {
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


            final Activity activity = getActivity();
            List<AccessibilityServiceInfo> list =
                    AccessibilityManager.getInstance(activity)
                            .getEnabledAccessibilityServiceList(
                                    AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            final CharSequence exampleAccessibility;
            if (list.isEmpty()) {
                // This should never happen.  But we shouldn't crash
                exampleAccessibility = "";
            } else {
                exampleAccessibility = list.get(0).getResolveInfo()
                        .loadLabel(activity.getPackageManager());
            }
            return new AlertDialog.Builder(activity)
                    .setTitle(titleId)
                    .setMessage(getString(messageId, exampleAccessibility))
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .create();
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_ENCRYPTION_INTERSTITIAL_ACCESSIBILITY;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            EncryptionInterstitialFragment fragment =
                    (EncryptionInterstitialFragment) getParentFragment();
            if (fragment != null) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    fragment.setRequirePasswordState(true);
                    fragment.startLockIntent();
                } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                    fragment.setRequirePasswordState(false);
                }
            }
        }
    }
}
