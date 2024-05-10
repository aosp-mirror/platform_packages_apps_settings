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

package com.android.settings.biometrics.fingerprint;

import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;

/**
 * Preference controller that controls the fingerprint unlock features to be shown / be hidden.
 */
public class FingerprintUnlockCategoryController extends BasePreferenceController {
    private static final String TAG = "FingerprintUnlockCategoryPreferenceController";

    private int mUserId;
    @VisibleForTesting
    protected FingerprintManager mFingerprintManager;

    public FingerprintUnlockCategoryController(Context context, String key) {
        super(context, key);
        mFingerprintManager = Utils.getFingerprintManagerOrNull(context);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mFingerprintManager != null
                && mFingerprintManager.isHardwareDetected()
                && mFingerprintManager.isPowerbuttonFps()) {
            return mFingerprintManager.hasEnrolledTemplates(getUserId())
                    ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
        } else {
            return UNSUPPORTED_ON_DEVICE;
        }
    }

    public void setUserId(int userId) {
        mUserId = userId;
    }

    protected int getUserId() {
        return mUserId;
    }
}
