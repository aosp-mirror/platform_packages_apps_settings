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

import static androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import static androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY;

import static com.android.settings.biometrics2.factory.BiometricsViewModelFactory.CHALLENGE_GENERATOR_KEY;
import static com.android.settings.biometrics2.factory.BiometricsViewModelFactory.ENROLLMENT_REQUEST_KEY;
import static com.android.settings.biometrics2.factory.BiometricsViewModelFactory.USER_ID_KEY;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.CREDENTIAL_FAIL_NEED_TO_CHOOSE_LOCK;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.CREDENTIAL_FAIL_NEED_TO_CONFIRM_LOCK;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.CREDENTIAL_IS_GENERATING_CHALLENGE;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.CREDENTIAL_VALID;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel.FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_DIALOG;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel.FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_SKIP;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel.FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_START;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel.FingerprintEnrollFindSensorAction;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel.FINGERPRINT_ENROLL_INTRO_ACTION_CONTINUE_ENROLL;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel.FINGERPRINT_ENROLL_INTRO_ACTION_DONE_AND_FINISH;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel.FINGERPRINT_ENROLL_INTRO_ACTION_SKIP_OR_CANCEL;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel.FingerprintEnrollIntroAction;

import android.annotation.StyleRes;
import android.app.Application;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.viewmodel.CreationExtras;
import androidx.lifecycle.viewmodel.MutableCreationExtras;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling;
import com.android.settings.biometrics.fingerprint.SetupFingerprintEnrollEnrolling;
import com.android.settings.biometrics2.data.repository.FingerprintRepository;
import com.android.settings.biometrics2.factory.BiometricsViewModelFactory;
import com.android.settings.biometrics2.ui.model.CredentialModel;
import com.android.settings.biometrics2.ui.model.EnrollmentRequest;
import com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel;
import com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.FingerprintChallengeGenerator;
import com.android.settings.biometrics2.ui.viewmodel.DeviceFoldedViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollProgressViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollmentViewModel;
import com.android.settings.overlay.FeatureFactory;

import com.google.android.setupdesign.util.ThemeHelper;

/**
 * Fingerprint enrollment activity implementation
 */
public class FingerprintEnrollmentActivity extends FragmentActivity {

    private static final boolean DEBUG = false;
    private static final String TAG = "FingerprintEnrollmentActivity";

    private static final String INTRO_TAG = "enroll-intro";
    private static final String FIND_UDFPS_TAG = "enroll-find-udfps";
    private static final String FIND_SFPS_TAG = "enroll-find-sfps";
    private static final String FIND_RFPS_TAG = "enroll-find-rfps";
    private static final String SKIP_SETUP_FIND_FPS_DIALOG_TAG = "skip-setup-dialog";

    protected static final int LAUNCH_CONFIRM_LOCK_ACTIVITY = 1;

    private ViewModelProvider mViewModelProvider;
    private FingerprintEnrollmentViewModel mViewModel;
    private AutoCredentialViewModel mAutoCredentialViewModel;
    private final Observer<Integer> mIntroActionObserver = action -> {
        if (DEBUG) {
            Log.d(TAG, "mIntroActionObserver(" + action + ")");
        }
        if (action != null) {
            onIntroAction(action);
        }
    };
    private final Observer<Integer> mFindSensorActionObserver = action -> {
        if (DEBUG) {
            Log.d(TAG, "mFindSensorActionObserver(" + action + ")");
        }
        if (action != null) {
            onFindSensorAction(action);
        }
    };
    private final ActivityResultCallback<ActivityResult> mNextActivityResultCallback =
            result -> mViewModel.onContinueEnrollActivityResult(result,
                    mAutoCredentialViewModel.getUserId());
    private final ActivityResultLauncher<Intent> mNextActivityLauncher =
            registerForActivityResult(new StartActivityForResult(), mNextActivityResultCallback);
    private final ActivityResultCallback<ActivityResult> mChooseLockResultCallback =
            result -> onChooseOrConfirmLockResult(true /* isChooseLock */, result);
    private final ActivityResultLauncher<Intent> mChooseLockLauncher =
            registerForActivityResult(new StartActivityForResult(), mChooseLockResultCallback);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModelProvider = new ViewModelProvider(this);

        mViewModel = mViewModelProvider.get(FingerprintEnrollmentViewModel.class);
        mViewModel.setSavedInstanceState(savedInstanceState);

        mAutoCredentialViewModel = mViewModelProvider.get(AutoCredentialViewModel.class);
        mAutoCredentialViewModel.setCredentialModel(savedInstanceState, getIntent());

        // Theme
        setTheme(mViewModel.getRequest().getTheme());
        ThemeHelper.trySetDynamicColor(this);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

        // fragment
        setContentView(R.layout.biometric_enrollment_container);

        if (DEBUG) {
            Log.e(TAG, "onCreate() has savedInstance:" + (savedInstanceState != null));
        }
        if (savedInstanceState == null) {
            checkCredential();
            startIntroFragment();
        } else {
            final FragmentManager manager = getSupportFragmentManager();
            String[] tags = new String[] {
                    FIND_UDFPS_TAG,
                    FIND_SFPS_TAG,
                    FIND_RFPS_TAG,
                    INTRO_TAG
            };
            for (String tag: tags) {
                final Fragment fragment = manager.findFragmentByTag(tag);
                if (fragment == null) {
                    continue;
                }
                if (DEBUG) {
                    Log.e(TAG, "onCreate() currentFragment:" + tag);
                }
                if (tag.equals(INTRO_TAG)) {
                    attachIntroViewModel();
                } else { // FIND_UDFPS_TAG, FIND_SFPS_TAG, FIND_RFPS_TAG
                    attachFindSensorViewModel();
                    attachIntroViewModel();
                }
                break;
            }
        }

