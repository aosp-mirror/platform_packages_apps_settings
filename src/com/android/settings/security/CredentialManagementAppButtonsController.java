/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.security;

import android.app.Dialog;
import android.app.admin.DevicePolicyEventLogger;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.stats.devicepolicy.DevicePolicyEnums;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.widget.ActionButtonsPreference;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller that shows the remove button of the credential management app, which allows the user
 * to remove the credential management app and its certificates.
 */
public class CredentialManagementAppButtonsController extends BasePreferenceController {

    private static final String TAG = "CredentialManagementApp";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mHasCredentialManagerPackage;
    private Fragment mFragment;
    private final int mRemoveIcon;

    public CredentialManagementAppButtonsController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        if (context.getResources().getConfiguration().getLayoutDirection()
                == View.LAYOUT_DIRECTION_RTL) {
            mRemoveIcon = R.drawable.ic_redo_24;
        } else {
            mRemoveIcon = R.drawable.ic_undo_24;
        }
    }

    public void setParentFragment(Fragment fragment) {
        mFragment = fragment;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mExecutor.execute(() -> {
            try {
                IKeyChainService service = KeyChain.bind(mContext).getService();
                mHasCredentialManagerPackage = service.hasCredentialManagementApp();
            } catch (InterruptedException | RemoteException e) {
                Log.e(TAG, "Unable to display credential management app buttons");
            }
            mHandler.post(() -> displayButtons(screen));
        });
    }

    private void displayButtons(PreferenceScreen screen) {
        if (mHasCredentialManagerPackage) {
            ((ActionButtonsPreference) screen.findPreference(getPreferenceKey()))
                    .setButton1Text(R.string.uninstall_certs_credential_management_app)
                    .setButton1Icon(R.drawable.ic_upload)
                    .setButton1OnClickListener(view -> uninstallCertificates())
                    .setButton2Text(R.string.remove_credential_management_app)
                    .setButton2Icon(mRemoveIcon)
                    .setButton2OnClickListener(view -> showRemoveCredentialManagementAppDialog());
        }
    }

    private void uninstallCertificates() {
        mExecutor.execute(() -> {
            try {
                IKeyChainService service = KeyChain.bind(mContext).getService();
                for (String existingAlias :
                        service.getCredentialManagementAppPolicy().getAliases()) {
                    service.removeKeyPair(existingAlias);
                }
            } catch (InterruptedException | RemoteException e) {
                Log.e(TAG, "Unable to uninstall certificates");
            }
        });
    }

    private void showRemoveCredentialManagementAppDialog() {
        final RemoveCredentialManagementAppDialog dialog =
                RemoveCredentialManagementAppDialog.newInstance();
        dialog.show(mFragment.getParentFragmentManager(),
                RemoveCredentialManagementAppDialog.class.getName());
    }

    /**
     * Implements an AlertDialog for confirming that a user wants to remove the credential
     * management app. The app will no longer be able to manage certificates, but it will stay on
     * the device. All certificates installed by the credential management app will be uninstalled.
     */
    public static class RemoveCredentialManagementAppDialog extends InstrumentedDialogFragment {

        public static RemoveCredentialManagementAppDialog newInstance() {
            return new RemoveCredentialManagementAppDialog();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getContext(), R.style.Theme_AlertDialog)
                    .setTitle(R.string.remove_credential_management_app_dialog_title)
                    .setMessage(R.string.remove_credential_management_app_dialog_message)
                    .setPositiveButton(R.string.remove_credential_management_app,
                            (dialog, which) -> removeCredentialManagementApp())
                    .setNegativeButton(R.string.cancel, (dialog, which) -> dismiss())
                    .create();
        }

        private void removeCredentialManagementApp() {
            final ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    IKeyChainService service = KeyChain.bind(getContext()).getService();
                    service.removeCredentialManagementApp();
                    DevicePolicyEventLogger
                            .createEvent(DevicePolicyEnums.CREDENTIAL_MANAGEMENT_APP_REMOVED)
                            .write();
                    getParentFragment().getActivity().finish();
                } catch (InterruptedException | RemoteException e) {
                    Log.e(TAG, "Unable to remove the credential management app");
                }
            });
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.CREDENTIAL_MANAGEMENT_APP_REMOVE_APP;
        }
    }
}