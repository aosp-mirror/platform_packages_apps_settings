/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.biometrics.face;

import static android.app.admin.DevicePolicyResources.Strings.Settings.FACE_UNLOCK_DISABLED;

import static com.android.settings.biometrics.BiometricUtils.GatekeeperCredentialNotMatchException;

import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.SensorPrivacyManager;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.SensorProperties;
import android.hardware.face.FaceManager;
import android.hardware.face.FaceSensorPropertiesInternal;
import android.hardware.face.IFaceAuthenticatorsRegisteredCallback;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricEnrollActivity;
import com.android.settings.biometrics.BiometricEnrollIntroduction;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.biometrics.MultiBiometricEnrollHelper;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.password.SetupSkipDialog;
import com.android.settings.utils.SensorPrivacyManagerHelper;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.systemui.unfold.compat.ScreenSizeFoldProvider;
import com.android.systemui.unfold.updates.FoldProvider;

import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.span.LinkSpan;

import java.util.List;

/**
 * Provides introductory info about face unlock and prompts the user to agree before starting face
 * enrollment.
 */
public class FaceEnrollIntroduction extends BiometricEnrollIntroduction {
    private static final String TAG = "FaceEnrollIntroduction";

    private FaceManager mFaceManager;
    @Nullable private FooterButton mPrimaryFooterButton;
    @Nullable private FooterButton mSecondaryFooterButton;
    @Nullable private SensorPrivacyManager mSensorPrivacyManager;
    private boolean mIsFaceStrong;

    @Override
    protected void onCancelButtonClick(View view) {
        if (!BiometricUtils.tryStartingNextBiometricEnroll(this, ENROLL_NEXT_BIOMETRIC_REQUEST,
                "cancel")) {
            super.onCancelButtonClick(view);
        }
    }

    @Override
    protected void onSkipButtonClick(View view) {
        if (!BiometricUtils.tryStartingNextBiometricEnroll(this, ENROLL_NEXT_BIOMETRIC_REQUEST,
                "skip")) {
            super.onSkipButtonClick(view);
        }
    }

    @Override
    protected void onEnrollmentSkipped(@Nullable Intent data) {
        if (!BiometricUtils.tryStartingNextBiometricEnroll(this, ENROLL_NEXT_BIOMETRIC_REQUEST,
                "skipped")) {
            super.onEnrollmentSkipped(data);
        }
    }

    @Override
    protected void onFinishedEnrolling(@Nullable Intent data) {
        if (!BiometricUtils.tryStartingNextBiometricEnroll(this, ENROLL_NEXT_BIOMETRIC_REQUEST,
                "finished")) {
            super.onFinishedEnrolling(data);
        }
    }

