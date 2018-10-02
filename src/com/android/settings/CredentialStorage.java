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
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.internal.widget.LockPatternUtils;
import com.android.org.bouncycastle.asn1.ASN1InputStream;
import com.android.org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.security.ConfigureKeyGuardDialog;
import com.android.settings.vpn2.VpnUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import sun.security.util.ObjectIdentifier;
import sun.security.x509.AlgorithmId;

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
 * OR user had key guard and pre-ICS keystore password which was then reset
 *
 * KeyStore: LOCKED
 * KeyGuard: OFF/ON
 * Action:   confirm key guard
 * Notes:    request normal unlock to unlock the keystore.
 * if unlock, ensure key guard before install.
 * if reset, treat as UNINITALIZED/OFF
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
public final class CredentialStorage extends FragmentActivity {

    private static final String TAG = "CredentialStorage";

    public static final String ACTION_UNLOCK = "com.android.credentials.UNLOCK";
    public static final String ACTION_INSTALL = "com.android.credentials.INSTALL";
    public static final String ACTION_RESET = "com.android.credentials.RESET";

    // This is the minimum acceptable password quality.  If the current password quality is
    // lower than this, keystore should not be activated.
    public static final int MIN_PASSWORD_QUALITY = DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;

    private static final int CONFIRM_KEY_GUARD_REQUEST = 1;
    private static final int CONFIRM_CLEAR_SYSTEM_CREDENTIAL_REQUEST = 2;

    private final KeyStore mKeyStore = KeyStore.getInstance();
    private LockPatternUtils mUtils;

