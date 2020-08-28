/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.content.Context;
import android.text.TextUtils;

/**
 * Extension of WifiQrCode to support ADB QR code format.
 * It will be based on the ZXing format:
 *
 * WIFI:T:ADB;S:myname;P:mypassword;;
 */
public class AdbQrCode extends WifiQrCode {
    static final String SECURITY_ADB = "ADB";

    private WifiNetworkConfig mAdbConfig;

    public AdbQrCode(String qrCode) throws IllegalArgumentException {
        super(qrCode);

        // Only accept the zxing format.
        if (!WifiQrCode.SCHEME_ZXING_WIFI_NETWORK_CONFIG.equals(getScheme())) {
            throw new IllegalArgumentException("DPP format not supported for ADB QR code");
        }

        mAdbConfig = getWifiNetworkConfig();
        if (!SECURITY_ADB.equals(mAdbConfig.getSecurity())) {
            throw new IllegalArgumentException("Invalid security type");
        }

        if (TextUtils.isEmpty(mAdbConfig.getSsid())) {
            throw new IllegalArgumentException("Empty service name");
        }

        if (TextUtils.isEmpty(mAdbConfig.getPreSharedKey())) {
            throw new IllegalArgumentException("Empty password");
        }
    }

    public WifiNetworkConfig getAdbNetworkConfig() {
        return mAdbConfig;
    }

    /**
     * Triggers a vibration to notify of a valid QR code.
     *
     * @param context The context to use
     */
    public static void triggerVibrationForQrCodeRecognition(Context context) {
        WifiDppUtils.triggerVibrationForQrCodeRecognition(context);
    }
}
