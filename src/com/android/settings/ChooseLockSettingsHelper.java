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
import android.content.Intent;

import com.android.internal.widget.LockPatternUtils;

public class ChooseLockSettingsHelper {
    private LockPatternUtils mLockPatternUtils;
    private Activity mActivity;

    public ChooseLockSettingsHelper(Activity activity) {
        mActivity = activity;
        mLockPatternUtils = new LockPatternUtils(activity.getContentResolver());
    }

    public LockPatternUtils utils() {
        return mLockPatternUtils;
    }

    /**
     * If a pattern, password or PIN exists, prompt the user before allowing them to change it.
     * @return true if one exists and we launched an activity to confirm it
     * @see #onActivityResult(int, int, android.content.Intent)
     */
    protected boolean launchConfirmationActivity(int request) {
        boolean launched = false;
        switch (mLockPatternUtils.getPasswordMode()) {
            case LockPatternUtils.MODE_PATTERN:
                launched = confirmPattern(request);
                break;
            case LockPatternUtils.MODE_PIN:
            case LockPatternUtils.MODE_PASSWORD:
                launched = confirmPassword(request);
                break;
        }
        return launched;
    }

    /**
     * Launch screen to confirm the existing lock pattern.
     * @see #onActivityResult(int, int, android.content.Intent)
     * @return true if we launched an activity to confirm pattern
     */
    private boolean confirmPattern(int request) {
        if (!mLockPatternUtils.isLockPatternEnabled() || !mLockPatternUtils.savedPatternExists()) {
            return false;
        }
        final Intent intent = new Intent();
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
