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

import static com.android.settings.privatespace.PrivateSpaceSetupActivity.EXTRA_ACTION_TYPE;
import static com.android.settings.privatespace.PrivateSpaceSetupActivity.SET_LOCK_ACTION;
import static com.android.settings.privatespace.onelock.UseOneLockSettingsFragment.UNIFY_PRIVATE_LOCK_WITH_DEVICE_REQUEST;
import static com.android.settings.privatespace.onelock.UseOneLockSettingsFragment.UNUNIFY_PRIVATE_LOCK_FROM_DEVICE_REQUEST;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.privatespace.PrivateProfileContextHelperActivity;
import com.android.settings.privatespace.PrivateSpaceMaintainer;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.transition.SettingsTransitionHelper;
import com.android.settingslib.widget.MainSwitchPreference;

/** Represents the preference controller for using the same lock as the screen lock */
public class UseOneLockControllerSwitch extends AbstractPreferenceController
        implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "UseOneLockSwitch";
    private static final String KEY_UNIFICATION = "private_lock_unification";
    private final String mPreferenceKey;
    private final SettingsPreferenceFragment mHost;
    private final LockPatternUtils mLockPatternUtils;
    private final UserManager mUserManager;
    private final int mProfileUserId;
    private final UserHandle mUserHandle;
    private LockscreenCredential mCurrentDevicePassword;
    private LockscreenCredential mCurrentProfilePassword;
    private MainSwitchPreference mUnifyProfile;

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mUnifyProfile = screen.findPreference(mPreferenceKey);
    }
    public UseOneLockControllerSwitch(Context context, SettingsPreferenceFragment host) {
        this(context, host, KEY_UNIFICATION);
    }

    public UseOneLockControllerSwitch(Context context, SettingsPreferenceFragment host,
              String key) {
        super(context);
        mHost = host;
        mUserManager = context.getSystemService(UserManager.class);
        mLockPatternUtils = FeatureFactory.getFeatureFactory().getSecurityFeatureProvider()
                  .getLockPatternUtils(context);
        mUserHandle =  PrivateSpaceMaintainer.getInstance(context).getPrivateProfileHandle();
        mProfileUserId = mUserHandle != null ? mUserHandle.getIdentifier() : -1;
        mCurrentDevicePassword = LockscreenCredential.createNone();
        mCurrentProfilePassword = LockscreenCredential.createNone();
        this.mPreferenceKey = key;
    }

    @Override
    public String getPreferenceKey() {
        return mPreferenceKey;
    }

    @Override
    public boolean isAvailable() {
        return android.os.Flags.allowPrivateProfile();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        //Checks if the profile is in quiet mode and show a dialog to unpause the profile.
        if (Utils.startQuietModeDialogIfNecessary(mContext, mUserManager, mProfileUserId)) {
            return false;
        }
        final boolean useOneLock = (Boolean) value;
        if (useOneLock) {
            startUnification();
        } else {
            showAlertDialog();
        }
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        if (mUnifyProfile != null) {
            final boolean separate =
                      mLockPatternUtils.isSeparateProfileChallengeEnabled(mProfileUserId);
            mUnifyProfile.setChecked(!separate);
        }
    }

    /** Method to handle onActivityResult */
    public boolean handleActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == UNUNIFY_PRIVATE_LOCK_FROM_DEVICE_REQUEST
                  && resultCode == Activity.RESULT_OK && data != null) {
            mCurrentDevicePassword =
                      data.getParcelableExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
            separateLocks();
            return true;
        } else if (requestCode == UNIFY_PRIVATE_LOCK_WITH_DEVICE_REQUEST
                  && resultCode == Activity.RESULT_OK && data != null) {
            mCurrentProfilePassword =
                      data.getParcelableExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
            unifyLocks();
            return true;
        }
        return false;
    }

    private void separateLocks() {
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

    /** Unify primary and profile locks. */
    public void startUnification() {
        // Confirm profile lock
        final ChooseLockSettingsHelper.Builder builder =
                  new ChooseLockSettingsHelper.Builder(mHost.getActivity(), mHost);
        final boolean launched = builder.setRequestCode(UNIFY_PRIVATE_LOCK_WITH_DEVICE_REQUEST)
                  .setReturnCredentials(true)
                  .setUserId(mProfileUserId)
                  .show();
        if (!launched) {
            // If profile has no lock, go straight to unification.
            unifyLocks();
        }
    }

    private void unifyLocks() {
        unifyKeepingDeviceLock();
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

    private void showAlertDialog() {
        if (mUserHandle == null) {
            Log.e(TAG, "Private profile user handle is not expected to be null");
            mUnifyProfile.setChecked(true);
            return;
        }
        new AlertDialog.Builder(mContext)
                  .setMessage(R.string.private_space_new_lock_title)
                  .setPositiveButton(
                            R.string.private_space_set_lock_label,
                            (dialog, which) -> {
                                Intent intent = new Intent(mContext,
                                          PrivateProfileContextHelperActivity.class);
                                intent.putExtra(EXTRA_ACTION_TYPE, SET_LOCK_ACTION);
                                ((Activity) mContext).startActivityForResultAsUser(intent,
                                          UNUNIFY_PRIVATE_LOCK_FROM_DEVICE_REQUEST,
                                          /*Options*/ null, mUserHandle);
                            })
                  .setNegativeButton(R.string.private_space_cancel_label,
                            (DialogInterface dialog, int which) -> {
                                mUnifyProfile.setChecked(true);
                                dialog.dismiss();
                            })
                  .setOnCancelListener(
                            (DialogInterface dialog) -> {
                                mUnifyProfile.setChecked(true);
                                dialog.dismiss();
                            })
                  .show();
    }
}
