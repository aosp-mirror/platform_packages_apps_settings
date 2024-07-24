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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory;
import androidx.lifecycle.viewmodel.CreationExtras;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.biometrics.fingerprint.FingerprintUpdater;
import com.android.settings.biometrics2.data.repository.FingerprintRepository;
import com.android.settings.biometrics2.ui.model.CredentialModel;
import com.android.settings.biometrics2.ui.model.EnrollmentRequest;
import com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel;
import com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.ChallengeGenerator;
import com.android.settings.biometrics2.ui.viewmodel.DeviceFoldedViewModel;
import com.android.settings.biometrics2.ui.viewmodel.DeviceRotationViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollErrorDialogViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFinishViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollProgressViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollmentViewModel;
import com.android.settings.overlay.FeatureFactory;
import com.android.systemui.unfold.compat.ScreenSizeFoldProvider;

/**
 * View model factory for biometric enrollment fragment
 */
public class BiometricsViewModelFactory implements ViewModelProvider.Factory {

    private static final String TAG = "BiometricsViewModelFactory";

    public static final CreationExtras.Key<ChallengeGenerator> CHALLENGE_GENERATOR_KEY =
            new CreationExtras.Key<ChallengeGenerator>() {};
    public static final CreationExtras.Key<EnrollmentRequest> ENROLLMENT_REQUEST_KEY =
            new CreationExtras.Key<EnrollmentRequest>() {};
    public static final CreationExtras.Key<CredentialModel> CREDENTIAL_MODEL_KEY =
            new CreationExtras.Key<CredentialModel>() {};

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
        final FeatureFactory featureFactory = FeatureFactory.getFeatureFactory();
        final BiometricsRepositoryProvider provider =
                featureFactory.getBiometricsRepositoryProvider();

        if (modelClass.isAssignableFrom(AutoCredentialViewModel.class)) {
            final LockPatternUtils lockPatternUtils =
                    featureFactory.getSecurityFeatureProvider().getLockPatternUtils(application);
            final ChallengeGenerator challengeGenerator = extras.get(CHALLENGE_GENERATOR_KEY);
            final CredentialModel credentialModel = extras.get(CREDENTIAL_MODEL_KEY);
            if (challengeGenerator != null && credentialModel != null) {
                return (T) new AutoCredentialViewModel(application, lockPatternUtils,
                        challengeGenerator, credentialModel);
            }
        } else if (modelClass.isAssignableFrom(DeviceFoldedViewModel.class)) {
            return (T) new DeviceFoldedViewModel(new ScreenSizeFoldProvider(application),
                    application.getMainExecutor());
        } else if (modelClass.isAssignableFrom(DeviceRotationViewModel.class)) {
            return (T) new DeviceRotationViewModel(application);
        } else if (modelClass.isAssignableFrom(FingerprintEnrollFindSensorViewModel.class)) {
            final EnrollmentRequest request = extras.get(ENROLLMENT_REQUEST_KEY);
            if (request != null) {
                return (T) new FingerprintEnrollFindSensorViewModel(application, request.isSuw());
            }
        } else if (modelClass.isAssignableFrom(FingerprintEnrollIntroViewModel.class)) {
            final FingerprintRepository repository = provider.getFingerprintRepository(application);
            final EnrollmentRequest request = extras.get(ENROLLMENT_REQUEST_KEY);
            final CredentialModel credentialModel = extras.get(CREDENTIAL_MODEL_KEY);
            if (repository != null && request != null && credentialModel != null) {
                return (T) new FingerprintEnrollIntroViewModel(application, repository, request,
                        credentialModel.getUserId());
            }
        } else if (modelClass.isAssignableFrom(FingerprintEnrollmentViewModel.class)) {
            final FingerprintRepository repository = provider.getFingerprintRepository(application);
            final EnrollmentRequest request = extras.get(ENROLLMENT_REQUEST_KEY);
            if (repository != null && request != null) {
                return (T) new FingerprintEnrollmentViewModel(application, repository, request);
            }
        } else if (modelClass.isAssignableFrom(FingerprintEnrollProgressViewModel.class)) {
            final CredentialModel credentialModel = extras.get(CREDENTIAL_MODEL_KEY);
            if (credentialModel != null) {
                return (T) new FingerprintEnrollProgressViewModel(application,
                        new FingerprintUpdater(application), credentialModel.getUserId());
            }
        } else if (modelClass.isAssignableFrom(FingerprintEnrollEnrollingViewModel.class)) {
            final CredentialModel credentialModel = extras.get(CREDENTIAL_MODEL_KEY);
            final FingerprintRepository fingerprint = provider.getFingerprintRepository(
                    application);
            if (fingerprint != null && credentialModel != null) {
                return (T) new FingerprintEnrollEnrollingViewModel(application,
                        credentialModel.getUserId(), fingerprint);
            }
        } else if (modelClass.isAssignableFrom(FingerprintEnrollFinishViewModel.class)) {
            final CredentialModel credentialModel = extras.get(CREDENTIAL_MODEL_KEY);
            final EnrollmentRequest request = extras.get(ENROLLMENT_REQUEST_KEY);
            final FingerprintRepository fingerprint = provider.getFingerprintRepository(
                    application);
            if (fingerprint != null && credentialModel != null && request != null) {
                return (T) new FingerprintEnrollFinishViewModel(application,
                        credentialModel.getUserId(), request, fingerprint);
            }
        } else if (modelClass.isAssignableFrom(FingerprintEnrollErrorDialogViewModel.class)) {
            final EnrollmentRequest request = extras.get(ENROLLMENT_REQUEST_KEY);
            if (request != null) {
                return (T) new FingerprintEnrollErrorDialogViewModel(application, request.isSuw());
            }
        }
        return create(modelClass);
    }
}
