/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.password;

import android.annotation.NonNull;
import android.hardware.fingerprint.FingerprintManager;

import com.android.internal.util.Preconditions;

/**
 * Wrapper of {@link FingerprintManager}. Workaround for roboelectic testing. See
 * {@link IFingerprintManager} for details.
 */
public class FingerprintManagerWrapper implements IFingerprintManager {
    private @NonNull FingerprintManager mFingerprintManager;

    public FingerprintManagerWrapper(@NonNull FingerprintManager fingerprintManager) {
        Preconditions.checkNotNull(fingerprintManager);
        mFingerprintManager = fingerprintManager;
    }

    public boolean isHardwareDetected() {
        return mFingerprintManager.isHardwareDetected();
    }

    public boolean hasEnrolledFingerprints(int userId) {
        return mFingerprintManager.hasEnrolledFingerprints(userId);
    }

    public long preEnroll() {
        return mFingerprintManager.preEnroll();
    }
}
