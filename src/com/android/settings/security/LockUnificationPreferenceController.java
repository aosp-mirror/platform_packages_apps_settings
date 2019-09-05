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

import static com.android.settings.security.SecuritySettings.UNIFY_LOCK_CONFIRM_DEVICE_REQUEST;
import static com.android.settings.security.SecuritySettings.UNIFY_LOCK_CONFIRM_PROFILE_REQUEST;
import static com.android.settings.security.SecuritySettings.UNUNIFY_LOCK_CONFIRM_DEVICE_REQUEST;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * Controller for password unification/un-unification flows.
 *
 * When password is being unified, there may be two cases:
 *   1. If work password is not empty and satisfies device-wide policies (if any), it will be made
 *      into device-wide password. To do that we need both current device and profile passwords
 *      because both of them will be changed as a result.
 *   2. Otherwise device-wide password is preserved. In this case we only need current profile
 *      password, but after unifying the passwords we proceed to ask the user for a new device
 *      password.
 */
public class LockUnificationPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_UNIFICATION = "unification";

    private static final int MY_USER_ID = UserHandle.myUserId();

    private final UserManager mUm;
    private final DevicePolicyManager mDpm;
    private final LockPatternUtils mLockPatternUtils;
    private final int mProfileUserId;
    private final SecuritySettings mHost;

    private RestrictedSwitchPreference mUnifyProfile;


    private byte[] mCurrentDevicePassword;
    private byte[] mCurrentProfilePassword;
    private boolean mKeepDeviceLock;

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mUnifyProfile = screen.findPreference(KEY_UNIFICATION);
    }

    public LockUnificationPreferenceController(Context context, SecuritySettings host) {
        super(context);
        mHost = host;
        mUm = context.getSystemService(UserManager.class);
        mDpm = context.getSystemService(DevicePolicyManager.class);
        mLockPatternUtils = FeatureFactory.getFactory(context)
                .getSecurityFeatureProvider()
                .getLockPatternUtils(context);
        mProfileUserId = Utils.getManagedProfileId(mUm, MY_USER_ID);
    }

    @Override
    public boolean isAvailable() {
        return mProfileUserId != UserHandle.USER_NULL
                && mLockPatternUtils.isSeparateProfileChallengeAllowed(mProfileUserId);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_UNIFICATION;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (Utils.startQuietModeDialogIfNecessary(mContext, mUm, mProfileUserId)) {
            return false;
        }
        final boolean useOneLock = (Boolean) value;
        if (useOneLock) {
            // Keep current device (personal) lock if the profile lock is empty or is not compliant
            // with the policy on personal side.
            mKeepDeviceLock =
                    mLockPatternUtils.getKeyguardStoredPasswordQuality(mProfileUserId)
                            < DevicePolicyManager.PASSWORD_QUALITY_SOMETHING
                            || !mDpm.isProfileActivePasswordSufficientForParent(mProfileUserId);
            UnificationConfirmationDialog.newInstance(!mKeepDeviceLock).show(mHost);
        } else {
            final String title = mContext.getString(R.string.unlock_set_unlock_launch_picker_title);
            final ChooseLockSettingsHelper helper =
                    new ChooseLockSettingsHelper(mHost.getActivity(), mHost);
            if (!helper.launchConfirmationActivity(
                    UNUNIFY_LOCK_CONFIRM_DEVICE_REQUEST,
                    title, true /* returnCredentials */, MY_USER_ID)) {
                ununifyLocks();
            }
        }
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        if (mUnifyProfile != null) {
            final boolean separate =
                    mLockPatternUtils.isSeparateProfileChallengeEnabled(mProfileUserId);
            mUnifyProfile.setChecked(!separate);
            if (separate) {
                mUnifyProfile.setDisabledByAdmin(RestrictedLockUtilsInternal
                        .checkIfRestrictionEnforced(mContext, UserManager.DISALLOW_UNIFIED_PASSWORD,
                                mProfileUserId));
            }
        }
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == UNUNIFY_LOCK_CONFIRM_DEVICE_REQUEST
                && resultCode == Activity.RESULT_OK) {
            ununifyLocks();
            return true;
        } else if (requestCode == UNIFY_LOCK_CONFIRM_DEVICE_REQUEST
                && resultCode == Activity.RESULT_OK) {
            mCurrentDevicePassword =
                    data.getByteArrayExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
            launchConfirmProfileLock();
            return true;
        } else if (requestCode == UNIFY_LOCK_CONFIRM_PROFILE_REQUEST
                && resultCode == Activity.RESULT_OK) {
            mCurrentProfilePassword =
                    data.getByteArrayExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
            unifyLocks();
            return true;
        }
        return false;
    }

    private void ununifyLocks() {
        final Bundle extras = new Bundle();
        extras.putInt(Intent.EXTRA_USER_ID, mProfileUserId);
        new SubSettingLauncher(mContext)
                .setDestination(ChooseLockGeneric.ChooseLockGenericFragment.class.getName())
                    .setTitleRes(R.string.lock_settings_picker_title_profile)
                .setSourceMetricsCategory(mHost.getMetricsCategory())
                .setArguments(extras)
                .launch();
    }

    /** Asks the user to confirm device lock (if there is one) and proceeds to ask profile lock. */
    private void launchConfirmDeviceAndProfileLock() {
        final String title = mContext.getString(
                R.string.unlock_set_unlock_launch_picker_title);
        final ChooseLockSettingsHelper helper =
                new ChooseLockSettingsHelper(mHost.getActivity(), mHost);
        if (!helper.launchConfirmationActivity(
                UNIFY_LOCK_CONFIRM_DEVICE_REQUEST, title, true, MY_USER_ID)) {
            launchConfirmProfileLock();
        }
    }

    private void launchConfirmProfileLock() {
        final String title = mContext.getString(
                R.string.unlock_set_unlock_launch_picker_title_profile);
        final ChooseLockSettingsHelper helper =
                new ChooseLockSettingsHelper(mHost.getActivity(), mHost);
        if (!helper.launchConfirmationActivity(
                UNIFY_LOCK_CONFIRM_PROFILE_REQUEST, title, true, mProfileUserId)) {
            unifyLocks();
            // TODO: update relevant prefs.
            // createPreferenceHierarchy();
        }
    }

    void startUnification() {
        // If the device lock stays the same, only confirm profile lock. Otherwise confirm both.
        if (mKeepDeviceLock) {
            launchConfirmProfileLock();
        } else {
            launchConfirmDeviceAndProfileLock();
        }
    }

    private void unifyLocks() {
        if (mKeepDeviceLock) {
            unifyKeepingDeviceLock();
            promptForNewDeviceLock();
        } else {
            unifyKeepingWorkLock();
        }
        mCurrentDevicePassword = null;
        mCurrentProfilePassword = null;
    }

    private void unifyKeepingWorkLock() {
        final int profileQuality =
                mLockPatternUtils.getKeyguardStoredPasswordQuality(mProfileUserId);
        // PASSWORD_QUALITY_SOMETHING means pattern, everything above means PIN/password.
        if (profileQuality == DevicePolicyManager.PASSWORD_QUALITY_SOMETHING) {
            mLockPatternUtils.saveLockPattern(
                    LockPatternUtils.byteArrayToPattern(mCurrentProfilePassword),
                    mCurrentDevicePassword, MY_USER_ID);
        } else {
            mLockPatternUtils.saveLockPassword(
                    mCurrentProfilePassword, mCurrentDevicePassword, profileQuality, MY_USER_ID);
        }
        mLockPatternUtils.setSeparateProfileChallengeEnabled(mProfileUserId, false,
                mCurrentProfilePassword);
        final boolean profilePatternVisibility =
                mLockPatternUtils.isVisiblePatternEnabled(mProfileUserId);
        mLockPatternUtils.setVisiblePatternEnabled(profilePatternVisibility, MY_USER_ID);
    }

    private void unifyKeepingDeviceLock() {
        mLockPatternUtils.setSeparateProfileChallengeEnabled(mProfileUserId, false,
                mCurrentProfilePassword);
    }

    private void promptForNewDeviceLock() {
        new SubSettingLauncher(mContext)
                .setDestination(ChooseLockGeneric.ChooseLockGenericFragment.class.getName())
                .setTitleRes(R.string.lock_settings_picker_title)
                .setSourceMetricsCategory(mHost.getMetricsCategory())
                .launch();
    }

}
