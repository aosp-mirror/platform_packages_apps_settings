/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.security.KeyChain.KeyChainConnection;
import android.security.KeyChain;
import android.security.KeyStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.widget.LockPatternUtils;

public class CredentialStorage extends Activity {

    private static final String TAG = "CredentialStorage";

    public static final String ACTION_UNLOCK = "com.android.credentials.UNLOCK";
    public static final String ACTION_INSTALL = "com.android.credentials.INSTALL";
    public static final String ACTION_RESET = "com.android.credentials.RESET";

    // This is the minimum acceptable password quality.  If the current password quality is
    // lower than this, keystore should not be activated.
    static final int MIN_PASSWORD_QUALITY = DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;

    private final KeyStore mKeyStore = KeyStore.getInstance();
    private Bundle mBundle;

    @Override protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        String action = intent.getAction();

        if (ACTION_RESET.equals(action)) {
            new ResetDialog();
        } else {
            if (!checkKeyguardQuality()) {
                new ConfigureLockScreenDialog();
                return;
            }
            if (ACTION_INSTALL.equals(action) &&
                    "com.android.certinstaller".equals(getCallingPackage())) {
                mBundle = intent.getExtras();
            }
            // ACTION_UNLOCK also handled here
            switch (mKeyStore.state()) {
                case UNINITIALIZED:
                    // if we had a keyguard set, we should be initialized
                    throw new AssertionError();
                case LOCKED:
                    // if we have a keyguard, why didn't we unlock?
                    // possibly old style password, display prompt
                    new UnlockDialog();
                    break;
                case UNLOCKED:
                    install();
                    finish();
                    break;
            }
        }
    }

    private boolean checkKeyguardQuality() {
        int quality = new LockPatternUtils(this).getActivePasswordQuality();
        return (quality >= MIN_PASSWORD_QUALITY);
    }

    private void install() {
        if (mBundle != null && !mBundle.isEmpty()) {
            for (String key : mBundle.keySet()) {
                byte[] value = mBundle.getByteArray(key);
                if (value != null && !mKeyStore.put(key, value)) {
                    Log.e(TAG, "Failed to install " + key);
                    return;
                }
            }
            setResult(RESULT_OK);
        }
    }

    private class ResetDialog
            implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener
    {
        private boolean mResetConfirmed;

        private ResetDialog() {
            AlertDialog dialog = new AlertDialog.Builder(CredentialStorage.this)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.credentials_reset_hint)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .create();
            dialog.setOnDismissListener(this);
            dialog.show();
        }

        @Override public void onClick(DialogInterface dialog, int button) {
            mResetConfirmed = (button == DialogInterface.BUTTON_POSITIVE);
        }

        @Override public void onDismiss(DialogInterface dialog) {
            if (mResetConfirmed) {
                mResetConfirmed = false;
                new ResetKeyStoreAndKeyChain().execute();
                return;
            }
            finish();
        }
    }

    private class ResetKeyStoreAndKeyChain extends AsyncTask<Void, Void, Boolean> {

        @Override protected Boolean doInBackground(Void... unused) {

            mKeyStore.reset();

            try {
                KeyChainConnection keyChainConnection = KeyChain.bind(CredentialStorage.this);
                try {
                    return keyChainConnection.getService().reset();
                } catch (RemoteException e) {
                    return false;
                } finally {
                    keyChainConnection.close();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        @Override protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(CredentialStorage.this,
                               R.string.credentials_erased, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(CredentialStorage.this,
                               R.string.credentials_not_erased, Toast.LENGTH_SHORT).show();
            }
            finish();
        }
    }

    private class ConfigureLockScreenDialog
            implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener
    {
        private boolean mConfigureConfirmed;

        private ConfigureLockScreenDialog() {
            AlertDialog dialog = new AlertDialog.Builder(CredentialStorage.this)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.credentials_configure_lock_screen_hint)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .create();
            dialog.setOnDismissListener(this);
            dialog.show();
        }

        @Override public void onClick(DialogInterface dialog, int button) {
            mConfigureConfirmed = (button == DialogInterface.BUTTON_POSITIVE);
        }

        @Override public void onDismiss(DialogInterface dialog) {
            if (mConfigureConfirmed) {
                mConfigureConfirmed = false;
                Intent intent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
                intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY,
                                MIN_PASSWORD_QUALITY);
                startActivity(intent);
                return;
            }
            finish();
        }
    }

    private class UnlockDialog implements TextWatcher,
            DialogInterface.OnClickListener, DialogInterface.OnDismissListener
    {
        private boolean mUnlockConfirmed;

        private final Button mButton;
        private final TextView mOldPassword;
        private final TextView mError;

        private UnlockDialog() {
            View view = View.inflate(CredentialStorage.this, R.layout.credentials_dialog, null);

            ((TextView) view.findViewById(R.id.hint)).setText(R.string.credentials_unlock_hint);
            mOldPassword = (TextView) view.findViewById(R.id.old_password);
            mOldPassword.setVisibility(View.VISIBLE);
            mOldPassword.addTextChangedListener(this);
            mError = (TextView) view.findViewById(R.id.error);

            AlertDialog dialog = new AlertDialog.Builder(CredentialStorage.this)
                    .setView(view)
                    .setTitle(R.string.credentials_unlock)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .create();
            dialog.setOnDismissListener(this);
            dialog.show();
            mButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            mButton.setEnabled(false);
        }

        @Override public void afterTextChanged(Editable editable) {
            mButton.setEnabled(mOldPassword == null || mOldPassword.getText().length() > 0);
        }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override public void onTextChanged(CharSequence s,int start, int before, int count) {
        }

        @Override public void onClick(DialogInterface dialog, int button) {
            mUnlockConfirmed = (button == DialogInterface.BUTTON_POSITIVE);
        }

        @Override public void onDismiss(DialogInterface dialog) {
            if (mUnlockConfirmed) {
                mUnlockConfirmed = false;
                mError.setVisibility(View.VISIBLE);
                mKeyStore.unlock(mOldPassword.getText().toString());
                int error = mKeyStore.getLastError();
                if (error == KeyStore.NO_ERROR) {
                    Toast.makeText(CredentialStorage.this,
                                   R.string.credentials_enabled,
                                   Toast.LENGTH_SHORT).show();
                    install();
                } else if (error == KeyStore.UNINITIALIZED) {
                    Toast.makeText(CredentialStorage.this,
                                   R.string.credentials_erased,
                                   Toast.LENGTH_SHORT).show();
                } else if (error >= KeyStore.WRONG_PASSWORD) {
                    int count = error - KeyStore.WRONG_PASSWORD + 1;
                    if (count > 3) {
                        mError.setText(R.string.credentials_wrong_password);
                    } else if (count == 1) {
                        mError.setText(R.string.credentials_reset_warning);
                    } else {
                        mError.setText(getString(R.string.credentials_reset_warning_plural, count));
                    }
                    ((AlertDialog) dialog).show();
                    return;
                }
            }
            finish();
        }
    }
}
