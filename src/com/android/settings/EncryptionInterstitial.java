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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.utils.SettingsDividerItemDecoration;
import com.android.setupwizardlib.GlifPreferenceLayout;

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
        LinearLayout layout = (LinearLayout) findViewById(R.id.content_parent);
        layout.setFitsSystemWindows(false);
    }

    public static class EncryptionInterstitialFragment extends SettingsPreferenceFragment
            implements DialogInterface.OnClickListener {

        private static final int ACCESSIBILITY_WARNING_DIALOG = 1;
        private static final String KEY_ENCRYPT_REQUIRE_PASSWORD = "encrypt_require_password";
        private static final String KEY_ENCRYPT_DONT_REQUIRE_PASSWORD =
                "encrypt_dont_require_password";

        private Preference mRequirePasswordToDecrypt;
        private Preference mDontRequirePasswordToDecrypt;
        private boolean mPasswordRequired;
        private Intent mUnlockMethodIntent;
        private int mRequestedPasswordQuality;

        @Override
        protected int getMetricsCategory() {
            return MetricsEvent.ENCRYPTION;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.security_settings_encryption_interstitial);

            // Used for testing purposes
            findPreference(KEY_ENCRYPT_DONT_REQUIRE_PASSWORD)
                    .setViewId(R.id.encrypt_dont_require_password);

            mRequirePasswordToDecrypt = findPreference(KEY_ENCRYPT_REQUIRE_PASSWORD);
            mDontRequirePasswordToDecrypt = findPreference(KEY_ENCRYPT_DONT_REQUIRE_PASSWORD);
            boolean forFingerprint = getActivity().getIntent().getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, false);
            Intent intent = getActivity().getIntent();
            mRequestedPasswordQuality = intent.getIntExtra(EXTRA_PASSWORD_QUALITY, 0);
            mUnlockMethodIntent = intent.getParcelableExtra(EXTRA_UNLOCK_METHOD_INTENT);
            final int msgId;
            final int enableId;
            final int disableId;
            switch (mRequestedPasswordQuality) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    msgId = forFingerprint ?
                            R.string.encryption_interstitial_message_pattern_for_fingerprint :
                            R.string.encryption_interstitial_message_pattern;
                    enableId = R.string.encrypt_require_pattern;
                    disableId = R.string.encrypt_dont_require_pattern;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                    msgId = forFingerprint ?
                            R.string.encryption_interstitial_message_pin_for_fingerprint :
                            R.string.encryption_interstitial_message_pin;
                    enableId = R.string.encrypt_require_pin;
                    disableId = R.string.encrypt_dont_require_pin;
                    break;
                default:
                    msgId = forFingerprint ?
                            R.string.encryption_interstitial_message_password_for_fingerprint :
                            R.string.encryption_interstitial_message_password;
                    enableId = R.string.encrypt_require_password;
                    disableId = R.string.encrypt_dont_require_password;
                    break;
            }
            TextView message = (TextView) LayoutInflater.from(getActivity()).inflate(
                    R.layout.encryption_interstitial_header, null, false);
            message.setText(msgId);
            setHeaderView(message);

            mRequirePasswordToDecrypt.setTitle(enableId);

            mDontRequirePasswordToDecrypt.setTitle(disableId);

            setRequirePasswordState(getActivity().getIntent().getBooleanExtra(
                    EXTRA_REQUIRE_PASSWORD, true));
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            GlifPreferenceLayout layout = (GlifPreferenceLayout) view;
            layout.setDividerItemDecoration(new SettingsDividerItemDecoration(getContext()));

            layout.setIcon(getContext().getDrawable(R.drawable.ic_lock));
            layout.setHeaderText(getActivity().getTitle());

            // Use the dividers in SetupWizardRecyclerLayout. Suppress the dividers in
            // PreferenceFragment.
            setDivider(null);
        }

        @Override
        public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent,
                Bundle savedInstanceState) {
            GlifPreferenceLayout layout = (GlifPreferenceLayout) parent;
            return layout.onCreateRecyclerView(inflater, parent, savedInstanceState);
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

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            final String key = preference.getKey();
            if (key.equals(KEY_ENCRYPT_REQUIRE_PASSWORD)) {
                final boolean accEn = AccessibilityManager.getInstance(getActivity()).isEnabled();
                if (accEn && !mPasswordRequired) {
                    setRequirePasswordState(false); // clear the UI state
                    showDialog(ACCESSIBILITY_WARNING_DIALOG);
                } else {
                    setRequirePasswordState(true);
                    startLockIntent();
                }
            } else {
                setRequirePasswordState(false);
                startLockIntent();
            }
            return true;
        }

        @Override
        public Dialog onCreateDialog(int dialogId) {
            switch(dialogId) {
                case ACCESSIBILITY_WARNING_DIALOG: {
                    final int titleId;
                    final int messageId;
                    switch (mRequestedPasswordQuality) {
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
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                setRequirePasswordState(true);
                startLockIntent();
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                setRequirePasswordState(false);
            }
        }
    }
}
