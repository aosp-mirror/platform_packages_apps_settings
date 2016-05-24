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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.Credentials;
import android.security.KeyChain;
import android.security.KeyChain.KeyChainConnection;
import android.security.KeyStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.widget.LockPatternUtils;
import com.android.org.bouncycastle.asn1.ASN1InputStream;
import com.android.org.bouncycastle.asn1.pkcs.PrivateKeyInfo;

import sun.security.util.ObjectIdentifier;
import sun.security.x509.AlgorithmId;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * CredentialStorage handles KeyStore reset, unlock, and install.
 *
 * CredentialStorage has a pretty convoluted state machine to migrate
 * from the old style separate keystore password to a new key guard
 * based password, as well as to deal with setting up the key guard if
 * necessary.
 *
 * KeyStore: UNINITALIZED
 * KeyGuard: OFF
 * Action:   set up key guard
 * Notes:    factory state
 *
 * KeyStore: UNINITALIZED
 * KeyGuard: ON
 * Action:   confirm key guard
 * Notes:    user had key guard but no keystore and upgraded from pre-ICS
 *           OR user had key guard and pre-ICS keystore password which was then reset
 *
 * KeyStore: LOCKED
 * KeyGuard: OFF/ON
 * Action:   old unlock dialog
 * Notes:    assume old password, need to use it to unlock.
 *           if unlock, ensure key guard before install.
 *           if reset, treat as UNINITALIZED/OFF
 *
 * KeyStore: UNLOCKED
 * KeyGuard: OFF
 * Action:   set up key guard
 * Notes:    ensure key guard, then proceed
 *
 * KeyStore: UNLOCKED
 * keyguard: ON
 * Action:   normal unlock/install
 * Notes:    this is the common case
 */
public final class CredentialStorage extends Activity {

    private static final String TAG = "CredentialStorage";

    public static final String ACTION_UNLOCK = "com.android.credentials.UNLOCK";
    public static final String ACTION_INSTALL = "com.android.credentials.INSTALL";
    public static final String ACTION_RESET = "com.android.credentials.RESET";

    // This is the minimum acceptable password quality.  If the current password quality is
    // lower than this, keystore should not be activated.
    static final int MIN_PASSWORD_QUALITY = DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;

    private static final int CONFIRM_KEY_GUARD_REQUEST = 1;
    private static final int CONFIRM_CLEAR_SYSTEM_CREDENTIAL_REQUEST = 2;

    private final KeyStore mKeyStore = KeyStore.getInstance();

    /**
     * When non-null, the bundle containing credentials to install.
     */
    private Bundle mInstallBundle;

