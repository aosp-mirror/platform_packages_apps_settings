/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.biometrics;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/** Initializes and shows biometric error dialogs related to identity check. */
public class IdentityCheckBiometricErrorDialog extends InstrumentedDialogFragment {
    private static final String TAG = "BiometricErrorDialog";

    private static final String KEY_ERROR_CODE = "key_error_code";
    private static final String KEY_TWO_FACTOR_AUTHENTICATION = "key_two_factor_authentication";
    private static final String KEY_FINISH_ACTIVITY = "key_finish_activity";

    private String mActionIdentityCheckSettings = Settings.ACTION_SETTINGS;
    private String mIdentityCheckSettingsPackageName;
    @Nullable private BroadcastReceiver mBroadcastReceiver;
    private boolean mShouldFinishActivity = false;

    @NonNull
    @Override
    public Dialog onCreateDialog(
            @Nullable Bundle savedInstanceState) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final boolean isLockoutError = getArguments().getString(KEY_ERROR_CODE).equals(
                Utils.BiometricStatus.LOCKOUT.name());
        final View customView = inflater.inflate(R.layout.biometric_lockout_error_dialog,
                null);
        final boolean twoFactorAuthentication = getArguments().getBoolean(
                KEY_TWO_FACTOR_AUTHENTICATION);
        final String identityCheckSettingsAction = getActivity().getString(
                com.android.internal.R.string.identity_check_settings_action);
        mActionIdentityCheckSettings = identityCheckSettingsAction.isEmpty()
                ? mActionIdentityCheckSettings : identityCheckSettingsAction;
        mIdentityCheckSettingsPackageName = getActivity().getString(
                com.android.internal.R.string.identity_check_settings_package_name);
        mShouldFinishActivity = getArguments().getBoolean(
                KEY_FINISH_ACTIVITY);

