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

package com.android.settings.biometrics2.ui.view;

import static androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY;

import static com.android.settings.biometrics2.factory.BiometricsViewModelFactory.CHALLENGE_GENERATOR;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.CREDENTIAL_FAIL_NEED_TO_CHOOSE_LOCK;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.CREDENTIAL_FAIL_NEED_TO_CONFIRM_LOCK;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.CREDENTIAL_IS_GENERATING_CHALLENGE;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.CREDENTIAL_VALID;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel.FINGERPRINT_ENROLL_INTRO_ACTION_CONTINUE_ENROLL;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel.FINGERPRINT_ENROLL_INTRO_ACTION_DONE_AND_FINISH;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel.FINGERPRINT_ENROLL_INTRO_ACTION_SKIP_OR_CANCEL;

import android.app.Application;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.viewmodel.CreationExtras;
import androidx.lifecycle.viewmodel.MutableCreationExtras;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.biometrics.fingerprint.FingerprintEnrollFindSensor;
import com.android.settings.biometrics.fingerprint.SetupFingerprintEnrollFindSensor;
import com.android.settings.biometrics2.data.repository.FingerprintRepository;
import com.android.settings.biometrics2.factory.BiometricsViewModelFactory;
import com.android.settings.biometrics2.ui.model.EnrollmentRequest;
import com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel;
import com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.FingerprintChallengeGenerator;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollmentViewModel;
import com.android.settings.overlay.FeatureFactory;

import com.google.android.setupdesign.util.ThemeHelper;

/**
 * Fingerprint enrollment activity implementation
 */
public class FingerprintEnrollmentActivity extends FragmentActivity {

    private static final String TAG = "FingerprintEnrollmentActivity";

    protected static final int LAUNCH_CONFIRM_LOCK_ACTIVITY = 1;

