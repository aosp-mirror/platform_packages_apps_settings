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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.admin.DevicePolicyManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.PreferenceScreen;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.enterprise.DevicePolicyManagerWrapper;
import com.android.settings.enterprise.DevicePolicyManagerWrapperImpl;
import com.android.settingslib.core.AbstractPreferenceController;

import java.io.IOException;

public class RemoveAccountPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, OnClickListener {

    private static final String KEY_REMOVE_ACCOUNT = "remove_account";

    private Account mAccount;
    private Fragment mParentFragment;
    private UserHandle mUserHandle;
    private DevicePolicyManagerWrapper mDpm;

    public RemoveAccountPreferenceController(Context context, Fragment parent) {
        this(context, parent, new DevicePolicyManagerWrapperImpl(
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE)));
    }

    @VisibleForTesting
    RemoveAccountPreferenceController(Context context, Fragment parent,
            DevicePolicyManagerWrapper dpm) {
        super(context);
        mParentFragment = parent;
        mDpm = dpm;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final LayoutPreference removeAccountPreference =
            (LayoutPreference) screen.findPreference(KEY_REMOVE_ACCOUNT);
        Button removeAccountButton = (Button) removeAccountPreference.findViewById(R.id.button);
        removeAccountButton.setOnClickListener(this);
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
        final Intent intent = mDpm.createAdminSupportIntent(UserManager.DISALLOW_MODIFY_ACCOUNTS);
        if (intent != null) {
            // DISALLOW_MODIFY_ACCOUNTS is active, show admin support dialog
            mContext.startActivity(intent);
            return;
        }
        ConfirmRemoveAccountDialog.show(mParentFragment, mAccount, mUserHandle);
    }

    public void init(Account account, UserHandle userHandle) {
        mAccount = account;
        mUserHandle = userHandle;
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
            return MetricsProto.MetricsEvent.DIALOG_ACCOUNT_SYNC_REMOVE;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            Activity activity = getTargetFragment().getActivity();
            AccountManager.get(activity).removeAccountAsUser(mAccount, activity,
                    new AccountManagerCallback<Bundle>() {
                        @Override
                        public void run(AccountManagerFuture<Bundle> future) {
                            // If already out of this screen, don't proceed.
                            if (!getTargetFragment().isResumed()) {
                                return;
                            }
                            boolean failed = true;
                            try {
                                if (future.getResult()
                                    .getBoolean(AccountManager.KEY_BOOLEAN_RESULT)) {
                                    failed = false;
                                }
                            } catch (OperationCanceledException e) {
                                // handled below
                            } catch (IOException e) {
                                // handled below
                            } catch (AuthenticatorException e) {
                                // handled below
                            }
                            final Activity activity = getTargetFragment().getActivity();
                            if (failed && activity != null && !activity.isFinishing()) {
                                RemoveAccountFailureDialog.show(getTargetFragment());
                            } else {
                                activity.finish();
                            }
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
            dialog.show(parent.getFragmentManager(), FAILED_REMOVAL_DIALOG);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            return new AlertDialog.Builder(context)
                .setTitle(R.string.really_remove_account_title)
                .setMessage(R.string.remove_account_failed)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        }

        @Override
        public int getMetricsCategory() {
            return MetricsProto.MetricsEvent.DIALOG_ACCOUNT_SYNC_FAILED_REMOVAL;
        }

    }
}
