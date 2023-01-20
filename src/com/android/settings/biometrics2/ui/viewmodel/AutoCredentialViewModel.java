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

package com.android.settings.biometrics2.ui.viewmodel;

import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;

import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_KEY_CHALLENGE;
import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_KEY_SENSOR_ID;
import static com.android.settings.biometrics2.ui.model.CredentialModel.INVALID_GK_PW_HANDLE;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE;

import android.annotation.IntDef;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.VerifyCredentialResponse;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.biometrics.BiometricUtils.GatekeeperCredentialNotMatchException;
import com.android.settings.biometrics2.data.repository.FingerprintRepository;
import com.android.settings.biometrics2.ui.model.CredentialModel;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockPattern;
import com.android.settings.password.ChooseLockSettingsHelper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * AutoCredentialViewModel which uses CredentialModel to determine next actions for activity, like
 * start ChooseLockActivity, start ConfirmLockActivity, GenerateCredential, or do nothing.
 */
public class AutoCredentialViewModel extends AndroidViewModel {

    private static final String TAG = "AutoCredentialViewModel";

    @VisibleForTesting
    static final String KEY_CREDENTIAL_MODEL = "credential_model";

    @VisibleForTesting
    static final String KEY_IS_GENERATING_CHALLENGE_DURING_CHECKING_CREDENTIAL =
            "is_generating_challenge_during_checking_credential";

    private static final boolean DEBUG = false;

    /**
     * Valid credential, activity does nothing.
     */
    public static final int CREDENTIAL_VALID = 0;

    /**
     * This credential looks good, but still need to run generateChallenge().
     */
    public static final int CREDENTIAL_IS_GENERATING_CHALLENGE = 1;

    /**
     * Need activity to run choose lock
     */
    public static final int CREDENTIAL_FAIL_NEED_TO_CHOOSE_LOCK = 2;

    /**
     * Need activity to run confirm lock
     */
    public static final int CREDENTIAL_FAIL_NEED_TO_CONFIRM_LOCK = 3;