    private FingerprintEnrollmentViewModel mViewModel;
    private AutoCredentialViewModel mAutoCredentialViewModel;
    private ActivityResultLauncher<Intent> mNextActivityLauncher;
    private ActivityResultLauncher<Intent> mChooseLockLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNextActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                (it) -> mViewModel.onContinueEnrollActivityResult(
                        it,
                        mAutoCredentialViewModel.getUserId())
        );
        mChooseLockLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                (it) -> onChooseOrConfirmLockResult(true, it)
        );

        ViewModelProvider viewModelProvider = new ViewModelProvider(this);

        mViewModel = viewModelProvider.get(FingerprintEnrollmentViewModel.class);
        mViewModel.setRequest(new EnrollmentRequest(getIntent(), getApplicationContext()));
        mViewModel.setSavedInstanceState(savedInstanceState);

        mAutoCredentialViewModel = viewModelProvider.get(AutoCredentialViewModel.class);
        mAutoCredentialViewModel.setCredentialModel(savedInstanceState, getIntent());

        // Theme
        setTheme(mViewModel.getRequest().getTheme());
        ThemeHelper.trySetDynamicColor(this);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

        // fragment
        setContentView(R.layout.biometric_enrollment_container);
        final FingerprintEnrollIntroViewModel introViewModel =
                viewModelProvider.get(FingerprintEnrollIntroViewModel.class);
        introViewModel.setEnrollmentRequest(mViewModel.getRequest());
        introViewModel.setUserId(mAutoCredentialViewModel.getUserId());

        if (savedInstanceState == null) {
            checkCredential();

            final String tag = "FingerprintEnrollIntroFragment";
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.fragment_container_view, FingerprintEnrollIntroFragment.class, null,
                            tag)
                    .commit();
        }

        // observe LiveData
        getLifecycle().addObserver(mViewModel);
        mViewModel.getSetResultLiveData().observe(this, this::onSetActivityResult);

        mAutoCredentialViewModel.getGenerateChallengeFailedLiveData().observe(this,
                this::onGenerateChallengeFailed);

        // Clear ActionLiveData in FragmentViewModel to prevent getting previous action during
        // recreate, like press 'I agree' then press 'back' in FingerprintEnrollFindSensor activity.
        introViewModel.clearActionLiveData();
        introViewModel.getActionLiveData().observe(this, this::observeIntroAction);
    }

    private void onGenerateChallengeFailed(@NonNull Boolean ignoredBoolean) {
        onSetActivityResult(new ActivityResult(RESULT_CANCELED, null));
    }

    /**
     * Get intent which passing back to FingerprintSettings for late generateChallenge()
     */
    @Nullable
    private Intent createSetResultIntentWithGeneratingChallengeExtra(
            @Nullable Intent activityResultIntent) {
        if (!mViewModel.getRequest().isFromSettingsSummery()) {
            return activityResultIntent;
        }

        final Bundle extra = mAutoCredentialViewModel.createGeneratingChallengeExtras();
        if (extra != null) {
            if (activityResultIntent == null) {
                activityResultIntent = new Intent();
            }
            activityResultIntent.putExtras(extra);
        }
        return activityResultIntent;
    }

    private void onSetActivityResult(@NonNull ActivityResult result) {
        final int resultCode = mViewModel.getRequest().isAfterSuwOrSuwSuggestedAction()
                ? RESULT_CANCELED
                : result.getResultCode();
        final Intent intent = resultCode == BiometricEnrollBase.RESULT_FINISHED
                ? createSetResultIntentWithGeneratingChallengeExtra(result.getData())
                : result.getData();
        setResult(resultCode, intent);
        finish();
    }

    private void checkCredential() {
        switch (mAutoCredentialViewModel.checkCredential()) {
            case CREDENTIAL_FAIL_NEED_TO_CHOOSE_LOCK: {
                final Intent intent = mAutoCredentialViewModel.createChooseLockIntent(this,
                        mViewModel.getRequest().isSuw(), mViewModel.getRequest().getSuwExtras());
                if (!mViewModel.isWaitingActivityResult().compareAndSet(false, true)) {
                    Log.w(TAG, "chooseLock, fail to set isWaiting flag to true");
                }
                mChooseLockLauncher.launch(intent);
                return;
            }
            case CREDENTIAL_FAIL_NEED_TO_CONFIRM_LOCK: {
                final boolean launched = mAutoCredentialViewModel.createConfirmLockLauncher(
                        this,
                        LAUNCH_CONFIRM_LOCK_ACTIVITY,
                        getString(R.string.security_settings_fingerprint_preference_title)
                ).launch();
                if (!launched) {
                    // This shouldn't happen, as we should only end up at this step if a lock thingy
                    // is already set.
                    Log.e(TAG, "confirmLock, launched is true");
                    finish();
                } else if (!mViewModel.isWaitingActivityResult().compareAndSet(false, true)) {
                    Log.w(TAG, "confirmLock, fail to set isWaiting flag to true");
                }
                return;
            }
            case CREDENTIAL_VALID:
            case CREDENTIAL_IS_GENERATING_CHALLENGE: {
                // Do nothing
            }
        }
    }

    private void onChooseOrConfirmLockResult(boolean isChooseLock,
            @NonNull ActivityResult activityResult) {
        if (!mViewModel.isWaitingActivityResult().compareAndSet(true, false)) {
            Log.w(TAG, "isChooseLock:" + isChooseLock + ", fail to unset waiting flag");
        }
        if (mAutoCredentialViewModel.checkNewCredentialFromActivityResult(
                isChooseLock, activityResult)) {
            overridePendingTransition(R.anim.sud_slide_next_in, R.anim.sud_slide_next_out);
        } else {
            onSetActivityResult(activityResult);
        }
    }

    private void observeIntroAction(@Nullable Integer action) {
        if (action == null) {
            return;
        }
        switch (action) {
            case FINGERPRINT_ENROLL_INTRO_ACTION_DONE_AND_FINISH: {
                onSetActivityResult(
                        new ActivityResult(BiometricEnrollBase.RESULT_FINISHED, null));
                return;
            }
            case FINGERPRINT_ENROLL_INTRO_ACTION_SKIP_OR_CANCEL: {
                onSetActivityResult(
                        new ActivityResult(BiometricEnrollBase.RESULT_SKIP, null));
                return;
            }
            case FINGERPRINT_ENROLL_INTRO_ACTION_CONTINUE_ENROLL: {
                final boolean isSuw = mViewModel.getRequest().isSuw();
                if (!mViewModel.isWaitingActivityResult().compareAndSet(false, true)) {
                    Log.w(TAG, "startNext, isSuw:" + isSuw + ", fail to set isWaiting flag");
                }
                final Intent intent = new Intent(this, isSuw
                        ? SetupFingerprintEnrollFindSensor.class
                        : FingerprintEnrollFindSensor.class);
                intent.putExtras(mAutoCredentialViewModel.createCredentialIntentExtra());
                intent.putExtras(mViewModel.getNextActivityBaseIntentExtras());
                mNextActivityLauncher.launch(intent);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mViewModel.checkFinishActivityDuringOnPause(isFinishing(), isChangingConfigurations());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == LAUNCH_CONFIRM_LOCK_ACTIVITY) {
            onChooseOrConfirmLockResult(false, new ActivityResult(resultCode, data));
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @NonNull
    @Override
    public CreationExtras getDefaultViewModelCreationExtras() {
        final Application application =
                super.getDefaultViewModelCreationExtras().get(APPLICATION_KEY);
        final MutableCreationExtras ret = new MutableCreationExtras();
        ret.set(APPLICATION_KEY, application);
        final FingerprintRepository repository = FeatureFactory.getFactory(application)
                .getBiometricsRepositoryProvider().getFingerprintRepository(application);
        ret.set(CHALLENGE_GENERATOR, new FingerprintChallengeGenerator(repository));
        return ret;
    }

    @NonNull
    @Override
    public ViewModelProvider.Factory getDefaultViewModelProviderFactory() {
        return new BiometricsViewModelFactory();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        getWindow().setStatusBarColor(getBackgroundColor());
    }

    @ColorInt
    private int getBackgroundColor() {
        final ColorStateList stateList = Utils.getColorAttr(this, android.R.attr.windowBackground);
        return stateList != null ? stateList.getDefaultColor() : Color.TRANSPARENT;
    }

    @Override
    protected void onDestroy() {
        getLifecycle().removeObserver(mViewModel);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mViewModel.onSaveInstanceState(outState);
        mAutoCredentialViewModel.onSaveInstanceState(outState);
    }
}
