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

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;

import com.android.internal.widget.LockPatternUtils;

public class ChooseLockSettingsHelper {
    private LockPatternUtils mLockPatternUtils;
    private Activity mActivity;

    public ChooseLockSettingsHelper(Activity activity) {
        mActivity = activity;
        mLockPatternUtils = new LockPatternUtils(activity);
    }

    public LockPatternUtils utils() {
        return mLockPatternUtils;
    }

    /**
     * If a pattern, password or PIN exists, prompt the user before allowing them to change it.
     * @param message optional message to display about the action about to be done
     * @param details optional detail message to display
     * @return true if one exists and we launched an activity to confirm it
     * @see #onActivityResult(int, int, android.content.Intent)
     */
    protected boolean launchConfirmationActivity(int request,
            CharSequence message, CharSequence details) {
        boolean launched = false;
        switch (mLockPatternUtils.getKeyguardStoredPasswordQuality()) {
            case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                launched = confirmPattern(request, message, details);
                break;
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                // TODO: update UI layout for ConfirmPassword to show message and details
                launched = confirmPassword(request);
                break;
        }
        return launched;
    }

    /**
     * Launch screen to confirm the existing lock pattern.
     * @param message shown in header of ConfirmLockPattern if not null
     * @param details shown in footer of ConfirmLockPattern if not null
     * @see #onActivityResult(int, int, android.content.Intent)
     * @return true if we launched an activity to confirm pattern
     */
    private boolean confirmPattern(int request, CharSequence message, CharSequence details) {
        if (!mLockPatternUtils.isLockPatternEnabled() || !mLockPatternUtils.savedPatternExists()) {
            return false;
        }
        final Intent intent = new Intent();
        // supply header and footer text in the intent
        intent.putExtra(ConfirmLockPattern.HEADER_TEXT, message);
        intent.putExtra(ConfirmLockPattern.FOOTER_TEXT, details);
        intent.setClassName("com.android.settings", "com.android.settings.ConfirmLockPattern");
        mActivity.startActivityForResult(intent, request);
        return true;
    }

    /**
     * Launch screen to confirm the existing lock password.
     * @see #onActivityResult(int, int, android.content.Intent)
     * @return true if we launched an activity to confirm password
     */
    private boolean confirmPassword(int request) {
        if (!mLockPatternUtils.isLockPasswordEnabled()) return false;
        final Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.ConfirmLockPassword");
        mActivity.startActivityForResult(intent, request);
        return true;
    }


}