        setTitle(customView, isLockoutError);
        setBody(customView, isLockoutError, twoFactorAuthentication);
        alertDialogBuilder.setView(customView);
        setPositiveButton(alertDialogBuilder, isLockoutError, twoFactorAuthentication);
        if (!isLockoutError || !twoFactorAuthentication) {
            setNegativeButton(alertDialogBuilder, isLockoutError);
        }
        if (isLockoutError) {
            mBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                        dismiss();
                    }
                }
            };
            getContext().registerReceiver(mBroadcastReceiver,
                    new IntentFilter(Intent.ACTION_SCREEN_OFF));
        }

        return alertDialogBuilder.create();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mBroadcastReceiver != null) {
            getContext().unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
        if (mShouldFinishActivity && getActivity() != null) {
            getActivity().finish();
        }
    }

    /**
     * Shows an error dialog to prompt the user to resolve biometric errors for identity check.
     * @param fragmentActivity calling activity
     * @param errorCode refers to the biometric error
     * @param twoFactorAuthentication if the surface requests LSKF before identity check auth
     */
    public static void showBiometricErrorDialog(FragmentActivity fragmentActivity,
            Utils.BiometricStatus errorCode, boolean twoFactorAuthentication) {
        showBiometricErrorDialog(fragmentActivity, errorCode, twoFactorAuthentication,
                false /* finishActivityOnDismiss */);
    }

    /**
     * Shows an error dialog to prompt the user to resolve biometric errors for identity check.
     * Finishes the activity once the dialog is dismissed.
     * @param fragmentActivity calling activity
     * @param errorCode refers to the biometric error
     */
    public static void showBiometricErrorDialogAndFinishActivityOnDismiss(
            FragmentActivity fragmentActivity, Utils.BiometricStatus errorCode) {
        showBiometricErrorDialog(fragmentActivity, errorCode, true /* twoFactorAuthentication */,
                true /* finishActivityOnDismiss */);
    }

    private static void showBiometricErrorDialog(FragmentActivity fragmentActivity,
            Utils.BiometricStatus errorCode, boolean twoFactorAuthentication,
            boolean finishActivityOnDismiss) {
        final IdentityCheckBiometricErrorDialog identityCheckBiometricErrorDialog =
                new IdentityCheckBiometricErrorDialog();
        final Bundle args = new Bundle();
        args.putCharSequence(KEY_ERROR_CODE, errorCode.name());
        args.putBoolean(KEY_TWO_FACTOR_AUTHENTICATION, twoFactorAuthentication);
        args.putBoolean(KEY_FINISH_ACTIVITY, finishActivityOnDismiss);
        identityCheckBiometricErrorDialog.setArguments(args);
        identityCheckBiometricErrorDialog.show(fragmentActivity.getSupportFragmentManager(),
                IdentityCheckBiometricErrorDialog.class.getName());
    }
    private void setTitle(View view, boolean lockout) {
        final TextView titleTextView = view.findViewById(R.id.title);
        if (lockout) {
            titleTextView.setText(R.string.identity_check_lockout_error_title);
        } else {
            titleTextView.setText(R.string.identity_check_general_error_title);
        }
    }

    private void setBody(View view, boolean lockout, boolean twoFactorAuthentication) {
        final TextView textView1 = view.findViewById(R.id.description_1);
        final TextView textView2 = view.findViewById(R.id.description_2);

        if (lockout) {
            if (twoFactorAuthentication) {
                textView1.setText(
                        R.string.identity_check_lockout_error_two_factor_auth_description_1);
            } else {
                textView1.setText(R.string.identity_check_lockout_error_description_1);
            }
            textView2.setText(getClickableDescriptionForLockoutError());
            textView2.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            textView1.setText(R.string.identity_check_general_error_description_1);
            textView2.setVisibility(View.GONE);
        }
    }

    private SpannableString getClickableDescriptionForLockoutError() {
        final String description = getResources().getString(
                R.string.identity_check_lockout_error_description_2);
        final SpannableString spannableString = new SpannableString(description);
        final ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                dismiss();
                final Intent autoLockSettingsIntent = new Intent(mActionIdentityCheckSettings);
                final ResolveInfo autoLockSettingsInfo = getActivity().getPackageManager()
                        .resolveActivity(autoLockSettingsIntent, 0 /* flags */);
                if (autoLockSettingsInfo != null) {
                    startActivity(autoLockSettingsIntent);
                } else {
                    Log.e(TAG, "Auto lock settings intent could not be resolved.");
                }
            }
            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
            }
        };
        final String goToSettings = getActivity().getString(R.string.go_to_settings);
        spannableString.setSpan(clickableSpan, description.indexOf(goToSettings),
                description.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannableString;
    }

    private void setPositiveButton(AlertDialog.Builder alertDialogBuilder, boolean lockout,
            boolean twoFactorAuthentication) {
        if (lockout) {
            if (twoFactorAuthentication) {
                alertDialogBuilder.setPositiveButton((R.string.okay),
                        (dialog, which) -> dialog.dismiss());
            } else {
                DevicePolicyManager devicePolicyManager = (DevicePolicyManager)
                        getContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
                alertDialogBuilder.setPositiveButton(
                        R.string.identity_check_lockout_error_lock_screen,
                        (dialog, which) -> {
                            dialog.dismiss();
                            devicePolicyManager.lockNow();
                        });
            }
        } else {
            alertDialogBuilder.setPositiveButton(R.string.identity_check_biometric_error_ok,
                    (dialog, which) -> dialog.dismiss());
        }
    }

    private void setNegativeButton(AlertDialog.Builder alertDialogBuilder, boolean lockout) {
        if (lockout) {
            alertDialogBuilder.setNegativeButton(R.string.identity_check_biometric_error_cancel,
                    (dialog, which) -> dialog.dismiss());
        } else {
            alertDialogBuilder.setNegativeButton(R.string.go_to_identity_check,
                    (dialog, which) -> {
                        final Intent autoLockSettingsIntent = new Intent(
                                mActionIdentityCheckSettings);
                        final ResolveInfo autoLockSettingsInfo = getActivity().getPackageManager()
                                .resolveActivity(autoLockSettingsIntent, 0 /* flags */);
                        if (autoLockSettingsInfo != null) {
                            startActivity(autoLockSettingsIntent);
                        } else {
                            Log.e(TAG, "Identity check settings intent could not be resolved.");
                        }
                    });
        }
    }

    @Override
    public int getMetricsCategory() {
        return 0;
    }
}
