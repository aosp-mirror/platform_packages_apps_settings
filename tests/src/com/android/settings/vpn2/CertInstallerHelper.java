/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.vpn2;

import android.os.Environment;
import android.security.Credentials;
import android.security.KeyStore;
import android.util.Log;

import com.android.internal.net.VpnProfile;
import com.android.org.bouncycastle.asn1.ASN1InputStream;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.DEROctetString;
import com.android.org.bouncycastle.asn1.x509.BasicConstraints;

import junit.framework.Assert;

import libcore.io.Streams;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Certificate installer helper to extract information from a provided file
 * and install certificates to keystore.
 */
public class CertInstallerHelper {
    private static final String TAG = "CertInstallerHelper";
    /* Define a password to unlock keystore after it is reset */
    private static final String CERT_STORE_PASSWORD = "password";
    private final int mUid = KeyStore.UID_SELF;
    private PrivateKey mUserKey;  // private key
    private X509Certificate mUserCert;  // user certificate
    private List<X509Certificate> mCaCerts = new ArrayList<X509Certificate>();
    private KeyStore mKeyStore = KeyStore.getInstance();

    /**
     * Unlock keystore and set password
     */
    public CertInstallerHelper() {
        mKeyStore.reset();
        mKeyStore.password(CERT_STORE_PASSWORD);
    }

    private void extractCertificate(String certFile, String password) {
        InputStream in = null;
        final byte[] raw;
        java.security.KeyStore keystore = null;
        try {
            // Read .p12 file from SDCARD and extract with password
            in = new FileInputStream(new File(
                    Environment.getExternalStorageDirectory(), certFile));
            raw = Streams.readFully(in);

            keystore = java.security.KeyStore.getInstance("PKCS12");
            PasswordProtection passwordProtection = new PasswordProtection(password.toCharArray());
            keystore.load(new ByteArrayInputStream(raw), passwordProtection.getPassword());

            // Install certificates and private keys
            Enumeration<String> aliases = keystore.aliases();
            if (!aliases.hasMoreElements()) {
                Assert.fail("key store failed to put in keychain");
            }
            ArrayList<String> aliasesList = Collections.list(aliases);
            // The keystore is initialized for each test case, there will
            // be only one alias in the keystore
            Assert.assertEquals(1, aliasesList.size());
            String alias = aliasesList.get(0);
            java.security.KeyStore.Entry entry = keystore.getEntry(alias, passwordProtection);
            Log.d(TAG, "extracted alias = " + alias + ", entry=" + entry.getClass());

            if (entry instanceof PrivateKeyEntry) {
                Assert.assertTrue(installFrom((PrivateKeyEntry) entry));
            }
        } catch (IOException e) {
            Assert.fail("Failed to read certficate: " + e);
        } catch (KeyStoreException e) {
            Log.e(TAG, "failed to extract certificate" + e);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "failed to extract certificate" + e);
        } catch (CertificateException e) {
            Log.e(TAG, "failed to extract certificate" + e);
        } catch (UnrecoverableEntryException e) {
            Log.e(TAG, "failed to extract certificate" + e);
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e(TAG, "close FileInputStream error: " + e);
                }
            }
        }
    }

    /**
     * Extract private keys, user certificates and ca certificates
     */
    private synchronized boolean installFrom(PrivateKeyEntry entry) {
        mUserKey = entry.getPrivateKey();
        mUserCert = (X509Certificate) entry.getCertificate();

        Certificate[] certs = entry.getCertificateChain();
        Log.d(TAG, "# certs extracted = " + certs.length);
        mCaCerts = new ArrayList<X509Certificate>(certs.length);
        for (Certificate c : certs) {
            X509Certificate cert = (X509Certificate) c;
            if (isCa(cert)) {
                mCaCerts.add(cert);
            }
        }
        Log.d(TAG, "# ca certs extracted = " + mCaCerts.size());
        return true;
    }

    private boolean isCa(X509Certificate cert) {
        try {
            byte[] asn1EncodedBytes = cert.getExtensionValue("2.5.29.19");
            if (asn1EncodedBytes == null) {
                return false;
            }
            DEROctetString derOctetString = (DEROctetString)
                    new ASN1InputStream(asn1EncodedBytes).readObject();
            byte[] octets = derOctetString.getOctets();
            ASN1Sequence sequence = (ASN1Sequence)
                    new ASN1InputStream(octets).readObject();
            return BasicConstraints.getInstance(sequence).isCA();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Extract certificate from the given file, and install it to keystore
     * @param name certificate name
     * @param certFile .p12 file which includes certificates
     * @param password password to extract the .p12 file
     */
    public void installCertificate(VpnProfile profile, String certFile, String password) {
        // extract private keys, certificates from the provided file
        extractCertificate(certFile, password);
        // install certificate to the keystore
        int flags = KeyStore.FLAG_ENCRYPTED;
        try {
            if (mUserKey != null) {
                Log.v(TAG, "has private key");
                String key = Credentials.USER_PRIVATE_KEY + profile.ipsecUserCert;
                byte[] value = mUserKey.getEncoded();

                if (!mKeyStore.importKey(key, value, mUid, flags)) {
                    Log.e(TAG, "Failed to install " + key + " as user " + mUid);
                    return;
                }
                Log.v(TAG, "install " + key + " as user " + mUid + " is successful");
            }

            if (mUserCert != null) {
                String certName = Credentials.USER_CERTIFICATE + profile.ipsecUserCert;
                byte[] certData = Credentials.convertToPem(mUserCert);

                if (!mKeyStore.put(certName, certData, mUid, flags)) {
                    Log.e(TAG, "Failed to install " + certName + " as user " + mUid);
                    return;
                }
                Log.v(TAG, "install " + certName + " as user" + mUid + " is successful.");
            }

            if (!mCaCerts.isEmpty()) {
                String caListName = Credentials.CA_CERTIFICATE + profile.ipsecCaCert;
                X509Certificate[] caCerts = mCaCerts.toArray(new X509Certificate[mCaCerts.size()]);
                byte[] caListData = Credentials.convertToPem(caCerts);

                if (!mKeyStore.put(caListName, caListData, mUid, flags)) {
                    Log.e(TAG, "Failed to install " + caListName + " as user " + mUid);
                    return;
                }
                Log.v(TAG, " install " + caListName + " as user " + mUid + " is successful");
            }
        } catch (CertificateEncodingException e) {
            Log.e(TAG, "Exception while convert certificates to pem " + e);
            throw new AssertionError(e);
        } catch (IOException e) {
            Log.e(TAG, "IOException while convert to pem: " + e);
        }
    }

    public int getUid() {
        return mUid;
    }
}
