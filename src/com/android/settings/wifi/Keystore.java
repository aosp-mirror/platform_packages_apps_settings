package com.android.settings.wifi;

import android.util.Log;

import java.io.File;

/**
 */
public abstract class Keystore {
    public static final String TAG = "Keystore";

    private static final String PACKAGE_PREFIX =
        Keystore.class.getPackage().getName() + ".";

    public static final String ACTION_KEYSTORE_CERTIFICATES =
        PACKAGE_PREFIX + "CERTIFICATES";
    public static final String ACTION_KEYSTORE_USERKEYS =
        PACKAGE_PREFIX + "USERKEYS";

    /**
     */
    public static Keystore getInstance() {
        return new FileKeystore();
    }

    /**
     */
    public abstract String getUserkey(String key);

    /**
     */
    public abstract String getCertificate(String key);

    /**
     */
    public abstract String[] getAllCertificateKeys();

    /**
     */
    public abstract String[] getAllUserkeyKeys();

    private static class FileKeystore extends Keystore {
        private static final String PATH = "/data/misc/keystore/";
        private static final String USERKEY_PATH = PATH + "userkeys/";
        private static final String CERT_PATH = PATH + "certs/";

        @Override
        public String getUserkey(String key) {
            String path = USERKEY_PATH + key;
            return (new File(path).exists() ? path : null);
        }

        @Override
        public String getCertificate(String key) {
            String path = CERT_PATH + key;
            return (new File(path).exists() ? path : null);
        }

        @Override
        public String[] getAllCertificateKeys() {
            File dir = new File(CERT_PATH);
            if (dir.exists()) {
                return dir.list();
            } else {
                Log.v(TAG, "-------- cert directory does not exist!");
                return null;
            }
        }

        @Override
        public String[] getAllUserkeyKeys() {
            File dir = new File(USERKEY_PATH);
            if (dir.exists()) {
                return dir.list();
            } else {
                Log.v(TAG, "-------- userkey directory does not exist!");
                return null;
            }
        }
    }
}
