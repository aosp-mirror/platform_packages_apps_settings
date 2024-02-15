/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.accounts;

import static android.app.admin.DevicePolicyResources.Strings.Settings.REMOVE_ACCOUNT_FAILED_ADMIN_RESTRICTION;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.RestrictedButton;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.LayoutPreference;

import java.io.IOException;

public class RemoveAccountPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, OnClickListener {

    private static final String TAG = "RemoveAccountPrefController";
    private static final String KEY_REMOVE_ACCOUNT = "remove_account";

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private Account mAccount;
    private Fragment mParentFragment;
    private UserHandle mUserHandle;
    private LayoutPreference mRemoveAccountPreference;
    private RestrictedButton mRemoveAccountButton;

    public RemoveAccountPreferenceController(Context context, Fragment parent) {
        super(context);
        mParentFragment = parent;
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mRemoveAccountPreference = screen.findPreference(KEY_REMOVE_ACCOUNT);
        mRemoveAccountButton = mRemoveAccountPreference.findViewById(R.id.button);
        mRemoveAccountButton.setOnClickListener(this);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        mRemoveAccountButton.updateState();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_REMOVE_ACCOUNT;
    }

    @Override
    public void onClick(View v) {
        mMetricsFeatureProvider.logClickedPreference(mRemoveAccountPreference,
                mMetricsFeatureProvider.getMetricsCategory(mParentFragment));
        ConfirmRemoveAccountDialog.show(mParentFragment, mAccount, mUserHandle);
    }

    public void init(Account account, UserHandle userHandle) {
        mAccount = account;
        mUserHandle = userHandle;
        mRemoveAccountButton.init(mUserHandle, UserManager.DISALLOW_MODIFY_ACCOUNTS);
    }

    /**
     * Dialog to confirm with user about account removal
     */
    public static class ConfirmRemoveAccountDialog extends InstrumentedDialogFragment implements
            DialogInterface.OnClickListener {
        private static final String KEY_ACCOUNT = "account";
        private static final String REMOVE_ACCOUNT_DIALOG = "confirmRemoveAccount";
        private Account mAccount;
        private UserHandle mUserHandle;

        public static ConfirmRemoveAccountDialog show(
                Fragment parent, Account account, UserHandle userHandle) {
            if (!parent.isAdded()) {
                return null;
            }
            final ConfirmRemoveAccountDialog dialog = new ConfirmRemoveAccountDialog();
            Bundle bundle = new Bundle();
            bundle.putParcelable(KEY_ACCOUNT, account);
            bundle.putParcelable(Intent.EXTRA_USER, userHandle);
            dialog.setArguments(bundle);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), REMOVE_ACCOUNT_DIALOG);
            return dialog;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final Bundle arguments = getArguments();
            mAccount = arguments.getParcelable(KEY_ACCOUNT);
            mUserHandle = arguments.getParcelable(Intent.EXTRA_USER);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            return new AlertDialog.Builder(context)
                    .setTitle(R.string.really_remove_account_title)
                    .setMessage(R.string.really_remove_account_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.remove_account_label, this)
                    .create();
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_ACCOUNT_SYNC_REMOVE;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            Activity activity = getTargetFragment().getActivity();
            AccountManager.get(activity).removeAccountAsUser(mAccount, activity,
                    future -> {
                        final Activity targetActivity = getTargetFragment().getActivity();
                        if (targetActivity == null || targetActivity.isFinishing()) {
                            Log.w(TAG, "Activity is no longer alive, skipping results");
                            return;
                        }
                        boolean failed = true;
                        try {
                            if (future.getResult()
                                    .getBoolean(AccountManager.KEY_BOOLEAN_RESULT)) {
                                failed = false;
                            }
                        } catch (OperationCanceledException
                                | IOException
                                | AuthenticatorException e) {
                            // handled below
                            Log.w(TAG, "Remove account error: " + e);
                        }
                        Log.i(TAG, "failed: " + failed);
                        if (failed) {
                            RemoveAccountFailureDialog.show(getTargetFragment());
                        } else {
                            targetActivity.finish();
                        }
                    }, null, mUserHandle);
        }
    }

    /**
     * Dialog to tell user about account removal failure
     */
    public static class RemoveAccountFailureDialog extends InstrumentedDialogFragment {

        private static final String FAILED_REMOVAL_DIALOG = "removeAccountFailed";

        public static void show(Fragment parent) {
            if (!parent.isAdded()) {
                return;
            }
            final RemoveAccountFailureDialog dialog = new RemoveAccountFailureDialog();
            dialog.setTargetFragment(parent, 0);
            try {
                dialog.show(parent.getFragmentManager(), FAILED_REMOVAL_DIALOG);
            } catch (IllegalStateException e) {
                Log.w(TAG, "Can't show RemoveAccountFailureDialog. " +  e.getMessage());
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            return new AlertDialog.Builder(context)
                    .setTitle(R.string.remove_account_label)
                    .setMessage(getContext().getSystemService(DevicePolicyManager.class)
                            .getResources()
                            .getString(REMOVE_ACCOUNT_FAILED_ADMIN_RESTRICTION,
                                    () -> getString(R.string.remove_account_failed)))
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_ACCOUNT_SYNC_FAILED_REMOVAL;
        }

    }
}
