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
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.security.IKeyChainService;
import android.security.KeyStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.UnsupportedEncodingException;

public class CredentialStorage extends Activity implements TextWatcher,
        DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    public static final String ACTION_UNLOCK = "com.android.credentials.UNLOCK";
    public static final String ACTION_SET_PASSWORD = "com.android.credentials.SET_PASSWORD";
    public static final String ACTION_INSTALL = "com.android.credentials.INSTALL";
    public static final String ACTION_RESET = "com.android.credentials.RESET";

    private static final String TAG = "CredentialStorage";

    private KeyStore mKeyStore = KeyStore.getInstance();
    private boolean mPositive = false;
    private boolean mNeutral = false;
    private Bundle mBundle;

    private TextView mOldPassword;
    private TextView mNewPassword;
    private TextView mConfirmPassword;
    private TextView mError;
    private Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String action = intent.getAction();
        int state = mKeyStore.test();

        if (ACTION_RESET.equals(action)) {
            showResetDialog();
        } else if (ACTION_SET_PASSWORD.equals(action)) {
            showPasswordDialog(state == KeyStore.UNINITIALIZED);
        } else {
            if (ACTION_INSTALL.equals(action) &&
                    "com.android.certinstaller".equals(getCallingPackage())) {
                mBundle = intent.getExtras();
            }
            if (state == KeyStore.UNINITIALIZED) {
                showPasswordDialog(true);
            } else if (state == KeyStore.LOCKED) {
                showUnlockDialog();
            } else {
                install();
                finish();
            }
        }
    }

    private void install() {
        if (mBundle != null && !mBundle.isEmpty()) {
            try {
                for (String key : mBundle.keySet()) {
                    byte[] value = mBundle.getByteArray(key);
                    if (value != null && !mKeyStore.put(key.getBytes("UTF-8"), value)) {
                        Log.e(TAG, "Failed to install " + key);
                        return;
                    }
                }
                setResult(RESULT_OK);
            } catch (UnsupportedEncodingException e) {
                // Should never happen.
                throw new RuntimeException(e);
            }
        }
    }

    private void showResetDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(android.R.string.dialog_alert_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.credentials_reset_hint)
                .setNeutralButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .create();
        dialog.setOnDismissListener(this);
        dialog.show();
    }

    private void showPasswordDialog(boolean firstTime) {
        View view = View.inflate(this, R.layout.credentials_dialog, null);

        ((TextView) view.findViewById(R.id.hint)).setText(R.string.credentials_password_hint);
        if (!firstTime) {
            view.findViewById(R.id.old_password_prompt).setVisibility(View.VISIBLE);
            mOldPassword = (TextView) view.findViewById(R.id.old_password);
            mOldPassword.setVisibility(View.VISIBLE);
            mOldPassword.addTextChangedListener(this);
        }
        view.findViewById(R.id.new_passwords).setVisibility(View.VISIBLE);
        mNewPassword = (TextView) view.findViewById(R.id.new_password);
        mNewPassword.addTextChangedListener(this);
        mConfirmPassword = (TextView) view.findViewById(R.id.confirm_password);
        mConfirmPassword.addTextChangedListener(this);
        mError = (TextView) view.findViewById(R.id.error);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setTitle(R.string.credentials_set_password)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .create();
        dialog.setOnDismissListener(this);
        dialog.show();
        mButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        mButton.setEnabled(false);
    }

    private void showUnlockDialog() {
        View view = View.inflate(this, R.layout.credentials_dialog, null);

        ((TextView) view.findViewById(R.id.hint)).setText(R.string.credentials_unlock_hint);
        mOldPassword = (TextView) view.findViewById(R.id.old_password);
        mOldPassword.setVisibility(View.VISIBLE);
        mOldPassword.addTextChangedListener(this);
        mError = (TextView) view.findViewById(R.id.error);

        AlertDialog dialog = new AlertDialog.Builder(this)
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

    public void afterTextChanged(Editable editable) {
        if ((mOldPassword == null || mOldPassword.getText().length() > 0) &&
            (mNewPassword == null || mNewPassword.getText().length() >= 8) &&
            (mConfirmPassword == null || mConfirmPassword.getText().length() >= 8)) {
            mButton.setEnabled(true);
        } else {
            mButton.setEnabled(false);
        }
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s,int start, int before, int count) {
    }

    public void onClick(DialogInterface dialog, int button) {
        mPositive = (button == DialogInterface.BUTTON_POSITIVE);
        mNeutral = (button == DialogInterface.BUTTON_NEUTRAL);
    }

    public void onDismiss(DialogInterface dialog) {
        if (mPositive) {
            mPositive = false;
            mError.setVisibility(View.VISIBLE);

            if (mNewPassword == null) {
                mKeyStore.unlock(mOldPassword.getText().toString());
            } else {
                String newPassword = mNewPassword.getText().toString();
                String confirmPassword = mConfirmPassword.getText().toString();

                if (!newPassword.equals(confirmPassword)) {
                    mError.setText(R.string.credentials_passwords_mismatch);
                    ((AlertDialog) dialog).show();
                    return;
                } else if (mOldPassword == null) {
                    mKeyStore.password(newPassword);
                } else {
                    mKeyStore.password(mOldPassword.getText().toString(), newPassword);
                }
            }

            int error = mKeyStore.getLastError();
            if (error == KeyStore.NO_ERROR) {
                Toast.makeText(this, R.string.credentials_enabled, Toast.LENGTH_SHORT).show();
                install();
            } else if (error == KeyStore.UNINITIALIZED) {
                Toast.makeText(this, R.string.credentials_erased,  Toast.LENGTH_SHORT).show();
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
        if (mNeutral) {
            mNeutral = false;
            new ResetKeyStoreAndKeyChain().execute();
            return;
        }
        finish();
    }

    private class ResetKeyStoreAndKeyChain extends AsyncTask<Void, Void, Boolean> {

        @Override protected Boolean doInBackground(Void... unused) {

            mKeyStore.reset();

            final BlockingQueue<IKeyChainService> q = new LinkedBlockingQueue<IKeyChainService>(1);
            ServiceConnection keyChainServiceConnection = new ServiceConnection() {
                @Override public void onServiceConnected(ComponentName name, IBinder service) {
                    try {
                        q.put(IKeyChainService.Stub.asInterface(service));
                    } catch (InterruptedException e) {
                        throw new AssertionError(e);
                    }
                }
                @Override public void onServiceDisconnected(ComponentName name) {}
            };
            boolean isBound = bindService(new Intent(IKeyChainService.class.getName()),
                                          keyChainServiceConnection,
                                          Context.BIND_AUTO_CREATE);
            if (!isBound) {
                Log.w(TAG, "could not bind to KeyChainService");
                return false;
            }
            IKeyChainService keyChainService;
            try {
                keyChainService = q.take();
                return keyChainService.reset();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (RemoteException e) {
                return false;
            } finally {
                unbindService(keyChainServiceConnection);
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
}