    @Override
    protected boolean shouldFinishWhenBackgrounded() {
        return super.shouldFinishWhenBackgrounded() && !BiometricUtils.isPostureGuidanceShowing(
                mDevicePostureState, mLaunchedPostureGuidance);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mFaceManager = getFaceManager();

        super.onCreate(savedInstanceState);

        if (savedInstanceState == null
                && !WizardManagerHelper.isAnySetupWizard(getIntent())
                && !getIntent().getBooleanExtra(EXTRA_FROM_SETTINGS_SUMMARY, false)
                && maxFacesEnrolled()) {
            // from tips && maxEnrolled
            Log.d(TAG, "launch face settings");
            launchFaceSettingsActivity();
            finish();
        }

        // Wait super::onCreated() then return because SuperNotCalledExceptio will be thrown
        // if we don't wait for it.
        if (isFinishing()) {
            return;
        }

        // Apply extracted theme color to icons.
        final ImageView iconGlasses = findViewById(R.id.icon_glasses);
        final ImageView iconLooking = findViewById(R.id.icon_looking);
        iconGlasses.getBackground().setColorFilter(getIconColorFilter());
        iconLooking.getBackground().setColorFilter(getIconColorFilter());

        // Set text for views with multiple variations.
        final TextView infoMessageGlasses = findViewById(R.id.info_message_glasses);
        final TextView infoMessageLooking = findViewById(R.id.info_message_looking);
        final TextView howMessage = findViewById(R.id.how_message);
        final TextView inControlTitle = findViewById(R.id.title_in_control);
        final TextView inControlMessage = findViewById(R.id.message_in_control);
        final TextView lessSecure = findViewById(R.id.info_message_less_secure);
        infoMessageGlasses.setText(getInfoMessageGlasses());
        infoMessageLooking.setText(getInfoMessageLooking());
        inControlTitle.setText(getInControlTitle());
        howMessage.setText(getHowMessage());
        inControlMessage.setText(Html.fromHtml(getString(getInControlMessage()),
                Html.FROM_HTML_MODE_LEGACY));
        inControlMessage.setMovementMethod(LinkMovementMethod.getInstance());
        lessSecure.setText(getLessSecureMessage());

        // Set up and show the "require eyes" info section if necessary.
        if (getResources().getBoolean(R.bool.config_face_intro_show_require_eyes)) {
            final LinearLayout infoRowRequireEyes = findViewById(R.id.info_row_require_eyes);
            final ImageView iconRequireEyes = findViewById(R.id.icon_require_eyes);
            final TextView infoMessageRequireEyes = findViewById(R.id.info_message_require_eyes);
            infoRowRequireEyes.setVisibility(View.VISIBLE);
            iconRequireEyes.getBackground().setColorFilter(getIconColorFilter());
            infoMessageRequireEyes.setText(getInfoMessageRequireEyes());
        }

        mFaceManager.addAuthenticatorsRegisteredCallback(
                new IFaceAuthenticatorsRegisteredCallback.Stub() {
                    @Override
                    public void onAllAuthenticatorsRegistered(
                            @NonNull List<FaceSensorPropertiesInternal> sensors) {
                        if (sensors.isEmpty()) {
                            Log.e(TAG, "No sensors");
                            return;
                        }

                        boolean isFaceStrong = sensors.get(0).sensorStrength
                                == SensorProperties.STRENGTH_STRONG;
                        mIsFaceStrong = isFaceStrong;
                        onFaceStrengthChanged();
                    }
                });

        // This path is an entry point for SetNewPasswordController, e.g.
        // adb shell am start -a android.app.action.SET_NEW_PASSWORD
        if (mToken == null && BiometricUtils.containsGatekeeperPasswordHandle(getIntent())) {
            if (generateChallengeOnCreate()) {
                mFooterBarMixin.getPrimaryButton().setEnabled(false);
                // We either block on generateChallenge, or need to gray out the "next" button until
                // the challenge is ready. Let's just do this for now.
                mFaceManager.generateChallenge(mUserId, (sensorId, userId, challenge) -> {
                    if (isFinishing()) {
                        // Do nothing if activity is finishing
                        Log.w(TAG, "activity finished before challenge callback launched.");
                        return;
                    }

                    try {
                        mToken = requestGatekeeperHat(challenge);
                        mSensorId = sensorId;
                        mChallenge = challenge;
                        mFooterBarMixin.getPrimaryButton().setEnabled(true);
                    } catch (GatekeeperCredentialNotMatchException e) {
                        // Let BiometricEnrollBase#onCreate() to trigger confirmLock()
                        getIntent().removeExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE);
                        recreate();
                    }
                });
            }
        }