    /**
     * After unsuccessful KeyStore.unlock, the number of unlock
     * attempts remaining before the KeyStore will reset itself.
     *
     * Reset to -1 on successful unlock or reset.
     */
    private int mRetriesRemaining = -1;

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        String action = intent.getAction();
        UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
        if (!userManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_CREDENTIALS)) {
            if (ACTION_RESET.equals(action)) {
                new ResetDialog();
            } else {
                if (ACTION_INSTALL.equals(action) && checkCallerIsCertInstallerOrSelfInProfile()) {
                    mInstallBundle = intent.getExtras();
                }
                // ACTION_UNLOCK also handled here in addition to ACTION_INSTALL
                handleUnlockOrInstall();
            }
        } else {
            // Users can set a screen lock if there is none even if they can't modify the
            // credentials store.
            if (ACTION_UNLOCK.equals(action) && mKeyStore.state() == KeyStore.State.UNINITIALIZED) {
                ensureKeyGuard();
            } else {
                finish();
            }
        }
    }

    /**
     * Based on the current state of the KeyStore and key guard, try to
     * make progress on unlocking or installing to the keystore.
     */
    private void handleUnlockOrInstall() {
        // something already decided we are done, do not proceed
        if (isFinishing()) {
            return;
        }
        switch (mKeyStore.state()) {
            case UNINITIALIZED: {
                ensureKeyGuard();
                return;
            }
            case LOCKED: {
                new UnlockDialog();
                return;
            }
            case UNLOCKED: {
                if (!checkKeyGuardQuality()) {
                    new ConfigureKeyGuardDialog();
                    return;
                }
                installIfAvailable();
                finish();
                return;
            }
        }
    }

    /**
     * Make sure the user enters the key guard to set or change the
     * keystore password. This can be used in UNINITIALIZED to set the
     * keystore password or UNLOCKED to change the password (as is the
     * case after unlocking with an old-style password).
     */
    private void ensureKeyGuard() {
        if (!checkKeyGuardQuality()) {
            // key guard not setup, doing so will initialize keystore
            new ConfigureKeyGuardDialog();
            // will return to onResume after Activity
            return;
        }
        // force key guard confirmation
        if (confirmKeyGuard(CONFIRM_KEY_GUARD_REQUEST)) {
            // will return password value via onActivityResult
            return;
        }
        finish();
    }

    /**
     * Returns true if the currently set key guard matches our minimum quality requirements.
     */
    private boolean checkKeyGuardQuality() {
        int credentialOwner =
                UserManager.get(this).getCredentialOwnerProfile(UserHandle.myUserId());
        int quality = new LockPatternUtils(this).getActivePasswordQuality(credentialOwner);
        return (quality >= MIN_PASSWORD_QUALITY);
    }

    private boolean isHardwareBackedKey(byte[] keyData) {
        try {
            ASN1InputStream bIn = new ASN1InputStream(new ByteArrayInputStream(keyData));
            PrivateKeyInfo pki = PrivateKeyInfo.getInstance(bIn.readObject());
            String algOid = pki.getAlgorithmId().getAlgorithm().getId();
            String algName = new AlgorithmId(new ObjectIdentifier(algOid)).getName();

            return KeyChain.isBoundKeyAlgorithm(algName);
        } catch (IOException e) {
            Log.e(TAG, "Failed to parse key data");
            return false;
        }
    }

    /**
     * Install credentials if available, otherwise do nothing.
     */
    private void installIfAvailable() {
        if (mInstallBundle == null || mInstallBundle.isEmpty()) {
            return;
        }

        Bundle bundle = mInstallBundle;
        mInstallBundle = null;

        final int uid = bundle.getInt(Credentials.EXTRA_INSTALL_AS_UID, KeyStore.UID_SELF);

        if (uid != KeyStore.UID_SELF && !UserHandle.isSameUser(uid, Process.myUid())) {
            int dstUserId = UserHandle.getUserId(uid);
            int myUserId = UserHandle.myUserId();

            // Restrict install target to the wifi uid.
            if (uid != Process.WIFI_UID) {
                Log.e(TAG, "Failed to install credentials as uid " + uid + ": cross-user installs"
                        + " may only target wifi uids");
                return;
            }

            Intent installIntent = new Intent(ACTION_INSTALL)
                    .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                    .putExtras(bundle);
            startActivityAsUser(installIntent, new UserHandle(dstUserId));
            return;
        }

        if (bundle.containsKey(Credentials.EXTRA_USER_PRIVATE_KEY_NAME)) {
            String key = bundle.getString(Credentials.EXTRA_USER_PRIVATE_KEY_NAME);
            byte[] value = bundle.getByteArray(Credentials.EXTRA_USER_PRIVATE_KEY_DATA);

            int flags = KeyStore.FLAG_ENCRYPTED;
            if (uid == Process.WIFI_UID && isHardwareBackedKey(value)) {
                // Hardware backed keystore is secure enough to allow for WIFI stack
                // to enable access to secure networks without user intervention
                Log.d(TAG, "Saving private key with FLAG_NONE for WIFI_UID");
                flags = KeyStore.FLAG_NONE;
            }

            if (!mKeyStore.importKey(key, value, uid, flags)) {
                Log.e(TAG, "Failed to install " + key + " as uid " + uid);
                return;
            }
        }

        int flags = (uid == Process.WIFI_UID) ? KeyStore.FLAG_NONE : KeyStore.FLAG_ENCRYPTED;

        if (bundle.containsKey(Credentials.EXTRA_USER_CERTIFICATE_NAME)) {
            String certName = bundle.getString(Credentials.EXTRA_USER_CERTIFICATE_NAME);
            byte[] certData = bundle.getByteArray(Credentials.EXTRA_USER_CERTIFICATE_DATA);

            if (!mKeyStore.put(certName, certData, uid, flags)) {
                Log.e(TAG, "Failed to install " + certName + " as uid " + uid);
                return;
            }
        }

        if (bundle.containsKey(Credentials.EXTRA_CA_CERTIFICATES_NAME)) {
            String caListName = bundle.getString(Credentials.EXTRA_CA_CERTIFICATES_NAME);
            byte[] caListData = bundle.getByteArray(Credentials.EXTRA_CA_CERTIFICATES_DATA);

            if (!mKeyStore.put(caListName, caListData, uid, flags)) {
                Log.e(TAG, "Failed to install " + caListName + " as uid " + uid);
                return;
            }
        }

        setResult(RESULT_OK);
    }

    /**
     * Prompt for reset confirmation, resetting on confirmation, finishing otherwise.
     */
    private class ResetDialog
            implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener
    {
        private boolean mResetConfirmed;

        private ResetDialog() {
            AlertDialog dialog = new AlertDialog.Builder(CredentialStorage.this)
                    .setTitle(android.R.string.dialog_alert_title)
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
                if (confirmKeyGuard(CONFIRM_CLEAR_SYSTEM_CREDENTIAL_REQUEST)) {
                    // will return password value via onActivityResult
                    return;
                }
            }
            finish();
        }
    }

    /**
     * Background task to handle reset of both keystore and user installed CAs.
     */
    private class ResetKeyStoreAndKeyChain extends AsyncTask<Void, Void, Boolean> {

        @Override protected Boolean doInBackground(Void... unused) {

            // Clear all the users credentials could have been installed in for this user.
            new LockPatternUtils(CredentialStorage.this).resetKeyStore(UserHandle.myUserId());

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

    /**
     * Prompt for key guard configuration confirmation.
     */
    private class ConfigureKeyGuardDialog
            implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener
    {
        private boolean mConfigureConfirmed;

        private ConfigureKeyGuardDialog() {
            AlertDialog dialog = new AlertDialog.Builder(CredentialStorage.this)
                    .setTitle(android.R.string.dialog_alert_title)
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

    /**
     * Check that the caller is either certinstaller or Settings running in a profile of this user.
     */
    private boolean checkCallerIsCertInstallerOrSelfInProfile() {
        if (TextUtils.equals("com.android.certinstaller", getCallingPackage())) {
            // CertInstaller is allowed to install credentials if it has the same signature as
            // Settings package.
            return getPackageManager().checkSignatures(
                    getCallingPackage(), getPackageName()) == PackageManager.SIGNATURE_MATCH;
        }

        final int launchedFromUserId;
        try {
            int launchedFromUid = android.app.ActivityManagerNative.getDefault()
                    .getLaunchedFromUid(getActivityToken());
            if (launchedFromUid == -1) {
                Log.e(TAG, ACTION_INSTALL + " must be started with startActivityForResult");
                return false;
            }
            if (!UserHandle.isSameApp(launchedFromUid, Process.myUid())) {
                // Not the same app
                return false;
            }
            launchedFromUserId = UserHandle.getUserId(launchedFromUid);
        } catch (RemoteException re) {
            // Error talking to ActivityManager, just give up
            return false;
        }

        UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
        UserInfo parentInfo = userManager.getProfileParent(launchedFromUserId);
        if (parentInfo == null || parentInfo.id != UserHandle.myUserId()) {
            // Caller is not running in a profile of this user
            return false;
        }
        return true;
    }

    /**
     * Confirm existing key guard, returning password via onActivityResult.
     */
    private boolean confirmKeyGuard(int requestCode) {
        Resources res = getResources();
        boolean launched = new ChooseLockSettingsHelper(this)
                .launchConfirmationActivity(requestCode,
                        res.getText(R.string.credentials_title), true);
        return launched;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        /**
         * Receive key guard password initiated by confirmKeyGuard.
         */
        if (requestCode == CONFIRM_KEY_GUARD_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                String password = data.getStringExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
                if (!TextUtils.isEmpty(password)) {
                    // success
                    mKeyStore.unlock(password);
                    // return to onResume
                    return;
                }
            }
            // failed confirmation, bail
            finish();
        } else if (requestCode == CONFIRM_CLEAR_SYSTEM_CREDENTIAL_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                new ResetKeyStoreAndKeyChain().execute();
                return;
            }
            // failed confirmation, bail
            finish();
        }
    }

    /**
     * Prompt for unlock with old-style password.
     *
     * On successful unlock, ensure migration to key guard before continuing.
     * On unsuccessful unlock, retry by calling handleUnlockOrInstall.
     */
    private class UnlockDialog implements TextWatcher,
            DialogInterface.OnClickListener, DialogInterface.OnDismissListener
    {
        private boolean mUnlockConfirmed;

        private final Button mButton;
        private final TextView mOldPassword;
        private final TextView mError;

        private UnlockDialog() {
            View view = View.inflate(CredentialStorage.this, R.layout.credentials_dialog, null);

            CharSequence text;
            if (mRetriesRemaining == -1) {
                text = getResources().getText(R.string.credentials_unlock_hint);
            } else if (mRetriesRemaining > 3) {
                text = getResources().getText(R.string.credentials_wrong_password);
            } else if (mRetriesRemaining == 1) {
                text = getResources().getText(R.string.credentials_reset_warning);
            } else {
                text = getString(R.string.credentials_reset_warning_plural, mRetriesRemaining);
            }

            ((TextView) view.findViewById(R.id.hint)).setText(text);
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
                    mRetriesRemaining = -1;
                    Toast.makeText(CredentialStorage.this,
                                   R.string.credentials_enabled,
                                   Toast.LENGTH_SHORT).show();
                    // aha, now we are unlocked, switch to key guard.
                    // we'll end up back in onResume to install
                    ensureKeyGuard();
                } else if (error == KeyStore.UNINITIALIZED) {
                    mRetriesRemaining = -1;
                    Toast.makeText(CredentialStorage.this,
                                   R.string.credentials_erased,
                                   Toast.LENGTH_SHORT).show();
                    // we are reset, we can now set new password with key guard
                    handleUnlockOrInstall();
                } else if (error >= KeyStore.WRONG_PASSWORD) {
                    // we need to try again
                    mRetriesRemaining = error - KeyStore.WRONG_PASSWORD + 1;
                    handleUnlockOrInstall();
                }
                return;
            }
            finish();
        }
    }
}
