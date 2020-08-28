/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.Context;
import android.os.UserHandle;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.users.OwnerInfoSettings;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.ObservablePreferenceFragment;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class OwnerInfoPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnResume {

    @VisibleForTesting
    static final String KEY_OWNER_INFO = "owner_info_settings";
    private static final int MY_USER_ID = UserHandle.myUserId();

    private final LockPatternUtils mLockPatternUtils;
    private final ObservablePreferenceFragment mParent;
    private RestrictedPreference mOwnerInfoPref;

    // Container fragment should implement this in order to show the correct summary
    public interface OwnerInfoCallback {
        void onOwnerInfoUpdated();
    }

    public OwnerInfoPreferenceController(Context context, ObservablePreferenceFragment parent) {
        super(context);
        mParent = parent;
        mLockPatternUtils = new LockPatternUtils(context);
        if (parent != null) {
            parent.getSettingsLifecycle().addObserver(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mOwnerInfoPref  = screen.findPreference(KEY_OWNER_INFO);
    }

    @Override
    public void onResume() {
        updateEnableState();
        updateSummary();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_OWNER_INFO;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(getPreferenceKey(), preference.getKey())) {
            OwnerInfoSettings.show(mParent);
            return true;
        }
        return false;
    }

    public void updateEnableState() {
        if (mOwnerInfoPref == null) {
            return;
        }
        if (isDeviceOwnerInfoEnabled()) {
            EnforcedAdmin admin = getDeviceOwner();
            mOwnerInfoPref.setDisabledByAdmin(admin);
        } else {
            mOwnerInfoPref.setDisabledByAdmin(null);
            mOwnerInfoPref.setEnabled(!mLockPatternUtils.isLockScreenDisabled(MY_USER_ID));
        }
    }

    public void updateSummary() {
        if (mOwnerInfoPref != null) {
            if (isDeviceOwnerInfoEnabled()) {
                mOwnerInfoPref.setSummary(
                    getDeviceOwnerInfo());
            } else {
                mOwnerInfoPref.setSummary(isOwnerInfoEnabled()
                    ? getOwnerInfo()
                    : mContext.getString(
                        com.android.settings.R.string.owner_info_settings_summary));
            }
        }
    }

    // Wrapper methods to allow testing
    @VisibleForTesting
    boolean isDeviceOwnerInfoEnabled() {
        return mLockPatternUtils.isDeviceOwnerInfoEnabled();
    }

    @VisibleForTesting
    String getDeviceOwnerInfo() {
        return mLockPatternUtils.getDeviceOwnerInfo();
    }

    @VisibleForTesting
    boolean isOwnerInfoEnabled() {
        return mLockPatternUtils.isOwnerInfoEnabled(MY_USER_ID);
    }

    @VisibleForTesting
    String getOwnerInfo() {
        return mLockPatternUtils.getOwnerInfo(MY_USER_ID);
    }

    @VisibleForTesting
    EnforcedAdmin getDeviceOwner() {
        return RestrictedLockUtilsInternal.getDeviceOwner(mContext);
    }
}
