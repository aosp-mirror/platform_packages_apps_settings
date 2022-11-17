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
import android.app.KeyguardManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory;
import androidx.lifecycle.viewmodel.CreationExtras;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.biometrics2.data.repository.FingerprintRepository;
import com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel;
import com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.ChallengeGenerator;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollmentViewModel;
import com.android.settings.overlay.FeatureFactory;

/**
 * View model factory for biometric enrollment fragment
 */
public class BiometricsViewModelFactory implements ViewModelProvider.Factory {

    private static final String TAG = "BiometricsViewModelFact";

    public static final CreationExtras.Key<ChallengeGenerator> CHALLENGE_GENERATOR =
            new CreationExtras.Key<>() {};

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass,
            @NonNull CreationExtras extras) {
        final Application application = extras.get(AndroidViewModelFactory.APPLICATION_KEY);

        if (application == null) {
            Log.w(TAG, "create, null application");
            return create(modelClass);
        }
        final FeatureFactory featureFactory = FeatureFactory.getFactory(application);
        final BiometricsRepositoryProvider provider = FeatureFactory.getFactory(application)
                .getBiometricsRepositoryProvider();

        if (modelClass.isAssignableFrom(FingerprintEnrollIntroViewModel.class)) {
            final FingerprintRepository repository = provider.getFingerprintRepository(application);
            if (repository != null) {
                return (T) new FingerprintEnrollIntroViewModel(application, repository);
            }
        } else if (modelClass.isAssignableFrom(FingerprintEnrollmentViewModel.class)) {
            final FingerprintRepository repository = provider.getFingerprintRepository(application);
            if (repository != null) {
                return (T) new FingerprintEnrollmentViewModel(application, repository,
                        application.getSystemService(KeyguardManager.class));
            }
        } else if (modelClass.isAssignableFrom(AutoCredentialViewModel.class)) {
            final LockPatternUtils lockPatternUtils =
                    featureFactory.getSecurityFeatureProvider().getLockPatternUtils(application);
            final ChallengeGenerator challengeGenerator = extras.get(CHALLENGE_GENERATOR);
            if (challengeGenerator != null) {
                return (T) new AutoCredentialViewModel(application, lockPatternUtils,
                        challengeGenerator);
            }
        }
        return create(modelClass);
    }
}