        mSensorPrivacyManager = getApplicationContext()
                .getSystemService(SensorPrivacyManager.class);
        final SensorPrivacyManagerHelper helper = SensorPrivacyManagerHelper
                .getInstance(getApplicationContext());
        final boolean cameraPrivacyEnabled = helper
                .isSensorBlocked(SensorPrivacyManagerHelper.SENSOR_CAMERA);
        Log.v(TAG, "cameraPrivacyEnabled : " + cameraPrivacyEnabled);
    }

    private void launchFaceSettingsActivity() {
        final Intent intent = new Intent(this, Settings.FaceSettingsInternalActivity.class);
        final byte[] token = getIntent().getByteArrayExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
        if (token != null) {
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, token);
        }
        final int userId = getIntent().getIntExtra(Intent.EXTRA_USER_ID, UserHandle.myUserId());
        if (userId != UserHandle.USER_NULL) {
            intent.putExtra(Intent.EXTRA_USER_ID, userId);
        }
        BiometricUtils.copyMultiBiometricExtras(getIntent(), intent);
        intent.putExtra(EXTRA_FROM_SETTINGS_SUMMARY, true);
        intent.putExtra(EXTRA_KEY_CHALLENGE, getIntent().getLongExtra(EXTRA_KEY_CHALLENGE, -1L));
        intent.putExtra(EXTRA_KEY_SENSOR_ID, getIntent().getIntExtra(EXTRA_KEY_SENSOR_ID, -1));
        startActivity(intent);
    }

    @VisibleForTesting
    @Nullable
    protected FaceManager getFaceManager() {
        return Utils.getFaceManagerOrNull(this);
    }

    @VisibleForTesting
    @Nullable
    protected Intent getPostureGuidanceIntent() {
        return mPostureGuidanceIntent;
    }

    @VisibleForTesting
    @Nullable
    protected FoldProvider.FoldCallback getPostureCallback() {
        return mFoldCallback;
    }

    @VisibleForTesting
    @BiometricUtils.DevicePostureInt
    protected int getDevicePostureState() {
        return mDevicePostureState;
    }

    @VisibleForTesting
    @Nullable
    protected byte[] requestGatekeeperHat(long challenge) {
        return BiometricUtils.requestGatekeeperHat(this, getIntent(), mUserId, challenge);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mScreenSizeFoldProvider != null && getPostureCallback() != null) {
            mScreenSizeFoldProvider.onConfigurationChange(newConfig);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenFoldEventForPostureGuidance();
    }

    private void listenFoldEventForPostureGuidance() {
        if (maxFacesEnrolled()) {
            Log.d(TAG, "Device has enrolled face, do not show posture guidance");
            return;
        }

        if (getPostureGuidanceIntent() == null) {
            Log.d(TAG, "Device do not support posture guidance");
            return;
        }

        BiometricUtils.setDevicePosturesAllowEnroll(
                getResources().getInteger(R.integer.config_face_enroll_supported_posture));

        if (getPostureCallback() == null) {
            mFoldCallback = isFolded -> {
                mDevicePostureState = isFolded ? BiometricUtils.DEVICE_POSTURE_CLOSED
                        : BiometricUtils.DEVICE_POSTURE_OPENED;
                if (BiometricUtils.shouldShowPostureGuidance(mDevicePostureState,
                        mLaunchedPostureGuidance) && !mNextLaunched) {
                    launchPostureGuidance();
                }
            };
        }

        if (mScreenSizeFoldProvider == null) {
            mScreenSizeFoldProvider = new ScreenSizeFoldProvider(getApplicationContext());
            mScreenSizeFoldProvider.registerCallback(mFoldCallback, getMainExecutor());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_POSTURE_GUIDANCE) {
            mLaunchedPostureGuidance = false;
            if (resultCode == RESULT_CANCELED || resultCode == RESULT_SKIP) {
                onSkipButtonClick(getCurrentFocus());
            }
            return;
        }

        // If user has skipped or finished enrolling, don't restart enrollment.
        final boolean isEnrollRequest = requestCode == BIOMETRIC_FIND_SENSOR_REQUEST
                || requestCode == ENROLL_NEXT_BIOMETRIC_REQUEST;
        final boolean isResultSkipOrFinished = resultCode == RESULT_SKIP
                || resultCode == SetupSkipDialog.RESULT_SKIP || resultCode == RESULT_FINISHED;
        boolean hasEnrolledFace = false;
        if (data != null) {
            hasEnrolledFace = data.getBooleanExtra(EXTRA_FINISHED_ENROLL_FACE, false);
        }

        if (resultCode == RESULT_CANCELED) {
            if (hasEnrolledFace || !BiometricUtils.isPostureAllowEnrollment(mDevicePostureState)) {
                setResult(resultCode, data);
                finish();
                return;
            }
        }

        if (isEnrollRequest && isResultSkipOrFinished || hasEnrolledFace) {
            data = setSkipPendingEnroll(data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    protected boolean generateChallengeOnCreate() {
        return true;
    }

    @StringRes
    protected int getInfoMessageGlasses() {
        return R.string.security_settings_face_enroll_introduction_info_glasses;
    }

    @StringRes
    protected int getInfoMessageLooking() {
        return R.string.security_settings_face_enroll_introduction_info_looking;
    }

    @StringRes
    protected int getInfoMessageRequireEyes() {
        return R.string.security_settings_face_enroll_introduction_info_gaze;
    }

    @StringRes
    protected int getHowMessage() {
        return R.string.security_settings_face_enroll_introduction_how_message;
    }

    @StringRes
    protected int getInControlTitle() {
        return R.string.security_settings_face_enroll_introduction_control_title;
    }

    @StringRes
    protected int getInControlMessage() {
        return R.string.security_settings_face_enroll_introduction_control_message;
    }

    @StringRes
    protected int getLessSecureMessage() {
        return R.string.security_settings_face_enroll_introduction_info_less_secure;
    }

    @Override
    protected boolean isDisabledByAdmin() {
        return RestrictedLockUtilsInternal.checkIfKeyguardFeaturesDisabled(
                this, DevicePolicyManager.KEYGUARD_DISABLE_FACE, mUserId) != null;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.face_enroll_introduction;
    }

    @Override
    protected int getHeaderResDisabledByAdmin() {
        return R.string.security_settings_face_enroll_introduction_title_unlock_disabled;
    }

    @Override
    protected int getHeaderResDefault() {
        return R.string.security_settings_face_enroll_introduction_title;
    }

    @Override
    protected String getDescriptionDisabledByAdmin() {
        DevicePolicyManager devicePolicyManager = getSystemService(DevicePolicyManager.class);
        return devicePolicyManager.getResources().getString(
                FACE_UNLOCK_DISABLED,
                () -> getString(R.string.security_settings_face_enroll_introduction_message_unlock_disabled));
    }

    @Override
    protected FooterButton getCancelButton() {
        if (mFooterBarMixin != null) {
            return mFooterBarMixin.getSecondaryButton();
        }
        return null;
    }

    @Override
    protected FooterButton getNextButton() {
        if (mFooterBarMixin != null) {
            return mFooterBarMixin.getPrimaryButton();
        }
        return null;
    }

    @Override
    protected TextView getErrorTextView() {
        return findViewById(R.id.error_text);
    }

    private boolean maxFacesEnrolled() {
        if (mFaceManager != null) {
            // This will need to be updated for devices with multiple face sensors.
            final int numEnrolledFaces = mFaceManager.getEnrolledFaces(mUserId).size();
            final int maxFacesEnrollable = getApplicationContext().getResources()
                    .getInteger(R.integer.suw_max_faces_enrollable);
            return numEnrolledFaces >= maxFacesEnrollable;
        } else {
            return false;
        }
    }

    //TODO: Refactor this to something that conveys it is used for getting a string ID.
    @Override
    protected int checkMaxEnrolled() {
        if (mFaceManager != null) {
            if (maxFacesEnrolled()) {
                return R.string.face_intro_error_max;
            }
        } else {
            return R.string.face_intro_error_unknown;
        }
        return 0;
    }

    @Override
    protected void getChallenge(GenerateChallengeCallback callback) {
        mFaceManager = Utils.getFaceManagerOrNull(this);
        if (mFaceManager == null) {
            callback.onChallengeGenerated(0, 0, 0L);
            return;
        }
        mFaceManager.generateChallenge(mUserId, callback::onChallengeGenerated);
    }

    @Override
    protected String getExtraKeyForBiometric() {
        return ChooseLockSettingsHelper.EXTRA_KEY_FOR_FACE;
    }

    @Override
    protected Intent getEnrollingIntent() {
        Intent intent = new Intent(this, FaceEnrollEducation.class);
        WizardManagerHelper.copyWizardManagerExtras(getIntent(), intent);
        return intent;
    }

    @Override
    protected int getConfirmLockTitleResId() {
        return R.string.security_settings_face_preference_title;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FACE_ENROLL_INTRO;
    }

    @Override
    public void onClick(LinkSpan span) {
        // TODO(b/110906762)
    }

    @Override
    public @BiometricAuthenticator.Modality int getModality() {
        return BiometricAuthenticator.TYPE_FACE;
    }

    @Override
    protected void onNextButtonClick(View view) {
        final boolean parentelConsentRequired =
                getIntent()
                .getBooleanExtra(BiometricEnrollActivity.EXTRA_REQUIRE_PARENTAL_CONSENT, false);
        final boolean cameraPrivacyEnabled = SensorPrivacyManagerHelper
                .getInstance(getApplicationContext())
                .isSensorBlocked(SensorPrivacyManagerHelper.SENSOR_CAMERA);
        final boolean isSetupWizard = WizardManagerHelper.isAnySetupWizard(getIntent());
        final boolean isSettingUp = isSetupWizard || (parentelConsentRequired
                && !WizardManagerHelper.isUserSetupComplete(this));
        if (cameraPrivacyEnabled && !isSettingUp) {
            if (mSensorPrivacyManager == null) {
                mSensorPrivacyManager = getApplicationContext()
                        .getSystemService(SensorPrivacyManager.class);
            }
            mSensorPrivacyManager.showSensorUseDialog(SensorPrivacyManager.Sensors.CAMERA);
        } else {
            super.onNextButtonClick(view);
        }
    }

    @Override
    @NonNull
    protected FooterButton getPrimaryFooterButton() {
        if (mPrimaryFooterButton == null) {
            mPrimaryFooterButton = new FooterButton.Builder(this)
                    .setText(R.string.security_settings_face_enroll_introduction_agree)
                    .setButtonType(FooterButton.ButtonType.OPT_IN)
                    .setListener(this::onNextButtonClick)
                    .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                    .build();
        }
        return mPrimaryFooterButton;
    }

    @Override
    @NonNull
    protected FooterButton getSecondaryFooterButton() {
        if (mSecondaryFooterButton == null) {
            mSecondaryFooterButton = new FooterButton.Builder(this)
                    .setText(R.string.security_settings_face_enroll_introduction_no_thanks)
                    .setListener(this::onSkipButtonClick)
                    .setButtonType(FooterButton.ButtonType.NEXT)
                    .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                    .build();
        }
        return mSecondaryFooterButton;
    }

    @Override
    @StringRes
    protected int getAgreeButtonTextRes() {
        return R.string.security_settings_fingerprint_enroll_introduction_agree;
    }

    @Override
    @StringRes
    protected int getMoreButtonTextRes() {
        return R.string.security_settings_face_enroll_introduction_more;
    }

    @Override
    protected void updateDescriptionText() {
        if (mIsFaceStrong) {
            setDescriptionText(getString(
                    R.string.security_settings_face_enroll_introduction_message_class3));
        }
        super.updateDescriptionText();
    }

    @NonNull
    protected static Intent setSkipPendingEnroll(@Nullable Intent data) {
        if (data == null) {
            data = new Intent();
        }
        data.putExtra(MultiBiometricEnrollHelper.EXTRA_SKIP_PENDING_ENROLL, true);
        return data;
    }

    protected boolean isFaceStrong() {
        return mIsFaceStrong;
    }

    private void onFaceStrengthChanged() {
        // Set up and show the "less secure" info section if necessary.
        if (!mIsFaceStrong && getResources().getBoolean(
                R.bool.config_face_intro_show_less_secure)) {
            final LinearLayout infoRowLessSecure = findViewById(R.id.info_row_less_secure);
            final ImageView iconLessSecure = findViewById(R.id.icon_less_secure);
            infoRowLessSecure.setVisibility(View.VISIBLE);
            iconLessSecure.getBackground().setColorFilter(getIconColorFilter());
        }
        updateDescriptionText();
    }
}