    /**
     * When non-null, the bundle containing credentials to install.
     */
    private Bundle mInstallBundle;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        mUtils = new LockPatternUtils(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final Intent intent = getIntent();
        final String action = intent.getAction();
        final UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
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
                // Force key guard confirmation
                confirmKeyGuard(CONFIRM_KEY_GUARD_REQUEST);
                return;
            }
            case UNLOCKED: {
                if (!mUtils.isSecure(UserHandle.myUserId())) {
                    final ConfigureKeyGuardDialog dialog = new ConfigureKeyGuardDialog();
                    dialog.show(getSupportFragmentManager(), ConfigureKeyGuardDialog.TAG);
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
        if (!mUtils.isSecure(UserHandle.myUserId())) {
            // key guard not setup, doing so will initialize keystore
            final ConfigureKeyGuardDialog dialog = new ConfigureKeyGuardDialog();
            dialog.show(getSupportFragmentManager(), ConfigureKeyGuardDialog.TAG);
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

    private boolean isHardwareBackedKey(byte[] keyData) {
        try {
            final ASN1InputStream bIn = new ASN1InputStream(new ByteArrayInputStream(keyData));
            final PrivateKeyInfo pki = PrivateKeyInfo.getInstance(bIn.readObject());
            final String algOid = pki.getPrivateKeyAlgorithm().getAlgorithm().getId();
            final String algName = new AlgorithmId(new ObjectIdentifier(algOid)).getName();

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

        final Bundle bundle = mInstallBundle;
        mInstallBundle = null;

        final int uid = bundle.getInt(Credentials.EXTRA_INSTALL_AS_UID, KeyStore.UID_SELF);

        if (uid != KeyStore.UID_SELF && !UserHandle.isSameUser(uid, Process.myUid())) {
            final int dstUserId = UserHandle.getUserId(uid);

            // Restrict install target to the wifi uid.
            if (uid != Process.WIFI_UID) {
                Log.e(TAG, "Failed to install credentials as uid " + uid + ": cross-user installs"
                        + " may only target wifi uids");
                return;
            }

            final Intent installIntent = new Intent(ACTION_INSTALL)
                    .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                    .putExtras(bundle);
            startActivityAsUser(installIntent, new UserHandle(dstUserId));
            return;
        }

        if (bundle.containsKey(Credentials.EXTRA_USER_PRIVATE_KEY_NAME)) {
            final String key = bundle.getString(Credentials.EXTRA_USER_PRIVATE_KEY_NAME);
            final byte[] value = bundle.getByteArray(Credentials.EXTRA_USER_PRIVATE_KEY_DATA);

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
            // The key was prepended USER_PRIVATE_KEY by the CredentialHelper. However,
            // KeyChain internally uses the raw alias name and only prepends USER_PRIVATE_KEY
            // to the key name when interfacing with KeyStore.
            // This is generally a symptom of CredentialStorage and CredentialHelper relying
            // on internal implementation details of KeyChain and imitating its functionality
            // rather than delegating to KeyChain for the certificate installation.
            if (uid == Process.SYSTEM_UID || uid == KeyStore.UID_SELF) {
                new MarkKeyAsUserSelectable(
                        key.replaceFirst("^" + Credentials.USER_PRIVATE_KEY, "")).execute();
            }
        }

        final int flags = KeyStore.FLAG_NONE;

        if (bundle.containsKey(Credentials.EXTRA_USER_CERTIFICATE_NAME)) {
            final String certName = bundle.getString(Credentials.EXTRA_USER_CERTIFICATE_NAME);
            final byte[] certData = bundle.getByteArray(Credentials.EXTRA_USER_CERTIFICATE_DATA);

            if (!mKeyStore.put(certName, certData, uid, flags)) {
                Log.e(TAG, "Failed to install " + certName + " as uid " + uid);
                return;
            }
        }

        if (bundle.containsKey(Credentials.EXTRA_CA_CERTIFICATES_NAME)) {
            final String caListName = bundle.getString(Credentials.EXTRA_CA_CERTIFICATES_NAME);
            final byte[] caListData = bundle.getByteArray(Credentials.EXTRA_CA_CERTIFICATES_DATA);

            if (!mKeyStore.put(caListName, caListData, uid, flags)) {
                Log.e(TAG, "Failed to install " + caListName + " as uid " + uid);
                return;
            }
        }

        // Send the broadcast.
        final Intent broadcast = new Intent(KeyChain.ACTION_KEYCHAIN_CHANGED);
        sendBroadcast(broadcast);

        setResult(RESULT_OK);
    }

    /**
     * Prompt for reset confirmation, resetting on confirmation, finishing otherwise.
     */
    private class ResetDialog
            implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
        private boolean mResetConfirmed;

        private ResetDialog() {
            final AlertDialog dialog = new AlertDialog.Builder(CredentialStorage.this)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage(R.string.credentials_reset_hint)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .create();
            dialog.setOnDismissListener(this);
            dialog.show();
        }

        @Override
        public void onClick(DialogInterface dialog, int button) {
            mResetConfirmed = (button == DialogInterface.BUTTON_POSITIVE);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
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

        @Override
        protected Boolean doInBackground(Void... unused) {

            // Clear all the users credentials could have been installed in for this user.
            mUtils.resetKeyStore(UserHandle.myUserId());

            try {
                final KeyChainConnection keyChainConnection = KeyChain.bind(CredentialStorage.this);
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

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(CredentialStorage.this,
                        R.string.credentials_erased, Toast.LENGTH_SHORT).show();
                clearLegacyVpnIfEstablished();
            } else {
                Toast.makeText(CredentialStorage.this,
                        R.string.credentials_not_erased, Toast.LENGTH_SHORT).show();
            }
            finish();
        }
    }

    private void clearLegacyVpnIfEstablished() {
        final boolean isDone = VpnUtils.disconnectLegacyVpn(getApplicationContext());
        if (isDone) {
            Toast.makeText(CredentialStorage.this, R.string.vpn_disconnected,
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Background task to mark a given key alias as user-selectable, so that
     * it can be selected by users from the Certificate Selection prompt.
     */
    private class MarkKeyAsUserSelectable extends AsyncTask<Void, Void, Boolean> {
        final String mAlias;

        MarkKeyAsUserSelectable(String alias) {
            mAlias = alias;
        }

        @Override
        protected Boolean doInBackground(Void... unused) {
            try (KeyChainConnection keyChainConnection = KeyChain.bind(CredentialStorage.this)) {
                keyChainConnection.getService().setUserSelectable(mAlias, true);
                return true;
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to mark key " + mAlias + " as user-selectable.");
                return false;
            } catch (InterruptedException e) {
                Log.w(TAG, "Failed to mark key " + mAlias + " as user-selectable.");
                Thread.currentThread().interrupt();
                return false;
            }
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
            final int launchedFromUid = android.app.ActivityManager.getService()
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

        final UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
        final UserInfo parentInfo = userManager.getProfileParent(launchedFromUserId);
        // Caller is running in a profile of this user
        return ((parentInfo != null) && (parentInfo.id == UserHandle.myUserId()));
    }

    /**
     * Confirm existing key guard, returning password via onActivityResult.
     */
    private boolean confirmKeyGuard(int requestCode) {
        final Resources res = getResources();
        return new ChooseLockSettingsHelper(this)
                .launchConfirmationActivity(requestCode,
                        res.getText(R.string.credentials_title), true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Receive key guard password initiated by confirmKeyGuard.
        if (requestCode == CONFIRM_KEY_GUARD_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                final String password = data.getStringExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
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
}
