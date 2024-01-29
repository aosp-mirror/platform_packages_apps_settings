/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static android.content.Intent.ACTION_CLOSE_SYSTEM_DIALOGS;
import static android.content.Intent.FLAG_RECEIVER_FOREGROUND;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.contract.ActivityResultContracts;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.InstrumentedActivity;

/**
 * Sharing a Wi-Fi network by QR code after unlocking. Used by {@code InternetDialog} in QS.
 */
public class WifiDppConfiguratorAuthActivity extends InstrumentedActivity {
    private static final String WIFI_SHARING_KEY_ALIAS = "wifi_sharing_auth_key";
    private static final int MAX_UNLOCK_SECONDS = 60;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This is a transparent activity, disable the dim.
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        Intent authIntent = getSystemService(KeyguardManager.class)
                .createConfirmDeviceCredentialIntent(
                        getText(R.string.wifi_dpp_lockscreen_title), null, getUserId());
        if (authIntent == null
                || WifiDppUtils.isUnlockedWithinSeconds(
                        WIFI_SHARING_KEY_ALIAS, MAX_UNLOCK_SECONDS)) {
            startQrCodeActivity();
            finish();
        } else {
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    this::onAuthResult).launch(authIntent);
        }
    }

    @VisibleForTesting
    void onAuthResult(ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK) {
            startQrCodeActivity();
        }
        finish();
    }

    private void startQrCodeActivity() {
        // Close quick settings shade
        sendBroadcast(
                new Intent(ACTION_CLOSE_SYSTEM_DIALOGS).setFlags(FLAG_RECEIVER_FOREGROUND));
        Intent qrCodeIntent = new Intent();
        qrCodeIntent.setAction(
                WifiDppConfiguratorActivity.ACTION_CONFIGURATOR_QR_CODE_GENERATOR);
        qrCodeIntent.putExtras(getIntent());
        startActivity(qrCodeIntent);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_WIFI_DPP_CONFIGURATOR;
    }
}
