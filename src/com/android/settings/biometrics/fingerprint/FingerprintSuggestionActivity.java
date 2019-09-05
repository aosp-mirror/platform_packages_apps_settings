/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.biometrics.fingerprint;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;

import com.android.settings.R;
import com.android.settings.Utils;

import com.google.android.setupcompat.template.FooterButton;

public class FingerprintSuggestionActivity extends SetupFingerprintEnrollIntroduction {

    @Override
    protected void initViews() {
        super.initViews();

        final FooterButton cancelButton = getCancelButton();
        cancelButton.setText(
                this, R.string.security_settings_fingerprint_enroll_introduction_cancel);
    }

    @Override
    public void finish() {
        // Always use RESULT_CANCELED because this action can be done multiple times
        setResult(RESULT_CANCELED);
        super.finish();
    }

    public static boolean isSuggestionComplete(Context context) {
        return !Utils.hasFingerprintHardware(context)
                || !isFingerprintEnabled(context)
                || isNotSingleFingerprintEnrolled(context);
    }

    private static boolean isNotSingleFingerprintEnrolled(Context context) {
        final FingerprintManager manager = Utils.getFingerprintManagerOrNull(context);
        return manager == null || manager.getEnrolledFingerprints().size() != 1;
    }

    static boolean isFingerprintEnabled(Context context) {
        final DevicePolicyManager dpManager =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        final int dpmFlags = dpManager.getKeyguardDisabledFeatures(null, /* admin */
                context.getUserId());
        return (dpmFlags & DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT) == 0;
    }
}
