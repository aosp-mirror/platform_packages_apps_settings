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
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.Utils;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class VisiblePatternProfilePreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnResume {

    private static final String KEY_VISIBLE_PATTERN_PROFILE = "visiblepattern_profile";

    private final LockPatternUtils mLockPatternUtils;
    private final UserManager mUm;
    private final int mUserId = UserHandle.myUserId();
    private final int mProfileChallengeUserId;

    private Preference mPreference;

    public VisiblePatternProfilePreferenceController(Context context, Lifecycle lifecycle) {
        super(context, KEY_VISIBLE_PATTERN_PROFILE);
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mLockPatternUtils = FeatureFactory.getFactory(context)
                .getSecurityFeatureProvider()
                .getLockPatternUtils(context);
        mProfileChallengeUserId = Utils.getManagedProfileId(mUm, mUserId);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
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

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onResume() {
        mPreference.setVisible(isAvailable());
    }
}
