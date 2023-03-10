/*
 * Copyright (C) 2010 The Android Open Source Project
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

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.StartLockscreenValidationRequest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentSender;
import android.os.UserManager;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.SetupWizardUtils;
import com.android.settings.Utils;
import com.android.settings.core.SettingsBaseActivity;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.transition.SettingsTransitionHelper;

import com.google.android.setupcompat.util.WizardManagerHelper;

import java.util.Optional;

public final class ChooseLockSettingsHelper {

    private static final String TAG = "ChooseLockSettingsHelper";

    public static final String EXTRA_KEY_PASSWORD = "password";
    public static final String EXTRA_KEY_RETURN_CREDENTIALS = "return_credentials";
    // Force the verifyCredential path instead of checkCredential path. This will be removed
    // after b/161956762 is resolved.
    public static final String EXTRA_KEY_FORCE_VERIFY = "force_verify";
    // Gatekeeper HardwareAuthToken
    public static final String EXTRA_KEY_CHALLENGE_TOKEN = "hw_auth_token";
    // For the fingerprint-only path
    public static final String EXTRA_KEY_FOR_FINGERPRINT = "for_fingerprint";
    // For the face-only path
    public static final String EXTRA_KEY_FOR_FACE = "for_face";
    // For the paths where multiple biometric sensors exist
    public static final String EXTRA_KEY_FOR_BIOMETRICS = "for_biometrics";
    public static final String EXTRA_KEY_FOREGROUND_ONLY = "foreground_only";
    public static final String EXTRA_KEY_REQUEST_GK_PW_HANDLE = "request_gk_pw_handle";
    // Gatekeeper password handle, which can subsequently be used to generate Gatekeeper
    // HardwareAuthToken(s) via LockSettingsService#verifyGatekeeperPasswordHandle
    public static final String EXTRA_KEY_GK_PW_HANDLE = "gk_pw_handle";

    /**
     * When EXTRA_KEY_UNIFICATION_PROFILE_CREDENTIAL and EXTRA_KEY_UNIFICATION_PROFILE_ID are
     * provided to ChooseLockGeneric as fragment arguments {@link SubSettingLauncher#setArguments},
     * at the end of the password change flow, the supplied profile user
     * (EXTRA_KEY_UNIFICATION_PROFILE_ID) will be unified to its parent. The current profile
     * password is supplied by EXTRA_KEY_UNIFICATION_PROFILE_CREDENTIAL.
     */
    public static final String EXTRA_KEY_UNIFICATION_PROFILE_ID = "unification_profile_id";
    public static final String EXTRA_KEY_UNIFICATION_PROFILE_CREDENTIAL =
            "unification_profile_credential";

    /**
     * Intent extra for passing the requested min password complexity to later steps in the set new
     * screen lock flow.
     */
    public static final String EXTRA_KEY_REQUESTED_MIN_COMPLEXITY = "requested_min_complexity";

    /**
     * Intent extra for passing the label of the calling app to later steps in the set new screen
     * lock flow.
     */
    public static final String EXTRA_KEY_CALLER_APP_NAME = "caller_app_name";

    /**
     * Intent extra indicating that the calling app is an admin, such as a Device Adimn, Device
     * Owner, or Profile Owner.
     */
    public static final String EXTRA_KEY_IS_CALLING_APP_ADMIN = "is_calling_app_admin";

    /**
     * When invoked via {@link ConfirmLockPassword.InternalActivity}, this flag
     * controls if we relax the enforcement of
     * {@link Utils#enforceSameOwner(android.content.Context, int)}.
     */
    public static final String EXTRA_KEY_ALLOW_ANY_USER = "allow_any_user";

    /**
     *
     */
    public static final String EXTRA_KEY_DEVICE_PASSWORD_REQUIREMENT_ONLY =
            "device_password_requirement_only";

    @VisibleForTesting @NonNull LockPatternUtils mLockPatternUtils;
    @NonNull private final Activity mActivity;
    @Nullable private final Fragment mFragment;
    @Nullable private final ActivityResultLauncher mActivityResultLauncher;
    @NonNull private final Builder mBuilder;

    private ChooseLockSettingsHelper(@NonNull Builder builder, @NonNull Activity activity,
            @Nullable Fragment fragment,
            @Nullable ActivityResultLauncher activityResultLauncher) {
        mBuilder = builder;
        mActivity = activity;
        mFragment = fragment;
        mActivityResultLauncher = activityResultLauncher;
        mLockPatternUtils = new LockPatternUtils(activity);
    }

    public static class Builder {
        @NonNull private final Activity mActivity;
        @Nullable private Fragment mFragment;
        @Nullable private ActivityResultLauncher mActivityResultLauncher;

        private int mRequestCode;
        @Nullable private CharSequence mTitle;
        @Nullable private CharSequence mHeader;
        @Nullable private CharSequence mDescription;
        @Nullable private CharSequence mAlternateButton;
        @Nullable private CharSequence mCheckBoxLabel;
        private boolean mReturnCredentials;
        private boolean mExternal;
        private boolean mForegroundOnly;
        // ChooseLockSettingsHelper will determine the caller's userId if none provided.
        private int mUserId;
        private boolean mAllowAnyUserId;
        private boolean mForceVerifyPath;
        private boolean mRemoteLockscreenValidation;
        @Nullable private StartLockscreenValidationRequest mStartLockscreenValidationRequest;
        @Nullable private ComponentName mRemoteLockscreenValidationServiceComponent;
        boolean mRequestGatekeeperPasswordHandle;

        public Builder(@NonNull Activity activity) {
            mActivity = activity;
            mUserId = Utils.getCredentialOwnerUserId(mActivity);
        }

        public Builder(@NonNull Activity activity, @NonNull Fragment fragment) {
            this(activity);
            mFragment = fragment;
        }

        /**
         * @param requestCode for onActivityResult
         */
        @NonNull public Builder setRequestCode(int requestCode) {
            mRequestCode = requestCode;
            return this;
        }

        /**
         * @param title of the confirmation screen; shown in the action bar
         */
        @NonNull public Builder setTitle(@Nullable CharSequence title) {
            mTitle = title;
            return this;
        }

        /**
         * @param header of the confirmation screen; shown as large text
         */
        @NonNull public Builder setHeader(@Nullable CharSequence header) {
            mHeader = header;
            return this;
        }

        /**
         * @param description of the confirmation screen
         */
        @NonNull public Builder setDescription(@Nullable CharSequence description) {
            mDescription = description;
            return this;
        }

        /**
         * @param alternateButton text for an alternate button
         */
        @NonNull public Builder setAlternateButton(@Nullable CharSequence alternateButton) {
            mAlternateButton = alternateButton;
            return this;
        }

        /**
         * @param checkboxLabel text for the checkbox
         */
        @NonNull
        public Builder setCheckboxLabel(@Nullable CharSequence checkboxLabel) {
            mCheckBoxLabel = checkboxLabel;
            return this;
        }

        /**
         * @param returnCredentials if true, puts the following credentials into intent for
         *                          onActivityResult with the following keys:
         *                          {@link #EXTRA_KEY_PASSWORD},
         *                          {@link #EXTRA_KEY_CHALLENGE_TOKEN},
         *                          {@link #EXTRA_KEY_GK_PW_HANDLE}
         *                          Note that if this is true, this can only be called internally.
         *
         *                          This should also generally be set if
         *                          {@link #setRequestGatekeeperPasswordHandle(boolean)} is set.
         */
        @NonNull public Builder setReturnCredentials(boolean returnCredentials) {
            mReturnCredentials = returnCredentials;
            return this;
        }

        /**
         * @param userId for whom the credential should be confirmed.
         */
        @NonNull public Builder setUserId(int userId) {
            mUserId = userId;
            return this;
        }

        /**
         * @param allowAnyUserId Allows the caller to prompt for credentials of any user, including
         *                       those which aren't associated with the current user. As an example,
         *                       this is useful when unlocking the storage for secondary users.
         */
        @NonNull public Builder setAllowAnyUserId(boolean allowAnyUserId) {
            mAllowAnyUserId = allowAnyUserId;
            return this;
        }

        /**
         * @param external specifies whether this activity is launched externally, meaning that it
         *                 will get a dark theme, allow biometric authentication, and it will
         *                 forward the activity result.
         */
        @NonNull public Builder setExternal(boolean external) {
            mExternal = external;
            return this;
        }

        /**
         * @param foregroundOnly if true, the confirmation activity will be finished if it loses
         *                       foreground.
         */
        @NonNull public Builder setForegroundOnly(boolean foregroundOnly) {
            mForegroundOnly = foregroundOnly;
            return this;
        }

        /**
         * @param forceVerifyPath Forces the VerifyCredential path instead of the CheckCredential
         *                        path. This will be removed after b/161956762 is resolved.
         */
        @NonNull public Builder setForceVerifyPath(boolean forceVerifyPath) {
            mForceVerifyPath = forceVerifyPath;
            return this;
        }

        /**
         * @param isRemoteLockscreenValidation if true, remote device validation flow will be
         *                                 started. {@link #setStartLockscreenValidationRequest} and
         *                                 {@link #setRemoteLockscreenValidationServiceComponent}
         *                                 must also be used to set the required data.
         */
        @NonNull public Builder setRemoteLockscreenValidation(
                boolean isRemoteLockscreenValidation) {
            mRemoteLockscreenValidation = isRemoteLockscreenValidation;
            return this;
        }

        /**
         * @param startLockScreenValidationRequest contains information necessary to perform remote
         *                                         lockscreen validation such as the remote device's
         *                                         lockscreen type, public key to be used for
         *                                         encryption, and remaining attempts.
         */
        @NonNull public Builder setStartLockscreenValidationRequest(
                StartLockscreenValidationRequest startLockScreenValidationRequest) {
            mStartLockscreenValidationRequest = startLockScreenValidationRequest;
            return this;
        }

        /**
         * @param remoteLockscreenValidationServiceComponent the {@link ComponentName} of the
         * {@link android.service.remotelockscreenvalidation.RemoteLockscreenValidationService}
         * that will be used to validate the lockscreen guess.
         */
        @NonNull public Builder setRemoteLockscreenValidationServiceComponent(
                ComponentName remoteLockscreenValidationServiceComponent) {
            mRemoteLockscreenValidationServiceComponent =
                    remoteLockscreenValidationServiceComponent;
            return this;
        }

        /**
         * Requests that LockSettingsService return a handle to the Gatekeeper Password (instead of
         * the Gatekeeper HAT). This allows us to use a single entry of the user's credential
         * to create multiple Gatekeeper HATs containing distinct challenges via
         * {@link LockPatternUtils#verifyGatekeeperPasswordHandle(long, long, int)}.
         *
         * Upon confirmation of the user's password, the Gatekeeper Password Handle will be returned
         * via onActivityResult with the key being {@link #EXTRA_KEY_GK_PW_HANDLE}.
         * @param requestGatekeeperPasswordHandle
         */
        @NonNull public Builder setRequestGatekeeperPasswordHandle(
                boolean requestGatekeeperPasswordHandle) {
            mRequestGatekeeperPasswordHandle = requestGatekeeperPasswordHandle;
            return this;
        }

        /**
         * Support of ActivityResultLauncher.
         *
         * Which allowing the launch operation be controlled externally.
         * @param activityResultLauncher a launcher previously prepared.
         */
        @NonNull public Builder setActivityResultLauncher(
                ActivityResultLauncher activityResultLauncher) {
            mActivityResultLauncher = activityResultLauncher;
            return this;
        }

        @NonNull public ChooseLockSettingsHelper build() {
            if (!mAllowAnyUserId && mUserId != LockPatternUtils.USER_FRP) {
                Utils.enforceSameOwner(mActivity, mUserId);
            }

            if (mExternal && mReturnCredentials) {
                throw new IllegalArgumentException("External and ReturnCredentials specified. "
                        + " External callers should never be allowed to receive credentials in"
                        + " onActivityResult");
            }

            if (mRequestGatekeeperPasswordHandle && !mReturnCredentials) {
                // HAT containing the signed challenge will not be available to the caller.
                Log.w(TAG, "Requested gatekeeper password handle but not requesting"
                        + " ReturnCredentials. Are you sure this is what you want?");
            }

            return new ChooseLockSettingsHelper(this, mActivity, mFragment,
                    mActivityResultLauncher);
        }

        public boolean show() {
            return build().launch();
        }
    }

    /**
     * If a PIN, Pattern, or Password exists, prompt the user to confirm it.
     * @return true if the confirmation activity is shown (e.g. user has a credential set up)
     */
    public boolean launch() {
        return launchConfirmationActivity(mBuilder.mRequestCode, mBuilder.mTitle, mBuilder.mHeader,
                mBuilder.mDescription, mBuilder.mReturnCredentials, mBuilder.mExternal,
                mBuilder.mForceVerifyPath, mBuilder.mUserId, mBuilder.mAlternateButton,
                mBuilder.mCheckBoxLabel, mBuilder.mRemoteLockscreenValidation,
                mBuilder.mStartLockscreenValidationRequest,
                mBuilder.mRemoteLockscreenValidationServiceComponent, mBuilder.mAllowAnyUserId,
                mBuilder.mForegroundOnly, mBuilder.mRequestGatekeeperPasswordHandle);
    }

    private boolean launchConfirmationActivity(int request, @Nullable CharSequence title,
            @Nullable CharSequence header, @Nullable CharSequence description,
            boolean returnCredentials, boolean external, boolean forceVerifyPath,
            int userId, @Nullable CharSequence alternateButton,
            @Nullable CharSequence checkboxLabel, boolean remoteLockscreenValidation,
            @Nullable StartLockscreenValidationRequest startLockScreenValidationRequest,
            @Nullable ComponentName remoteLockscreenValidationServiceComponent,
            boolean allowAnyUser, boolean foregroundOnly, boolean requestGatekeeperPasswordHandle) {
        Optional<Class<?>> activityClass = determineAppropriateActivityClass(
                returnCredentials, forceVerifyPath, userId, startLockScreenValidationRequest);
        if (activityClass.isEmpty()) {
            return false;
        }

        return launchConfirmationActivity(request, title, header, description, activityClass.get(),
                returnCredentials, external, forceVerifyPath, userId, alternateButton,
                checkboxLabel, remoteLockscreenValidation, startLockScreenValidationRequest,
                remoteLockscreenValidationServiceComponent, allowAnyUser, foregroundOnly,
                requestGatekeeperPasswordHandle);
    }

    private boolean launchConfirmationActivity(int request, CharSequence title, CharSequence header,
            CharSequence message, Class<?> activityClass, boolean returnCredentials,
            boolean external, boolean forceVerifyPath, int userId,
            @Nullable CharSequence alternateButton, @Nullable CharSequence checkbox,
            boolean remoteLockscreenValidation,
            @Nullable StartLockscreenValidationRequest startLockScreenValidationRequest,
            @Nullable ComponentName remoteLockscreenValidationServiceComponent,
            boolean allowAnyUser, boolean foregroundOnly, boolean requestGatekeeperPasswordHandle) {
        final Intent intent = new Intent();
        intent.putExtra(ConfirmDeviceCredentialBaseFragment.TITLE_TEXT, title);
        intent.putExtra(ConfirmDeviceCredentialBaseFragment.HEADER_TEXT, header);
        intent.putExtra(ConfirmDeviceCredentialBaseFragment.DETAILS_TEXT, message);
        // TODO: Remove dark theme and show_cancel_button options since they are no longer used
        intent.putExtra(ConfirmDeviceCredentialBaseFragment.DARK_THEME, false);
        intent.putExtra(ConfirmDeviceCredentialBaseFragment.SHOW_CANCEL_BUTTON, false);
        intent.putExtra(ConfirmDeviceCredentialBaseFragment.SHOW_WHEN_LOCKED, external);
        intent.putExtra(ConfirmDeviceCredentialBaseFragment.USE_FADE_ANIMATION, external);
        intent.putExtra(ConfirmDeviceCredentialBaseFragment.IS_REMOTE_LOCKSCREEN_VALIDATION,
                remoteLockscreenValidation);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_RETURN_CREDENTIALS, returnCredentials);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FORCE_VERIFY, forceVerifyPath);
        intent.putExtra(Intent.EXTRA_USER_ID, userId);
        intent.putExtra(KeyguardManager.EXTRA_ALTERNATE_BUTTON_LABEL, alternateButton);
        intent.putExtra(KeyguardManager.EXTRA_CHECKBOX_LABEL, checkbox);
        intent.putExtra(KeyguardManager.EXTRA_START_LOCKSCREEN_VALIDATION_REQUEST,
                startLockScreenValidationRequest);
        intent.putExtra(Intent.EXTRA_COMPONENT_NAME, remoteLockscreenValidationServiceComponent);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOREGROUND_ONLY, foregroundOnly);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_ALLOW_ANY_USER, allowAnyUser);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_REQUEST_GK_PW_HANDLE,
                requestGatekeeperPasswordHandle);

        intent.setClassName(SETTINGS_PACKAGE_NAME, activityClass.getName());
        intent.putExtra(SettingsBaseActivity.EXTRA_PAGE_TRANSITION_TYPE,
                SettingsTransitionHelper.TransitionType.TRANSITION_SLIDE);

        Intent inIntent = mFragment != null ? mFragment.getActivity().getIntent() :
                mActivity.getIntent();
        copyInternalExtras(inIntent, intent);
        if (external) {
            intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            copyOptionalExtras(inIntent, intent);
            if (mActivityResultLauncher != null) {
                mActivityResultLauncher.launch(intent);
            } else if (mFragment != null) {
                mFragment.startActivity(intent);
            } else {
                mActivity.startActivity(intent);
            }
        } else {
            if (mActivityResultLauncher != null) {
                mActivityResultLauncher.launch(intent);
            } else if (mFragment != null) {
                mFragment.startActivityForResult(intent, request);
            } else {
                mActivity.startActivityForResult(intent, request);
            }
        }
        return true;
    }

    private Optional<Integer> passwordQualityToLockTypes(int quality) {
        switch (quality) {
            case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                return Optional.of(KeyguardManager.PATTERN);
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                return Optional.of(KeyguardManager.PIN);
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
            case DevicePolicyManager.PASSWORD_QUALITY_MANAGED:
                return Optional.of(KeyguardManager.PASSWORD);
        }
        Log.e(TAG, String.format(
                "Cannot determine appropriate activity class for password quality %d",
                quality));
        return Optional.empty();
    }

    private Optional<Class<?>> determineAppropriateActivityClass(boolean returnCredentials,
            boolean forceVerifyPath, int userId,
            @Nullable StartLockscreenValidationRequest startLockscreenValidationRequest) {
        int lockType;
        if (startLockscreenValidationRequest != null) {
            lockType = startLockscreenValidationRequest.getLockscreenUiType();
        } else {
            final int effectiveUserId = UserManager
                    .get(mActivity).getCredentialOwnerProfile(userId);
            Optional<Integer> lockTypeOptional = passwordQualityToLockTypes(
                    mLockPatternUtils.getKeyguardStoredPasswordQuality(effectiveUserId));
            if (lockTypeOptional.isEmpty()) {
                return Optional.empty();
            }
            lockType = lockTypeOptional.get();
        }

        switch (lockType) {
            case KeyguardManager.PASSWORD:
            case KeyguardManager.PIN:
                return Optional.of(returnCredentials || forceVerifyPath
                        ? ConfirmLockPassword.InternalActivity.class
                        : ConfirmLockPassword.class);
            case KeyguardManager.PATTERN:
                return Optional.of(returnCredentials || forceVerifyPath
                        ? ConfirmLockPattern.InternalActivity.class
                        : ConfirmLockPattern.class);
        }
        Log.e(TAG, String.format("Cannot determine appropriate activity class for lock type %d",
                lockType));
        return Optional.empty();
    }

    private void copyOptionalExtras(Intent inIntent, Intent outIntent) {
        IntentSender intentSender = inIntent.getParcelableExtra(Intent.EXTRA_INTENT);
        if (intentSender != null) {
            outIntent.putExtra(Intent.EXTRA_INTENT, intentSender);
        }
        int taskId = inIntent.getIntExtra(Intent.EXTRA_TASK_ID, -1);
        if (taskId != -1) {
            outIntent.putExtra(Intent.EXTRA_TASK_ID, taskId);
        }
        // If we will launch another activity once credentials are confirmed, exclude from recents.
        // This is a workaround to a framework bug where affinity is incorrect for activities
        // that are started from a no display activity, as is ConfirmDeviceCredentialActivity.
        // TODO: Remove once that bug is fixed.
        if (intentSender != null || taskId != -1) {
            outIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            outIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        }
    }

    private void copyInternalExtras(Intent inIntent, Intent outIntent) {
        SetupWizardUtils.copySetupExtras(inIntent, outIntent);
        String theme = inIntent.getStringExtra(WizardManagerHelper.EXTRA_THEME);
        if (theme != null) {
            outIntent.putExtra(WizardManagerHelper.EXTRA_THEME, theme);
        }
    }
}
