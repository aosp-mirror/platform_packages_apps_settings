/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.wifi.dpp;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.biometrics.BiometricPrompt;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiManager;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.wifi.AccessPoint;
import com.android.wifitrackerlib.WifiEntry;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * Here are the items shared by both WifiDppConfiguratorActivity & WifiDppEnrolleeActivity
 *
 * @see WifiQrCode
 */
public class WifiDppUtils {
    /**
     * The fragment tag specified to FragmentManager for container activities to manage fragments.
     */
    static final String TAG_FRAGMENT_QR_CODE_SCANNER = "qr_code_scanner_fragment";

    /**
     * @see #TAG_FRAGMENT_QR_CODE_SCANNER
     */
    static final String TAG_FRAGMENT_QR_CODE_GENERATOR = "qr_code_generator_fragment";

    /**
     * @see #TAG_FRAGMENT_QR_CODE_SCANNER
     */
    static final String TAG_FRAGMENT_CHOOSE_SAVED_WIFI_NETWORK =
            "choose_saved_wifi_network_fragment";

    /**
     * @see #TAG_FRAGMENT_QR_CODE_SCANNER
     */
    static final String TAG_FRAGMENT_ADD_DEVICE = "add_device_fragment";

    /** The data is one of the static String SECURITY_* in {@link WifiQrCode} */
    static final String EXTRA_WIFI_SECURITY = "security";

    /** The data corresponding to {@code WifiConfiguration} SSID */
    static final String EXTRA_WIFI_SSID = "ssid";

    /** The data corresponding to {@code WifiConfiguration} preSharedKey */
    static final String EXTRA_WIFI_PRE_SHARED_KEY = "preSharedKey";

    /** The data corresponding to {@code WifiConfiguration} hiddenSSID */
    static final String EXTRA_WIFI_HIDDEN_SSID = "hiddenSsid";

    /** The data corresponding to {@code WifiConfiguration} networkId */
    static final String EXTRA_WIFI_NETWORK_ID = "networkId";

    /** The data to recognize if it's a Wi-Fi hotspot for configuration */
    static final String EXTRA_IS_HOTSPOT = "isHotspot";

    /**
     * Default status code for Easy Connect
     */
    static final int EASY_CONNECT_EVENT_FAILURE_NONE = 0;

    /**
     * Success status code for Easy Connect.
     */
    static final int EASY_CONNECT_EVENT_SUCCESS = 1;

    private static final Duration VIBRATE_DURATION_QR_CODE_RECOGNITION = Duration.ofMillis(3);

    private static final String AES_CBC_PKCS7_PADDING = "AES/CBC/PKCS7Padding";

    /**
     * Returns whether the device support WiFi DPP.
     */
    static boolean isWifiDppEnabled(Context context) {
        final WifiManager manager = context.getSystemService(WifiManager.class);
        return manager.isEasyConnectSupported();
    }

    /**
     * Returns an intent to launch QR code scanner for Wi-Fi DPP enrollee.
     *
     * After enrollee success, the callee activity will return connecting WifiConfiguration by
     * putExtra {@code WifiDialogActivity.KEY_WIFI_CONFIGURATION} for
     * {@code Activity#setResult(int resultCode, Intent data)}. The calling object should check
     * if it's available before using it.
     *
     * @param ssid The data corresponding to {@code WifiConfiguration} SSID
     * @return Intent for launching QR code scanner
     */
    public static Intent getEnrolleeQrCodeScannerIntent(Context context, String ssid) {
        final Intent intent = new Intent(context, WifiDppEnrolleeActivity.class);
        intent.setAction(WifiDppEnrolleeActivity.ACTION_ENROLLEE_QR_CODE_SCANNER);
        if (!TextUtils.isEmpty(ssid)) {
            intent.putExtra(EXTRA_WIFI_SSID, ssid);
        }
        return intent;
    }

