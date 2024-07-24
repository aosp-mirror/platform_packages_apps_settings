/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.privatespace.onelock;

import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PASSWORD;
import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PATTERN;
import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PIN;

import android.content.Context;
import android.os.UserHandle;
import android.util.Log;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.privatespace.PrivateSpaceMaintainer;

/** Represents the preference controller for using the same lock as the screen lock */
public class UseOneLockController extends BasePreferenceController {
    private static final String TAG = "UseOneLockController";
    private final LockPatternUtils mLockPatternUtils;
    private final PrivateSpaceMaintainer mPrivateSpaceMaintainer;

    public UseOneLockController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mPrivateSpaceMaintainer = PrivateSpaceMaintainer.getInstance(mContext);
        mLockPatternUtils = FeatureFactory.getFeatureFactory()
                .getSecurityFeatureProvider()
                .getLockPatternUtils(context);
    }
    @Override
    public int getAvailabilityStatus() {
        return android.os.Flags.allowPrivateProfile() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }

    @Override
    public CharSequence getSummary() {
        UserHandle privateProfileHandle = mPrivateSpaceMaintainer.getPrivateProfileHandle();
        if (privateProfileHandle != null) {
            int privateUserId = privateProfileHandle.getIdentifier();
            if (mLockPatternUtils.isSeparateProfileChallengeEnabled(privateUserId)) {
                return mContext.getString(getCredentialTypeResId(privateUserId));
            }
        } else {
            Log.w(TAG, "Did not find Private Space.");
        }
        return mContext.getString(R.string.private_space_screen_lock_summary);
    }

    private int getCredentialTypeResId(int userId) {
        int credentialType = mLockPatternUtils.getCredentialTypeForUser(userId);
        switch (credentialType) {
            case CREDENTIAL_TYPE_PATTERN:
                return R.string.unlock_set_unlock_mode_pattern;
            case CREDENTIAL_TYPE_PIN:
                return R.string.unlock_set_unlock_mode_pin;
            case CREDENTIAL_TYPE_PASSWORD:
                return R.string.unlock_set_unlock_mode_password;
            default:
                // This is returned for CREDENTIAL_TYPE_NONE
                return R.string.unlock_set_unlock_mode_off;
        }
    }
}
