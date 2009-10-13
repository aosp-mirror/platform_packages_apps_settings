/*
 * Copyright (C) 2009 The Android Open Source Project
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
import android.content.Intent;
import android.os.Bundle;
import android.security.Credentials;
import android.security.KeyStore;
import android.util.Log;

/**
 * Installs credentials to the system keystore. It reacts to the
 * {@link Credentials#SYSTEM_INSTALL_ACTION} intent. All the key-value pairs in
 * the intent are installed to the system keystore. For security reason, the
 * current implementation limits that only com.android.certinstaller can use
 * this service.
 */
public class CredentialInstaller extends Activity {
    private static final String TAG = "CredentialInstaller";
    private static final String UNLOCKING = "ulck";

    private KeyStore mKeyStore = KeyStore.getInstance();
    private boolean mUnlocking = false;

    @Override
    protected void onResume() {
        super.onResume();

        if (!"com.android.certinstaller".equals(getCallingPackage())) finish();

        if (isKeyStoreUnlocked()) {
            install();
        } else if (!mUnlocking) {
            mUnlocking = true;
            Credentials.getInstance().unlock(this);
            return;
        }
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outStates) {
        super.onSaveInstanceState(outStates);
        outStates.putBoolean(UNLOCKING, mUnlocking);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedStates) {
        super.onRestoreInstanceState(savedStates);
        mUnlocking = savedStates.getBoolean(UNLOCKING);
    }

    private void install() {
        Intent intent = getIntent();
        Bundle bundle = (intent == null) ? null : intent.getExtras();
        if (bundle == null) return;
        for (String key : bundle.keySet()) {
            byte[] data = bundle.getByteArray(key);
            if (data == null) continue;
            boolean success = mKeyStore.put(key.getBytes(), data);
            Log.d(TAG, "install " + key + ": " + data.length + "  success? " + success);
            if (!success) return;
        }
        setResult(RESULT_OK);
    }

    private boolean isKeyStoreUnlocked() {
        return (mKeyStore.test() == KeyStore.NO_ERROR);
    }
}