    @IntDef(prefix = { "CREDENTIAL_" }, value = {
            CREDENTIAL_VALID,
            CREDENTIAL_IS_GENERATING_CHALLENGE,
            CREDENTIAL_FAIL_NEED_TO_CHOOSE_LOCK,
            CREDENTIAL_FAIL_NEED_TO_CONFIRM_LOCK
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface CredentialAction {}

    /**
     * Generic callback for FingerprintManager#generateChallenge or FaceManager#generateChallenge
     */
    public interface GenerateChallengeCallback {
        /**
         * Generic generateChallenge method for FingerprintManager or FaceManager
         */
        void onChallengeGenerated(int sensorId, int userId, long challenge);
    }

    /**
     * A generic interface class for calling different generateChallenge from FingerprintManager or
     * FaceManager
     */
    public interface ChallengeGenerator {
        /**
         * Get callback that will be called later after challenge generated
         */
        @Nullable
        GenerateChallengeCallback getCallback();

        /**
         * Set callback that will be called later after challenge generated
         */
        void setCallback(@Nullable GenerateChallengeCallback callback);

        /**
         * Method for generating challenge from FingerprintManager or FaceManager
         */
        void generateChallenge(int userId);
    }

    /**
     * Used to generate challenge through FingerprintRepository
     */
    public static class FingerprintChallengeGenerator implements ChallengeGenerator {

        private static final String TAG = "FingerprintChallengeGenerator";

        @NonNull
        private final FingerprintRepository mFingerprintRepository;

        @Nullable
        private GenerateChallengeCallback mCallback = null;

        public FingerprintChallengeGenerator(@NonNull FingerprintRepository fingerprintRepository) {
            mFingerprintRepository = fingerprintRepository;
        }

        @Nullable
        @Override
        public GenerateChallengeCallback getCallback() {
            return mCallback;
        }

        @Override
        public void setCallback(@Nullable GenerateChallengeCallback callback) {
            mCallback = callback;
        }

        @Override
        public void generateChallenge(int userId) {
            final GenerateChallengeCallback callback = mCallback;
            if (callback == null) {
                Log.e(TAG, "generateChallenge, null callback");
                return;
            }
            mFingerprintRepository.generateChallenge(userId, callback::onChallengeGenerated);
        }
    }

    @NonNull private final LockPatternUtils mLockPatternUtils;
    @NonNull private final ChallengeGenerator mChallengeGenerator;
    private CredentialModel mCredentialModel = null;
    @NonNull private final MutableLiveData<Boolean> mGenerateChallengeFailedLiveData =
            new MutableLiveData<>();

    // flag if token is generating through checkCredential()'s generateChallenge()
    private boolean mIsGeneratingChallengeDuringCheckingCredential;

    public AutoCredentialViewModel(
            @NonNull Application application,
            @NonNull LockPatternUtils lockPatternUtils,
            @NonNull ChallengeGenerator challengeGenerator) {
        super(application);
        mLockPatternUtils = lockPatternUtils;
        mChallengeGenerator = challengeGenerator;
    }

    /**
     * Set CredentialModel, the source is coming from savedInstanceState or activity intent
     */
    public void setCredentialModel(@Nullable Bundle savedInstanceState, @NonNull Intent intent) {
        final Bundle bundle;
        if (savedInstanceState != null) {
            bundle = savedInstanceState.getBundle(KEY_CREDENTIAL_MODEL);
            mIsGeneratingChallengeDuringCheckingCredential = savedInstanceState.getBoolean(
                    KEY_IS_GENERATING_CHALLENGE_DURING_CHECKING_CREDENTIAL);
        } else {
            bundle = intent.getExtras();
        }
        mCredentialModel = new CredentialModel(bundle, SystemClock.elapsedRealtimeClock());

        if (DEBUG) {
            Log.d(TAG, "setCredentialModel " + mCredentialModel + ", savedInstanceState exist:"
                    + (savedInstanceState != null));
        }
    }

    /**
     * Handle onSaveInstanceState from activity
     */
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(KEY_IS_GENERATING_CHALLENGE_DURING_CHECKING_CREDENTIAL,
                mIsGeneratingChallengeDuringCheckingCredential);
        outState.putBundle(KEY_CREDENTIAL_MODEL, mCredentialModel.getBundle());
    }

    @NonNull
    public LiveData<Boolean> getGenerateChallengeFailedLiveData() {
        return mGenerateChallengeFailedLiveData;
    }

    /**
     * Get bundle which passing back to FingerprintSettings for late generateChallenge()
     */
    @Nullable
    public Bundle createGeneratingChallengeExtras() {
        if (!mIsGeneratingChallengeDuringCheckingCredential
                || !mCredentialModel.isValidToken()
                || !mCredentialModel.isValidChallenge()) {
            return null;
        }

        Bundle bundle = new Bundle();
        bundle.putByteArray(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN,
                mCredentialModel.getToken());
        bundle.putLong(EXTRA_KEY_CHALLENGE, mCredentialModel.getChallenge());
        return bundle;
    }

    /**
     * Check credential status for biometric enrollment.
     */
    @CredentialAction
    public int checkCredential() {
        if (isValidCredential()) {
            return CREDENTIAL_VALID;
        }
        if (isUnspecifiedPassword()) {
            return CREDENTIAL_FAIL_NEED_TO_CHOOSE_LOCK;
        } else if (mCredentialModel.isValidGkPwHandle()) {
            final long gkPwHandle = mCredentialModel.getGkPwHandle();
            mCredentialModel.clearGkPwHandle();
            // GkPwHandle is got through caller activity, we shall not revoke it after
            // generateChallenge(). Let caller activity to make decision.
            generateChallenge(gkPwHandle, false /* revokeGkPwHandle */);
            mIsGeneratingChallengeDuringCheckingCredential = true;
            return CREDENTIAL_IS_GENERATING_CHALLENGE;
        } else {
            return CREDENTIAL_FAIL_NEED_TO_CONFIRM_LOCK;
        }
    }

    private void generateChallenge(long gkPwHandle, boolean revokeGkPwHandle) {
        mChallengeGenerator.setCallback((sensorId, userId, challenge) -> {
            try {
                final byte[] newToken = requestGatekeeperHat(gkPwHandle, challenge, userId);
                mCredentialModel.setSensorId(sensorId);
                mCredentialModel.setChallenge(challenge);
                mCredentialModel.setToken(newToken);
            } catch (IllegalStateException e) {
                Log.e(TAG, "generateChallenge, IllegalStateException", e);
                mGenerateChallengeFailedLiveData.postValue(true);
                return;
            }

            if (revokeGkPwHandle) {
                mLockPatternUtils.removeGatekeeperPasswordHandle(gkPwHandle);
            }

            if (DEBUG) {
                Log.d(TAG, "generateChallenge(), model:" + mCredentialModel
                        + ", revokeGkPwHandle:" + revokeGkPwHandle);
            }

            // Check credential again
            if (!isValidCredential()) {
                Log.w(TAG, "generateChallenge, invalid Credential");
                mGenerateChallengeFailedLiveData.postValue(true);
            }
        });
        mChallengeGenerator.generateChallenge(getUserId());
    }

    private boolean isValidCredential() {
        return !isUnspecifiedPassword() && mCredentialModel.isValidToken();
    }

    private boolean isUnspecifiedPassword() {
        return mLockPatternUtils.getActivePasswordQuality(getUserId())
                == PASSWORD_QUALITY_UNSPECIFIED;
    }

    /**
     * Handle activity result from ChooseLockGeneric, ConfirmLockPassword, or ConfirmLockPattern
     * @param isChooseLock true if result is coming from ChooseLockGeneric. False if result is
     *                     coming from ConfirmLockPassword or ConfirmLockPattern
     * @param result activity result
     * @return if it is a valid result
     */
    public boolean checkNewCredentialFromActivityResult(boolean isChooseLock,
            @NonNull ActivityResult result) {
        if ((isChooseLock && result.getResultCode() == ChooseLockPattern.RESULT_FINISHED)
                || (!isChooseLock && result.getResultCode() == Activity.RESULT_OK)) {
            final Intent data = result.getData();
            if (data != null) {
                final long gkPwHandle = result.getData().getLongExtra(
                        EXTRA_KEY_GK_PW_HANDLE, INVALID_GK_PW_HANDLE);
                // Revoke self requested GkPwHandle because it shall only used once inside this
                // activity lifecycle.
                generateChallenge(gkPwHandle, true /* revokeGkPwHandle */);
                return true;
            }
        }
        return false;
    }

    /**
     * Get userId for this credential
     */
    public int getUserId() {
        return mCredentialModel.getUserId();
    }

    /**
     * Get userId for this credential
     */
    @Nullable
    public byte[] getToken() {
        return mCredentialModel.getToken();
    }

    @Nullable
    private byte[] requestGatekeeperHat(long gkPwHandle, long challenge, int userId)
            throws IllegalStateException {
        final VerifyCredentialResponse response = mLockPatternUtils
                .verifyGatekeeperPasswordHandle(gkPwHandle, challenge, userId);
        if (!response.isMatched()) {
            throw new GatekeeperCredentialNotMatchException("Unable to request Gatekeeper HAT");
        }
        return response.getGatekeeperHAT();
    }

    /**
     * Get Credential intent extra which will be used to launch next activity.
     */
    @NonNull
    public Bundle createCredentialIntentExtra() {
        final Bundle retBundle = new Bundle();
        if (mCredentialModel.isValidGkPwHandle()) {
            retBundle.putLong(EXTRA_KEY_GK_PW_HANDLE, mCredentialModel.getGkPwHandle());
        }
        if (mCredentialModel.isValidToken()) {
            retBundle.putByteArray(EXTRA_KEY_CHALLENGE_TOKEN, mCredentialModel.getToken());
        }
        if (mCredentialModel.isValidUserId()) {
            retBundle.putInt(Intent.EXTRA_USER_ID, mCredentialModel.getUserId());
        }
        retBundle.putLong(EXTRA_KEY_CHALLENGE, mCredentialModel.getChallenge());
        retBundle.putInt(EXTRA_KEY_SENSOR_ID, mCredentialModel.getSensorId());
        return retBundle;
    }

    /**
     * Create Intent for choosing lock
     */
    @NonNull
    public Intent createChooseLockIntent(@NonNull Context context, boolean isSuw,
            @NonNull Bundle suwExtras) {
        final Intent intent = BiometricUtils.getChooseLockIntent(context, isSuw,
                suwExtras);
        intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_INSECURE_OPTIONS,
                true);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_REQUEST_GK_PW_HANDLE, true);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, true);

        if (mCredentialModel.isValidUserId()) {
            intent.putExtra(Intent.EXTRA_USER_ID, mCredentialModel.getUserId());
        }
        return intent;
    }

    /**
     * Create ConfirmLockLauncher
     */
    @NonNull
    public ChooseLockSettingsHelper createConfirmLockLauncher(@NonNull Activity activity,
            int requestCode, @NonNull String title) {
        final ChooseLockSettingsHelper.Builder builder =
                new ChooseLockSettingsHelper.Builder(activity);
        builder.setRequestCode(requestCode)
                .setTitle(title)
                .setRequestGatekeeperPasswordHandle(true)
                .setForegroundOnly(true)
                .setReturnCredentials(true);

        if (mCredentialModel.isValidUserId()) {
            builder.setUserId(mCredentialModel.getUserId());
        }
        return builder.build();
    }

}
