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

import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_SET_UNLOCK_LAUNCH_PICKER_TITLE;

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
import com.android.internal.widget.LockscreenCredential;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.transition.SettingsTransitionHelper;

/**
 * Controller for password unification/un-unification flows.
 *
 * When password is being unified, there may be two cases:
 *   1. If device password will satisfy device-wide policies post-unification (when password policy
 *      set on the work challenge will be enforced on device password), the device password is
 *      preserved while work challenge is unified. Only the current work challenge is required
 *      in this flow.
 *   2. Otherwise the user will need to enroll a new compliant device password before unification
 *      takes place. In this case we first confirm the current work challenge, then guide the user
 *      through an enrollment flow for the new device password, and finally unify the work challenge
 *      at the very end.
 */
public class LockUnificationPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_UNIFICATION = "unification";

    private static final int MY_USER_ID = UserHandle.myUserId();

    private final UserManager mUm;
    private final DevicePolicyManager mDpm;
    private final LockPatternUtils mLockPatternUtils;
    private final int mProfileUserId;
    private final SettingsPreferenceFragment mHost;

    private RestrictedSwitchPreference mUnifyProfile;

    private final String mPreferenceKey;

    private LockscreenCredential mCurrentDevicePassword;
    private LockscreenCredential mCurrentProfilePassword;
    private boolean mRequireNewDevicePassword;

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mUnifyProfile = screen.findPreference(mPreferenceKey);
    }

    public LockUnificationPreferenceController(Context context, SettingsPreferenceFragment host) {
        this(context, host, KEY_UNIFICATION);
    }

    public LockUnificationPreferenceController(
            Context context, SettingsPreferenceFragment host, String key) {
        super(context);
        mHost = host;
        mUm = context.getSystemService(UserManager.class);
        mDpm = context.getSystemService(DevicePolicyManager.class);
        mLockPatternUtils = FeatureFactory.getFeatureFactory()
                .getSecurityFeatureProvider()
                .getLockPatternUtils(context);
        mProfileUserId = Utils.getManagedProfileId(mUm, MY_USER_ID);
        mCurrentDevicePassword = LockscreenCredential.createNone();
        mCurrentProfilePassword = LockscreenCredential.createNone();
        this.mPreferenceKey = key;
    }

    @Override
    public boolean isAvailable() {
        return mProfileUserId != UserHandle.USER_NULL
                && mUm.isManagedProfile(mProfileUserId);
    }

    @Override
    public String getPreferenceKey() {
        return mPreferenceKey;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (Utils.startQuietModeDialogIfNecessary(mContext, mUm, mProfileUserId)) {
            return false;
        }
        final boolean useOneLock = (Boolean) value;
        if (useOneLock) {
            mRequireNewDevicePassword = !mDpm.isPasswordSufficientAfterProfileUnification(
                    UserHandle.myUserId(), mProfileUserId);
            startUnification();
        } else {
            final String title = mContext.getString(R.string.unlock_set_unlock_launch_picker_title);
            final ChooseLockSettingsHelper.Builder builder =
                    new ChooseLockSettingsHelper.Builder(mHost.getActivity(), mHost);
            final boolean launched = builder.setRequestCode(UNUNIFY_LOCK_CONFIRM_DEVICE_REQUEST)
                    .setTitle(title)
                    .setReturnCredentials(true)
                    .setUserId(MY_USER_ID)
                    .show();

            if (!launched) {
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
            mCurrentDevicePassword =
                    data.getParcelableExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
            ununifyLocks();
            return true;
        } else if (requestCode == UNIFY_LOCK_CONFIRM_PROFILE_REQUEST
                && resultCode == Activity.RESULT_OK) {
            mCurrentProfilePassword =
                    data.getParcelableExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
            unifyLocks();
            return true;
        }
        return false;
    }

    private void ununifyLocks() {
        final Bundle extras = new Bundle();
        extras.putInt(Intent.EXTRA_USER_ID, mProfileUserId);
        extras.putParcelable(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD, mCurrentDevicePassword);
        new SubSettingLauncher(mContext)
                .setDestination(ChooseLockGeneric.ChooseLockGenericFragment.class.getName())
                .setSourceMetricsCategory(mHost.getMetricsCategory())
                .setArguments(extras)
                .setTransitionType(SettingsTransitionHelper.TransitionType.TRANSITION_SLIDE)
                .launch();
    }

    /**
     * Unify primary and profile locks.
     */
    public void startUnification() {
        // Confirm profile lock
        final String title = mDpm.getResources().getString(
                WORK_PROFILE_SET_UNLOCK_LAUNCH_PICKER_TITLE,
                () -> mContext.getString(R.string.unlock_set_unlock_launch_picker_title_profile));
        final ChooseLockSettingsHelper.Builder builder =
                new ChooseLockSettingsHelper.Builder(mHost.getActivity(), mHost);
        final boolean launched = builder.setRequestCode(UNIFY_LOCK_CONFIRM_PROFILE_REQUEST)
                .setTitle(title)
                .setReturnCredentials(true)
                .setUserId(mProfileUserId)
                .show();
        if (!launched) {
            // If profile has no lock, go straight to unification.
            unifyLocks();
            // TODO: update relevant prefs.
            // createPreferenceHierarchy();
        }
    }

    private void unifyLocks() {
        if (mRequireNewDevicePassword) {
            promptForNewDeviceLockAndThenUnify();
        } else {
            unifyKeepingDeviceLock();
        }
        if (mCurrentDevicePassword != null) {
            mCurrentDevicePassword.zeroize();
            mCurrentDevicePassword = null;
        }
        if (mCurrentProfilePassword != null) {
            mCurrentProfilePassword.zeroize();
            mCurrentProfilePassword = null;
        }
    }

    private void unifyKeepingDeviceLock() {
        mLockPatternUtils.setSeparateProfileChallengeEnabled(mProfileUserId, false,
                mCurrentProfilePassword);
    }

    private void promptForNewDeviceLockAndThenUnify() {
        final Bundle extras = new Bundle();
        extras.putInt(ChooseLockSettingsHelper.EXTRA_KEY_UNIFICATION_PROFILE_ID, mProfileUserId);
        extras.putParcelable(ChooseLockSettingsHelper.EXTRA_KEY_UNIFICATION_PROFILE_CREDENTIAL,
                mCurrentProfilePassword);
        new SubSettingLauncher(mContext)
                .setDestination(ChooseLockGeneric.ChooseLockGenericFragment.class.getName())
                .setTitleRes(R.string.lock_settings_picker_title)
                .setSourceMetricsCategory(mHost.getMetricsCategory())
                .setArguments(extras)
                .setTransitionType(SettingsTransitionHelper.TransitionType.TRANSITION_SLIDE)
                .launch();
    }

}
