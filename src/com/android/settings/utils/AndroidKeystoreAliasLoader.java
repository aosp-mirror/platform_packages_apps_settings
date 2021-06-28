/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.utils;

import android.security.keystore.KeyProperties;
import android.security.keystore2.AndroidKeyStoreLoadStoreParameter;
import android.util.Log;

import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

/**
 * This class provides a portable and unified way to load the content of AndroidKeyStore through
 * public API.
 * @hide
 */
public class AndroidKeystoreAliasLoader {
    private static final String TAG = "SettingsKeystoreUtils";

    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";

    private final Collection<String> mKeyCertAliases;
    private final Collection<String> mCaCertAliases;
    /**
     * This Constructor loads all aliases of asymmetric keys pairs and certificates in the
     * AndroidKeyStore within the given namespace.
     * Viable namespaces are {@link KeyProperties#NAMESPACE_WIFI},
     * {@link KeyProperties#NAMESPACE_APPLICATION}, or null. The latter two are equivalent in
     * that they will load the keystore content of the app's own namespace. In case of settings,
     * this is the namespace of the AID_SYSTEM.
     *
     * @param namespace {@link KeyProperties#NAMESPACE_WIFI},
     *                  {@link KeyProperties#NAMESPACE_APPLICATION}, or null
     * @hide
     */
    public AndroidKeystoreAliasLoader(Integer namespace) {
        mKeyCertAliases = new ArrayList<>();
        mCaCertAliases = new ArrayList<>();
        final KeyStore keyStore;
        final Enumeration<String> aliases;
        try {
            keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            if (namespace != null && namespace != KeyProperties.NAMESPACE_APPLICATION) {
                keyStore.load(new AndroidKeyStoreLoadStoreParameter(namespace));
            } else {
                keyStore.load(null);
            }
            aliases = keyStore.aliases();
        } catch (Exception e) {
            Log.e(TAG, "Failed to open Android Keystore.", e);
            // Will return empty lists.
            return;
        }

        while (aliases.hasMoreElements()) {
            final String alias = aliases.nextElement();
            try {
                final Key key = keyStore.getKey(alias, null);
                if (key != null) {
                    if (key instanceof PrivateKey) {
                        mKeyCertAliases.add(alias);
                        final Certificate[] cert = keyStore.getCertificateChain(alias);
                        if (cert != null && cert.length >= 2) {
                            mCaCertAliases.add(alias);
                        }
                    }
                } else {
                    if (keyStore.getCertificate(alias) != null) {
                        mCaCertAliases.add(alias);
                    }
                }
            } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
                Log.e(TAG, "Failed to load alias: "
                        + alias + " from Android Keystore. Ignoring.", e);
            }
        }
    }

    /**
     * Returns the aliases of the key pairs and certificates stored in the Android KeyStore at the
     * time the constructor was called.
     * @return Collection of keystore aliases.
     * @hide
     */
    public Collection<String> getKeyCertAliases() {
        return mKeyCertAliases;
    }

    /**
     * Returns the aliases of the trusted certificates stored in the Android KeyStore at the
     * time the constructor was called.
     * @return Collection of keystore aliases.
     * @hide
     */
    public Collection<String> getCaCertAliases() {
        return mCaCertAliases;
    }
}
