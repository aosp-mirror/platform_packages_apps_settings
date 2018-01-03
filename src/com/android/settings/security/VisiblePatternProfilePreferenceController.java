/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.security;

import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.Utils;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;

public class VisiblePatternProfilePreferenceController extends TogglePreferenceController {

    private static final String KEY_VISIBLE_PATTERN_PROFILE = "visiblepattern_profile";

    private final LockPatternUtils mLockPatternUtils;
    private final UserManager mUm;
    private final int mUserId = UserHandle.myUserId();
    private final int mProfileChallengeUserId;

    public VisiblePatternProfilePreferenceController(Context context) {
        super(context, KEY_VISIBLE_PATTERN_PROFILE);
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mLockPatternUtils = FeatureFactory.getFactory(context)
                .getSecurityFeatureProvider()
                .getLockPatternUtils(context);
        mProfileChallengeUserId = Utils.getManagedProfileId(mUm, mUserId);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mLockPatternUtils.isSecure(mProfileChallengeUserId)
                && mLockPatternUtils.getKeyguardStoredPasswordQuality(mProfileChallengeUserId)
                == PASSWORD_QUALITY_SOMETHING) {
            return AVAILABLE;
        }
        return DISABLED_FOR_USER;
    }

    @Override
    public boolean isChecked() {
        return mLockPatternUtils.isVisiblePatternEnabled(
                mProfileChallengeUserId);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (Utils.startQuietModeDialogIfNecessary(mContext, mUm, mProfileChallengeUserId)) {
            return false;
        }
        mLockPatternUtils.setVisiblePatternEnabled(isChecked, mProfileChallengeUserId);
        return true;
    }
}
