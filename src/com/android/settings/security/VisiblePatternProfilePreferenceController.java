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
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class VisiblePatternProfilePreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnResume {

    private static final String KEY_VISIBLE_PATTERN_PROFILE = "visiblepattern_profile";
    private static final String TAG = "VisPtnProfPrefCtrl";

    private final LockPatternUtils mLockPatternUtils;
    private final UserManager mUm;
    private final int mUserId = UserHandle.myUserId();
    private final int mProfileChallengeUserId;

    private Preference mPreference;

    public VisiblePatternProfilePreferenceController(Context context) {
        this(context, null /* lifecycle */);
    }

    public VisiblePatternProfilePreferenceController(Context context, Lifecycle lifecycle) {
        this(context, lifecycle, KEY_VISIBLE_PATTERN_PROFILE);
    }


    // TODO (b/73074893) Replace this constructor without Lifecycle using setter method instead.
    public VisiblePatternProfilePreferenceController(
            Context context, Lifecycle lifecycle, String key) {
        super(context, key);
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mLockPatternUtils = FeatureFactory.getFeatureFactory()
                .getSecurityFeatureProvider()
                .getLockPatternUtils(context);
        mProfileChallengeUserId = Utils.getManagedProfileId(mUm, mUserId);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        final FutureTask<Integer> futureTask = new FutureTask<>(
                // Put the API call in a future to avoid StrictMode violation.
                () -> {
                    final boolean isSecure = mLockPatternUtils.isSecure(mProfileChallengeUserId);
                    final boolean hasPassword = mLockPatternUtils
                            .getKeyguardStoredPasswordQuality(mProfileChallengeUserId)
                            == PASSWORD_QUALITY_SOMETHING;
                    if (isSecure && hasPassword) {
                        return AVAILABLE;
                    }
                    return DISABLED_FOR_USER;
                });
        try {
            futureTask.run();
            return futureTask.get();
        } catch (InterruptedException | ExecutionException e) {
            Log.w(TAG, "Error getting lock pattern state.");
            return DISABLED_FOR_USER;
        }
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
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_security;
    }

    @Override
    public void onResume() {
        if (mPreference != null) {
            mPreference.setVisible(isAvailable());
        }
    }
}