        // observe LiveData
        mViewModel.getSetResultLiveData().observe(this, this::onSetActivityResult);

        mAutoCredentialViewModel.getGenerateChallengeFailedLiveData().observe(this,
                this::onGenerateChallengeFailed);
    }

    private void startIntroFragment() {
        attachIntroViewModel();
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container_view, FingerprintEnrollIntroFragment.class, null,
                        INTRO_TAG)
                .commit();
    }

    private void attachIntroViewModel() {
        final FingerprintEnrollIntroViewModel introViewModel =
                mViewModelProvider.get(FingerprintEnrollIntroViewModel.class);

        // Clear ActionLiveData in FragmentViewModel to prevent getting previous action during
        // recreate, like press 'Agree' then press 'back' in FingerprintEnrollFindSensor activity.
        introViewModel.clearActionLiveData();
        introViewModel.getActionLiveData().observe(this, mIntroActionObserver);
    }

    // We need to make sure token is valid before entering find sensor page
    private void startFindSensorFragment() {
        attachFindSensorViewModel();
        if (mViewModel.canAssumeUdfps()) {
            // UDFPS does not need to start real fingerprint enrolling during finding sensor
            startFindFpsFragmentWithProgressViewModel(FingerprintEnrollFindUdfpsFragment.class,
                    FIND_UDFPS_TAG, false /* initProgressViewModel */);
        } else if (mViewModel.canAssumeSfps()) {
            startFindFpsFragmentWithProgressViewModel(FingerprintEnrollFindSfpsFragment.class,
                    FIND_SFPS_TAG, true /* initProgressViewModel */);
        } else {
            startFindFpsFragmentWithProgressViewModel(FingerprintEnrollFindRfpsFragment.class,
                    FIND_RFPS_TAG, true /* initProgressViewModel */);
        }
    }

    private void startFindFpsFragmentWithProgressViewModel(
            @NonNull Class<? extends Fragment> findFpsClass, @NonNull String tag,
            boolean initProgressViewModel) {
        if (initProgressViewModel) {
            final FingerprintEnrollProgressViewModel progressViewModel =
                    mViewModelProvider.get(FingerprintEnrollProgressViewModel.class);
            progressViewModel.setToken(mAutoCredentialViewModel.getToken());
        }
        final FingerprintEnrollFindSensorViewModel findSensorViewModel =
                mViewModelProvider.get(FingerprintEnrollFindSensorViewModel.class);
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(R.anim.sud_slide_next_in, R.anim.sud_slide_next_out,
                        R.anim.sud_slide_back_in, R.anim.sud_slide_back_out)
                .replace(R.id.fragment_container_view, findFpsClass, null, tag)
                .addToBackStack(tag)
                .commit();
    }

    private void attachFindSensorViewModel() {
        final FingerprintEnrollFindSensorViewModel findSensorViewModel =
                mViewModelProvider.get(FingerprintEnrollFindSensorViewModel.class);

        // Clear ActionLiveData in FragmentViewModel to prevent getting previous action during
        // recreate, like press 'Start' then press 'back' in FingerprintEnrollEnrolling activity.
        findSensorViewModel.clearActionLiveData();
        findSensorViewModel.getActionLiveData().observe(this, mFindSensorActionObserver);
    }

    private void startSkipSetupFindFpsDialog() {
        new SkipSetupFindFpsDialog().show(getSupportFragmentManager(),
                SKIP_SETUP_FIND_FPS_DIALOG_TAG);
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

    private void onIntroAction(@FingerprintEnrollIntroAction int action) {
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
                startFindSensorFragment();
            }
        }
    }

    private void onFindSensorAction(@FingerprintEnrollFindSensorAction int action) {
        switch (action) {
            case FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_SKIP: {
                onSetActivityResult(
                        new ActivityResult(BiometricEnrollBase.RESULT_SKIP, null));
                return;
            }
            case FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_DIALOG: {
                startSkipSetupFindFpsDialog();
                return;
            }
            case FINGERPRINT_ENROLL_FIND_SENSOR_ACTION_START: {
                final boolean isSuw = mViewModel.getRequest().isSuw();
                if (!mViewModel.isWaitingActivityResult().compareAndSet(false, true)) {
                    Log.w(TAG, "startNext, isSuw:" + isSuw + ", fail to set isWaiting flag");
                }
                Intent intent = new Intent(this, isSuw
                        ? SetupFingerprintEnrollEnrolling.class
                        : FingerprintEnrollEnrolling.class);
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
    protected void onApplyThemeResource(Resources.Theme theme, @StyleRes int resid, boolean first) {
        theme.applyStyle(R.style.SetupWizardPartnerResource, true);
        super.onApplyThemeResource(theme, resid, first);
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
        ret.set(CHALLENGE_GENERATOR_KEY, new FingerprintChallengeGenerator(repository));

        ret.set(ENROLLMENT_REQUEST_KEY, new EnrollmentRequest(getIntent(),
                getApplicationContext()));

        Bundle extras = getIntent().getExtras();
        final CredentialModel credentialModel = new CredentialModel(extras,
                SystemClock.elapsedRealtimeClock());
        ret.set(USER_ID_KEY, credentialModel.getUserId());

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
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        mViewModelProvider.get(DeviceFoldedViewModel.class).onConfigurationChanged(newConfig);
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mViewModel.onSaveInstanceState(outState);
        mAutoCredentialViewModel.onSaveInstanceState(outState);
    }
}
