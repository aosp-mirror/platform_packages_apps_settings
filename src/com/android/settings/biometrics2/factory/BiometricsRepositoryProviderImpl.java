/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.biometrics2.factory;

import android.app.Application;
import android.hardware.fingerprint.FingerprintManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.Utils;
import com.android.settings.biometrics2.data.repository.FingerprintRepository;

/**
 * Implementation for BiometricsRepositoryProvider
 */
public class BiometricsRepositoryProviderImpl implements BiometricsRepositoryProvider {

    private static volatile FingerprintRepository sFingerprintRepository;

    /**
     * Get FingerprintRepository
     */
    @Nullable
    @Override
    public FingerprintRepository getFingerprintRepository(@NonNull Application application) {
        final FingerprintManager fingerprintManager =
                Utils.getFingerprintManagerOrNull(application);
        if (fingerprintManager == null) {
            return null;
        }
        if (sFingerprintRepository == null) {
            synchronized (FingerprintRepository.class) {
                if (sFingerprintRepository == null) {
                    sFingerprintRepository = new FingerprintRepository(fingerprintManager);
                }
            }
        }
        return sFingerprintRepository;
    }
}
