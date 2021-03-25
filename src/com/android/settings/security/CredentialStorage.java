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

package com.android.settings.security;

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
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.security.KeyChain.KeyChainConnection;
import android.security.KeyStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.core.HideNonSystemOverlayMixin;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.vpn2.VpnUtils;

/**
 * CredentialStorage handles resetting and installing keys into KeyStore.
 */
public final class CredentialStorage extends FragmentActivity {

    private static final String TAG = "CredentialStorage";

    public static final String ACTION_INSTALL = "com.android.credentials.INSTALL";
    public static final String ACTION_RESET = "com.android.credentials.RESET";

    // This is the minimum acceptable password quality.  If the current password quality is
    // lower than this, keystore should not be activated.
    public static final int MIN_PASSWORD_QUALITY = DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;

    private static final int CONFIRM_CLEAR_SYSTEM_CREDENTIAL_REQUEST = 1;

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
        getLifecycle().addObserver(new HideNonSystemOverlayMixin(this));
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
                handleInstall();
            }
        } else {
            finish();
        }
    }

    /**
     * Install credentials from mInstallBundle into Keystore.
     */
    private void handleInstall() {
        // something already decided we are done, do not proceed
        if (isFinishing()) {
            return;
        }
        if (installIfAvailable()) {
            finish();
        }
    }

    /**
     * Install credentials if available, otherwise do nothing.
     *
     * @return true if the installation is done and the activity should be finished, false if
     * an asynchronous task is pending and will finish the activity when it's done.
     */
    private boolean installIfAvailable() {
        if (mInstallBundle == null || mInstallBundle.isEmpty()) {
            return true;
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
                return true;
            }

            final Intent installIntent = new Intent(ACTION_INSTALL)
                    .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                    .putExtras(bundle);
            startActivityAsUser(installIntent, new UserHandle(dstUserId));
            return true;
        }

        String alias = bundle.getString(Credentials.EXTRA_USER_KEY_ALIAS, null);
        if (TextUtils.isEmpty(alias)) {
            Log.e(TAG, "Cannot install key without an alias");
            return true;
        }

        final byte[] privateKeyData = bundle.getByteArray(Credentials.EXTRA_USER_PRIVATE_KEY_DATA);
        final byte[] certData = bundle.getByteArray(Credentials.EXTRA_USER_CERTIFICATE_DATA);
        final byte[] caListData = bundle.getByteArray(Credentials.EXTRA_CA_CERTIFICATES_DATA);
        new InstallKeyInKeyChain(alias, privateKeyData, certData, caListData, uid).execute();

        return false;
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
            if (!mResetConfirmed) {
                finish();
                return;
            }

            mResetConfirmed = false;
            if (!mUtils.isSecure(UserHandle.myUserId())) {
                // This task will call finish() in the end.
                new ResetKeyStoreAndKeyChain().execute();
            } else if (!confirmKeyGuard(CONFIRM_CLEAR_SYSTEM_CREDENTIAL_REQUEST)) {
                Log.w(TAG, "Failed to launch credential confirmation for a secure user.");
                finish();
            }
            // Confirmation result will be handled in onActivityResult if needed.
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
     * Background task to install a certificate into KeyChain or the WiFi Keystore.
     */
    private class InstallKeyInKeyChain extends AsyncTask<Void, Void, Boolean> {
        final String mAlias;
        private final byte[] mKeyData;
        private final byte[] mCertData;
        private final byte[] mCaListData;
        private final int mUid;

        InstallKeyInKeyChain(String alias, byte[] keyData, byte[] certData, byte[] caListData,
                int uid) {
            mAlias = alias;
            mKeyData = keyData;
            mCertData = certData;
            mCaListData = caListData;
            mUid = uid;
        }

        @Override
        protected Boolean doInBackground(Void... unused) {
            try (KeyChainConnection keyChainConnection = KeyChain.bind(CredentialStorage.this)) {
                IKeyChainService service = keyChainConnection.getService();
                if (!service.installKeyPair(mKeyData, mCertData, mCaListData, mAlias, mUid)) {
                    Log.w(TAG, String.format("Failed installing key %s", mAlias));
                    return false;
                }

                // If this is not a WiFi key, mark  it as user-selectable, so that it can be
                // selected by users from the Certificate Selection prompt.
                if (mUid == Process.SYSTEM_UID || mUid == KeyStore.UID_SELF) {
                    service.setUserSelectable(mAlias, true);
                }

                return true;
            } catch (RemoteException e) {
                Log.w(TAG, String.format("Failed to install key %s to uid %d", mAlias, mUid), e);
                return false;
            } catch (InterruptedException e) {
                Log.w(TAG, String.format("Interrupted while installing key %s", mAlias), e);
                Thread.currentThread().interrupt();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            CredentialStorage.this.onKeyInstalled(mAlias, mUid, result);
        }
    }

    private void onKeyInstalled(String alias, int uid, boolean result) {
        if (!result) {
            Log.w(TAG, String.format("Error installing alias %s for uid %d", alias, uid));
            finish();
            return;
        }

        Log.i(TAG, String.format("Successfully installed alias %s to uid %d.",
                alias, uid));

        // Send the broadcast.
        final Intent broadcast = new Intent(KeyChain.ACTION_KEYCHAIN_CHANGED);
        sendBroadcast(broadcast);
        setResult(RESULT_OK);

        finish();
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
        if (requestCode == CONFIRM_CLEAR_SYSTEM_CREDENTIAL_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                new ResetKeyStoreAndKeyChain().execute();
                return;
            }
            // failed confirmation, bail
            finish();
        }
    }
}
