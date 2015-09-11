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

package com.android.settings;

import android.annotation.Nullable;
import android.app.Activity;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.os.UserHandle;

import com.android.internal.widget.LockPatternUtils;

public final class ChooseLockSettingsHelper {

    static final String EXTRA_KEY_TYPE = "type";
    static final String EXTRA_KEY_PASSWORD = "password";
    public static final String EXTRA_KEY_HAS_CHALLENGE = "has_challenge";
    public static final String EXTRA_KEY_CHALLENGE = "challenge";
    public static final String EXTRA_KEY_CHALLENGE_TOKEN = "hw_auth_token";
    public static final String EXTRA_KEY_FOR_FINGERPRINT = "for_fingerprint";


    private LockPatternUtils mLockPatternUtils;
    private Activity mActivity;
    private Fragment mFragment;

    public ChooseLockSettingsHelper(Activity activity) {
        mActivity = activity;
        mLockPatternUtils = new LockPatternUtils(activity);
    }

    public ChooseLockSettingsHelper(Activity activity, Fragment fragment) {
        this(activity);
        mFragment = fragment;
    }

    public LockPatternUtils utils() {
        return mLockPatternUtils;
    }

    /**
     * If a pattern, password or PIN exists, prompt the user before allowing them to change it.
     *
     * @param title title of the confirmation screen; shown in the action bar
     * @return true if one exists and we launched an activity to confirm it
     * @see Activity#onActivityResult(int, int, android.content.Intent)
     */
    boolean launchConfirmationActivity(int request, CharSequence title) {
        return launchConfirmationActivity(request, title, null, null, false, false);
    }

    /**
     * If a pattern, password or PIN exists, prompt the user before allowing them to change it.
     *
     * @param title title of the confirmation screen; shown in the action bar
     * @param returnCredentials if true, put credentials into intent. Note that if this is true,
     *                          this can only be called internally.
     * @return true if one exists and we launched an activity to confirm it
     * @see Activity#onActivityResult(int, int, android.content.Intent)
     */
    boolean launchConfirmationActivity(int request, CharSequence title, boolean returnCredentials) {
        return launchConfirmationActivity(request, title, null, null, returnCredentials, false);
    }

    /**
     * If a pattern, password or PIN exists, prompt the user before allowing them to change it.
     *
     * @param title title of the confirmation screen; shown in the action bar
     * @param header header of the confirmation screen; shown as large text
     * @param description description of the confirmation screen
     * @param returnCredentials if true, put credentials into intent. Note that if this is true,
     *                          this can only be called internally.
     * @param external specifies whether this activity is launched externally, meaning that it will
     *                 get a dark theme and allow fingerprint authentication
     * @return true if one exists and we launched an activity to confirm it
     * @see Activity#onActivityResult(int, int, android.content.Intent)
     */
    boolean launchConfirmationActivity(int request, @Nullable CharSequence title,
            @Nullable CharSequence header, @Nullable CharSequence description,
            boolean returnCredentials, boolean external) {
        return launchConfirmationActivity(request, title, header, description,
                returnCredentials, external, false, 0);
    }

    /**
     * If a pattern, password or PIN exists, prompt the user before allowing them to change it.
     * @param message optional message to display about the action about to be done
     * @param details optional detail message to display
     * @param challenge a challenge to be verified against the device credential.
     *                  This method can only be called internally.
     * @return true if one exists and we launched an activity to confirm it
     * @see #onActivityResult(int, int, android.content.Intent)
     */
    public boolean launchConfirmationActivity(int request, @Nullable CharSequence title,
            @Nullable CharSequence header, @Nullable CharSequence description,
            long challenge) {
        return launchConfirmationActivity(request, title, header, description,
                false, false, true, challenge);
    }

    private boolean launchConfirmationActivity(int request, @Nullable CharSequence title,
            @Nullable CharSequence header, @Nullable CharSequence description,
            boolean returnCredentials, boolean external, boolean hasChallenge,
            long challenge) {
        boolean launched = false;

        int effectiveUserId = Utils.getEffectiveUserId(mActivity);

        switch (mLockPatternUtils.getKeyguardStoredPasswordQuality(effectiveUserId)) {
            case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                launched = launchConfirmationActivity(request, title, header, description,
                        returnCredentials || hasChallenge
                                ? ConfirmLockPattern.InternalActivity.class
                                : ConfirmLockPattern.class, external,
                                hasChallenge, challenge);
                break;
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                launched = launchConfirmationActivity(request, title, header, description,
                        returnCredentials || hasChallenge
                                ? ConfirmLockPassword.InternalActivity.class
                                : ConfirmLockPassword.class, external,
                                hasChallenge, challenge);
                break;
        }
        return launched;
    }

    private boolean launchConfirmationActivity(int request, CharSequence title, CharSequence header,
            CharSequence message, Class<?> activityClass, boolean external, boolean hasChallenge,
            long challenge) {
        final Intent intent = new Intent();
        intent.putExtra(ConfirmDeviceCredentialBaseFragment.TITLE_TEXT, title);
        intent.putExtra(ConfirmDeviceCredentialBaseFragment.HEADER_TEXT, header);
        intent.putExtra(ConfirmDeviceCredentialBaseFragment.DETAILS_TEXT, message);
        intent.putExtra(ConfirmDeviceCredentialBaseFragment.ALLOW_FP_AUTHENTICATION, external);
        intent.putExtra(ConfirmDeviceCredentialBaseFragment.DARK_THEME, external);
        intent.putExtra(ConfirmDeviceCredentialBaseFragment.SHOW_CANCEL_BUTTON, external);
        intent.putExtra(ConfirmDeviceCredentialBaseFragment.SHOW_WHEN_LOCKED, external);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, hasChallenge);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, challenge);
        intent.setClassName(ConfirmDeviceCredentialBaseFragment.PACKAGE, activityClass.getName());
        if (external) {
            intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            if (mFragment != null) {
                mFragment.startActivity(intent);
            } else {
                mActivity.startActivity(intent);
            }
        } else {
            if (mFragment != null) {
                mFragment.startActivityForResult(intent, request);
            } else {
                mActivity.startActivityForResult(intent, request);
            }
        }
        return true;
    }
}