    private static String getPresharedKey(WifiManager wifiManager,
            WifiConfiguration wifiConfiguration) {
        final List<WifiConfiguration> privilegedWifiConfigurations =
                wifiManager.getPrivilegedConfiguredNetworks();

        for (WifiConfiguration privilegedWifiConfiguration : privilegedWifiConfigurations) {
            if (privilegedWifiConfiguration.networkId == wifiConfiguration.networkId) {
                // WEP uses a shared key hence the AuthAlgorithm.SHARED is used
                // to identify it.
                if (wifiConfiguration.allowedKeyManagement.get(KeyMgmt.NONE)
                        && wifiConfiguration.allowedAuthAlgorithms.get(
                        WifiConfiguration.AuthAlgorithm.SHARED)) {
                    return privilegedWifiConfiguration
                            .wepKeys[privilegedWifiConfiguration.wepTxKeyIndex];
                } else {
                    return privilegedWifiConfiguration.preSharedKey;
                }
            }
        }
        return wifiConfiguration.preSharedKey;
    }

    static String removeFirstAndLastDoubleQuotes(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }

        int begin = 0;
        int end = str.length() - 1;
        if (str.charAt(begin) == '\"') {
            begin++;
        }
        if (str.charAt(end) == '\"') {
            end--;
        }
        return str.substring(begin, end+1);
    }

    static String getSecurityString(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(KeyMgmt.SAE)) {
            return WifiQrCode.SECURITY_SAE;
        }
        if (config.allowedKeyManagement.get(KeyMgmt.OWE)) {
            return WifiQrCode.SECURITY_NO_PASSWORD;
        }
        if (config.allowedKeyManagement.get(KeyMgmt.WPA_PSK) ||
                config.allowedKeyManagement.get(KeyMgmt.WPA2_PSK)) {
            return WifiQrCode.SECURITY_WPA_PSK;
        }
        return (config.wepKeys[0] == null) ?
                WifiQrCode.SECURITY_NO_PASSWORD : WifiQrCode.SECURITY_WEP;
    }

    static String getSecurityString(WifiEntry wifiEntry) {
        final int security = wifiEntry.getSecurity();
        switch (security) {
            case WifiEntry.SECURITY_SAE:
                return WifiQrCode.SECURITY_SAE;
            case WifiEntry.SECURITY_PSK:
                return WifiQrCode.SECURITY_WPA_PSK;
            case WifiEntry.SECURITY_WEP:
                return WifiQrCode.SECURITY_WEP;
            case WifiEntry.SECURITY_OWE:
            case WifiEntry.SECURITY_NONE:
            default:
                return WifiQrCode.SECURITY_NO_PASSWORD;
        }
    }

    /**
     * Returns an intent to launch QR code generator. It may return null if the security is not
     * supported by QR code generator.
     *
     * Do not use this method for Wi-Fi hotspot network, use
     * {@code getHotspotConfiguratorIntentOrNull} instead.
     *
     * @param context     The context to use for the content resolver
     * @param wifiManager An instance of {@link WifiManager}
     * @param accessPoint An instance of {@link AccessPoint}
     * @return Intent for launching QR code generator
     */
    public static Intent getConfiguratorQrCodeGeneratorIntentOrNull(Context context,
            WifiManager wifiManager, AccessPoint accessPoint) {
        final Intent intent = new Intent(context, WifiDppConfiguratorActivity.class);
        if (isSupportConfiguratorQrCodeGenerator(context, accessPoint)) {
            intent.setAction(WifiDppConfiguratorActivity.ACTION_CONFIGURATOR_QR_CODE_GENERATOR);
        } else {
            return null;
        }

        final WifiConfiguration wifiConfiguration = accessPoint.getConfig();
        setConfiguratorIntentExtra(intent, wifiManager, wifiConfiguration);

        // For a transition mode Wi-Fi AP, creates a QR code that's compatible with more devices
        if (accessPoint.isPskSaeTransitionMode()) {
            intent.putExtra(EXTRA_WIFI_SECURITY, WifiQrCode.SECURITY_WPA_PSK);
        }

        return intent;
    }

    /**
     * Returns an intent to launch QR code generator. It may return null if the security is not
     * supported by QR code generator.
     *
     * Do not use this method for Wi-Fi hotspot network, use
     * {@code getHotspotConfiguratorIntentOrNull} instead.
     *
     * @param context     The context to use for the content resolver
     * @param wifiManager An instance of {@link WifiManager}
     * @param wifiEntry An instance of {@link WifiEntry}
     * @return Intent for launching QR code generator
     */
    public static Intent getConfiguratorQrCodeGeneratorIntentOrNull(Context context,
            WifiManager wifiManager, WifiEntry wifiEntry) {
        final Intent intent = new Intent(context, WifiDppConfiguratorActivity.class);
        if (wifiEntry.canShare()) {
            intent.setAction(WifiDppConfiguratorActivity.ACTION_CONFIGURATOR_QR_CODE_GENERATOR);
        } else {
            return null;
        }

        final WifiConfiguration wifiConfiguration = wifiEntry.getWifiConfiguration();
        setConfiguratorIntentExtra(intent, wifiManager, wifiConfiguration);

        return intent;
    }

    /**
     * Returns an intent to launch QR code scanner. It may return null if the security is not
     * supported by QR code scanner.
     *
     * @param context     The context to use for the content resolver
     * @param wifiManager An instance of {@link WifiManager}
     * @param wifiEntry An instance of {@link WifiEntry}
     * @return Intent for launching QR code scanner
     */
    public static Intent getConfiguratorQrCodeScannerIntentOrNull(Context context,
            WifiManager wifiManager, WifiEntry wifiEntry) {
        final Intent intent = new Intent(context, WifiDppConfiguratorActivity.class);
        if (wifiEntry.canEasyConnect()) {
            intent.setAction(WifiDppConfiguratorActivity.ACTION_CONFIGURATOR_QR_CODE_SCANNER);
        } else {
            return null;
        }

        final WifiConfiguration wifiConfiguration = wifiEntry.getWifiConfiguration();
        setConfiguratorIntentExtra(intent, wifiManager, wifiConfiguration);

        if (wifiConfiguration.networkId == WifiConfiguration.INVALID_NETWORK_ID) {
            throw new IllegalArgumentException("Invalid network ID");
        } else {
            intent.putExtra(EXTRA_WIFI_NETWORK_ID, wifiConfiguration.networkId);
        }

        return intent;
    }

    /**
     * Returns an intent to launch QR code generator for the Wi-Fi hotspot. It may return null if
     * the security is not supported by QR code generator.
     *
     * @param context The context to use for the content resolver
     * @param wifiManager An instance of {@link WifiManager}
     * @param softApConfiguration {@link SoftApConfiguration} of the Wi-Fi hotspot
     * @return Intent for launching QR code generator
     */
    public static Intent getHotspotConfiguratorIntentOrNull(Context context,
            WifiManager wifiManager, SoftApConfiguration softApConfiguration) {
        final Intent intent = new Intent(context, WifiDppConfiguratorActivity.class);
        if (isSupportHotspotConfiguratorQrCodeGenerator(softApConfiguration)) {
            intent.setAction(WifiDppConfiguratorActivity.ACTION_CONFIGURATOR_QR_CODE_GENERATOR);
        } else {
            return null;
        }

        final String ssid = removeFirstAndLastDoubleQuotes(softApConfiguration.getSsid());
        String security;
        final int securityType = softApConfiguration.getSecurityType();
        if (securityType == SoftApConfiguration.SECURITY_TYPE_WPA3_SAE) {
            security = WifiQrCode.SECURITY_SAE;
        } else if (securityType == SoftApConfiguration.SECURITY_TYPE_WPA2_PSK
                || securityType == SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION) {
            security = WifiQrCode.SECURITY_WPA_PSK;
        } else {
            security = WifiQrCode.SECURITY_NO_PASSWORD;
        }

        // When the value of this key is read, the actual key is not returned, just a "*".
        // Call privileged system API to obtain actual key.
        final String preSharedKey = removeFirstAndLastDoubleQuotes(
                softApConfiguration.getPassphrase());

        if (!TextUtils.isEmpty(ssid)) {
            intent.putExtra(EXTRA_WIFI_SSID, ssid);
        }
        if (!TextUtils.isEmpty(security)) {
            intent.putExtra(EXTRA_WIFI_SECURITY, security);
        }
        if (!TextUtils.isEmpty(preSharedKey)) {
            intent.putExtra(EXTRA_WIFI_PRE_SHARED_KEY, preSharedKey);
        }
        intent.putExtra(EXTRA_WIFI_HIDDEN_SSID, softApConfiguration.isHiddenSsid());


        intent.putExtra(EXTRA_WIFI_NETWORK_ID, WifiConfiguration.INVALID_NETWORK_ID);
        intent.putExtra(EXTRA_IS_HOTSPOT, true);

        return intent;
    }

    /**
     * Set all extra except {@code EXTRA_WIFI_NETWORK_ID} for the intent to
     * launch configurator activity later.
     *
     * @param intent the target to set extra
     * @param wifiManager an instance of {@code WifiManager}
     * @param wifiConfiguration the Wi-Fi network for launching configurator activity
     */
    private static void setConfiguratorIntentExtra(Intent intent, WifiManager wifiManager,
            WifiConfiguration wifiConfiguration) {
        final String ssid = removeFirstAndLastDoubleQuotes(wifiConfiguration.SSID);
        final String security = getSecurityString(wifiConfiguration);

        // When the value of this key is read, the actual key is not returned, just a "*".
        // Call privileged system API to obtain actual key.
        final String preSharedKey = removeFirstAndLastDoubleQuotes(getPresharedKey(wifiManager,
                wifiConfiguration));

        if (!TextUtils.isEmpty(ssid)) {
            intent.putExtra(EXTRA_WIFI_SSID, ssid);
        }
        if (!TextUtils.isEmpty(security)) {
            intent.putExtra(EXTRA_WIFI_SECURITY, security);
        }
        if (!TextUtils.isEmpty(preSharedKey)) {
            intent.putExtra(EXTRA_WIFI_PRE_SHARED_KEY, preSharedKey);
        }
        intent.putExtra(EXTRA_WIFI_HIDDEN_SSID, wifiConfiguration.hiddenSSID);
    }

    /**
     * Checks whether the device is unlocked recently.
     *
     * @param keyStoreAlias key
     * @param seconds how many seconds since the device is unlocked
     * @return whether the device is unlocked within the time
     */
    public static boolean isUnlockedWithinSeconds(String keyStoreAlias, int seconds) {
        try {
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS7_PADDING);
            cipher.init(Cipher.ENCRYPT_MODE, generateSecretKey(keyStoreAlias, seconds));
            cipher.doFinal();
            return true;
        } catch (NoSuchPaddingException
                 | IllegalBlockSizeException
                 | NoSuchAlgorithmException
                 | BadPaddingException
                 | InvalidKeyException e) {
            return false;
        }
    }

    private static SecretKey generateSecretKey(String keyStoreAlias, int seconds) {
        KeyGenParameterSpec spec = new KeyGenParameterSpec
                .Builder(keyStoreAlias, KeyProperties.PURPOSE_ENCRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationParameters(
                        seconds,
                        KeyProperties.AUTH_DEVICE_CREDENTIAL | KeyProperties.AUTH_BIOMETRIC_STRONG)
                .build();
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES);
            keyGenerator.init(spec);
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException
                 | InvalidAlgorithmParameterException e) {
            return null;
        }
    }

    /**
     * Shows authentication screen to confirm credentials (pin, pattern or password) for the current
     * user of the device.
     *
     * @param context The {@code Context} used to get {@code KeyguardManager} service
     * @param successRunnable The {@code Runnable} which will be executed if the user does not setup
     *                        device security or if lock screen is unlocked
     */
    public static void showLockScreen(Context context, Runnable successRunnable) {
        final KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(
                Context.KEYGUARD_SERVICE);

        if (keyguardManager.isKeyguardSecure()) {
            final BiometricPrompt.AuthenticationCallback authenticationCallback =
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(
                                    BiometricPrompt.AuthenticationResult result) {
                            successRunnable.run();
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, CharSequence errString) {
                            //Do nothing
                        }
            };

            final int userId = UserHandle.myUserId();

            final BiometricPrompt.Builder builder = new BiometricPrompt.Builder(context)
                    .setTitle(context.getText(R.string.wifi_dpp_lockscreen_title));

            if (keyguardManager.isDeviceSecure()) {
                builder.setDeviceCredentialAllowed(true);
                builder.setTextForDeviceCredential(
                        null /* title */,
                        Utils.getConfirmCredentialStringForUser(
                                context, userId, Utils.getCredentialType(context, userId)),
                        null /* description */);
            }

            final BiometricPrompt bp = builder.build();
            final Handler handler = new Handler(Looper.getMainLooper());
            bp.authenticate(new CancellationSignal(),
                    runnable -> handler.post(runnable),
                    authenticationCallback);
        } else {
            successRunnable.run();
        }
    }

    /**
     * Checks if QR code generator supports to config other devices with the Wi-Fi network
     *
     * @param context The context to use for {@code WifiManager}
     * @param accessPoint The {@link AccessPoint} of the Wi-Fi network
     */
    public static boolean isSupportConfiguratorQrCodeGenerator(Context context,
            AccessPoint accessPoint) {
        if (accessPoint.isPasspoint()) {
            return false;
        }
        return isSupportZxing(context, accessPoint.getSecurity());
    }

    /**
     * Checks if this device supports to be configured by the Wi-Fi network of the security
     *
     * @param context The context to use for {@code WifiManager}
     * @param wifiEntrySecurity The security constants defined in {@link WifiEntry}
     */
    public static boolean isSupportEnrolleeQrCodeScanner(Context context, int wifiEntrySecurity) {
        return isSupportWifiDpp(context, wifiEntrySecurity)
                || isSupportZxing(context, wifiEntrySecurity);
    }

    private static boolean isSupportHotspotConfiguratorQrCodeGenerator(
            SoftApConfiguration softApConfiguration) {
        final int securityType = softApConfiguration.getSecurityType();
        return securityType == SoftApConfiguration.SECURITY_TYPE_WPA3_SAE
                || securityType == SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION
                || securityType == SoftApConfiguration.SECURITY_TYPE_WPA2_PSK
                || securityType == SoftApConfiguration.SECURITY_TYPE_OPEN;
    }

    private static boolean isSupportWifiDpp(Context context, int wifiEntrySecurity) {
        if (!isWifiDppEnabled(context)) {
            return false;
        }

        // DPP 1.0 only supports SAE and PSK.
        final WifiManager wifiManager = context.getSystemService(WifiManager.class);
        switch (wifiEntrySecurity) {
            case WifiEntry.SECURITY_SAE:
                if (wifiManager.isWpa3SaeSupported()) {
                    return true;
                }
                break;
            case WifiEntry.SECURITY_PSK:
                return true;
            default:
        }
        return false;
    }

    private static boolean isSupportZxing(Context context, int wifiEntrySecurity) {
        final WifiManager wifiManager = context.getSystemService(WifiManager.class);
        switch (wifiEntrySecurity) {
            case WifiEntry.SECURITY_PSK:
            case WifiEntry.SECURITY_WEP:
            case WifiEntry.SECURITY_NONE:
                return true;
            case WifiEntry.SECURITY_SAE:
                if (wifiManager.isWpa3SaeSupported()) {
                    return true;
                }
                break;
            case WifiEntry.SECURITY_OWE:
                if (wifiManager.isEnhancedOpenSupported()) {
                    return true;
                }
                break;
            default:
        }
        return false;
    }

    static void triggerVibrationForQrCodeRecognition(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null) {
          return;
        }
        vibrator.vibrate(VibrationEffect.createOneShot(
                VIBRATE_DURATION_QR_CODE_RECOGNITION.toMillis(),
                VibrationEffect.DEFAULT_AMPLITUDE));
    }

    @WifiEntry.Security
    static int getSecurityTypeFromWifiConfiguration(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.SAE)) {
            return WifiEntry.SECURITY_SAE;
        }
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK)) {
            return WifiEntry.SECURITY_PSK;
        }
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.SUITE_B_192)) {
            return WifiEntry.SECURITY_EAP_SUITE_B;
        }
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP)
                || config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X)) {
            return WifiEntry.SECURITY_EAP;
        }
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.OWE)) {
            return WifiEntry.SECURITY_OWE;
        }
        return (config.wepKeys[0] != null) ? WifiEntry.SECURITY_WEP : WifiEntry.SECURITY_NONE;
    }
}
