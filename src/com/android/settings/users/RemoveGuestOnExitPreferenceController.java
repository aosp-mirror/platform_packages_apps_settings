/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.settings.users;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.RestrictedSwitchPreference;

/**
 * Controller to control the preference toggle for "remove guest on exit"
 *
 * Note, class is not 'final' since we need to mock it for unit tests
 */
public class RemoveGuestOnExitPreferenceController extends BasePreferenceController
                implements  Preference.OnPreferenceChangeListener {

    private static final String TAG = RemoveGuestOnExitPreferenceController.class.getSimpleName();
    private static final String TAG_CONFIRM_GUEST_REMOVE = "confirmGuestRemove";
    private static final int REMOVE_GUEST_ON_EXIT_DEFAULT = 1;

    private final UserCapabilities mUserCaps;
    private final UserManager mUserManager;
    private final Fragment mParentFragment;
    private final Handler mHandler;

    public RemoveGuestOnExitPreferenceController(Context context, String key,
            Fragment parent, Handler handler) {
        super(context, key);
        mUserCaps = UserCapabilities.create(context);
        mUserManager = context.getSystemService(UserManager.class);
        mParentFragment = parent;
        mHandler = handler;
    }

    @Override
    public void updateState(Preference preference) {
        mUserCaps.updateAddUserCapabilities(mContext);
        final RestrictedSwitchPreference restrictedSwitchPreference =
                (RestrictedSwitchPreference) preference;
        restrictedSwitchPreference.setChecked(isChecked());
        if (!isAvailable()) {
            restrictedSwitchPreference.setVisible(false);
        } else {
            restrictedSwitchPreference.setDisabledByAdmin(
                    mUserCaps.disallowAddUser() ? mUserCaps.getEnforcedAdmin() : null);
            restrictedSwitchPreference.setVisible(mUserCaps.mUserSwitcherEnabled);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        // if guest is forced to be ephemeral via config_guestUserEphemeral
        // then disable this controller
        // also disable this controller for non-admin users
        // also disable when config_guestUserAllowEphemeralStateChange is false
        if (mUserManager.isGuestUserAlwaysEphemeral()
                || !UserManager.isGuestUserAllowEphemeralStateChange()
                || !mUserCaps.isAdmin()
                || mUserCaps.disallowAddUser()
                || mUserCaps.disallowAddUserSetByAdmin()) {
            return DISABLED_FOR_USER;
        } else {
            return mUserCaps.mUserSwitcherEnabled ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
        }
    }

    private boolean isChecked() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.REMOVE_GUEST_ON_EXIT, REMOVE_GUEST_ON_EXIT_DEFAULT) != 0;
    }

    private static boolean setChecked(Context context, boolean isChecked) {
        Settings.Global.putInt(context.getContentResolver(),
                Settings.Global.REMOVE_GUEST_ON_EXIT, isChecked ? 1 : 0);
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enable = (boolean) newValue;
        UserInfo guestInfo = mUserManager.findCurrentGuestUser();

        // no guest do the setting and return
        // guest ephemeral state will take effect on guest create
        if (guestInfo == null) {
            return setChecked(mContext, enable);
        }
        // if guest has never been initialized or started
        // we can change guest ephemeral state
        if (!guestInfo.isInitialized()) {
            boolean isSuccess = mUserManager.setUserEphemeral(guestInfo.id, enable);
            if (isSuccess) {
                return setChecked(mContext, enable);
            } else {
                Log.w(TAG, "Unused guest, id=" + guestInfo.id
                        + ". Mark ephemeral as " + enable + " failed !!!");
                return false;
            }
        }
        // if guest has been used before and is not ephemeral
        // but now we are making reset guest on exit preference as enabled
        // then show confirmation dialog box and remove this guest if confirmed by user
        if (guestInfo.isInitialized() && !guestInfo.isEphemeral() && enable) {
            ConfirmGuestRemoveFragment.show(mParentFragment,
                    mHandler,
                    enable,
                    guestInfo.id,
                    (RestrictedSwitchPreference) preference);
            return false;
        }
        // all other cases, there should be none, don't change state
        return false;
    }


    /**
     * Dialog to confirm guest removal on toggle clicked set to true
     *
     * Fragment must be a public static class to be  properly recreated from instance state
     * else we will get "AndroidRuntime: java.lang.IllegalStateException"
     */
    public static final class ConfirmGuestRemoveFragment extends InstrumentedDialogFragment
            implements DialogInterface.OnClickListener {

        private static final String TAG = ConfirmGuestRemoveFragment.class.getSimpleName();
        private static final String SAVE_ENABLING = "enabling";
        private static final String SAVE_GUEST_USER_ID = "guestUserId";

        private boolean mEnabling;
        private int mGuestUserId;
        private RestrictedSwitchPreference mPreference;
        private Handler mHandler;

        private static void show(Fragment parent,
                Handler handler,
                boolean enabling, int guestUserId,
                RestrictedSwitchPreference preference) {
            if (!parent.isAdded()) return;

            final ConfirmGuestRemoveFragment dialog = new ConfirmGuestRemoveFragment();
            dialog.mHandler = handler;
            dialog.mEnabling = enabling;
            dialog.mGuestUserId = guestUserId;
            dialog.setTargetFragment(parent, 0);
            dialog.mPreference = preference;
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_GUEST_REMOVE);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            if (savedInstanceState != null) {
                mEnabling = savedInstanceState.getBoolean(SAVE_ENABLING);
                mGuestUserId = savedInstanceState.getInt(SAVE_GUEST_USER_ID);
            }

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.remove_guest_on_exit_dialog_title);
            builder.setMessage(R.string.remove_guest_on_exit_dialog_message);
            builder.setPositiveButton(
                        com.android.settingslib.R.string.guest_exit_clear_data_button, this);
            builder.setNegativeButton(android.R.string.cancel, null);

            return builder.create();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putBoolean(SAVE_ENABLING, mEnabling);
            outState.putInt(SAVE_GUEST_USER_ID, mGuestUserId);
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_USER_REMOVE;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which != DialogInterface.BUTTON_POSITIVE) {
                return;
            }
            UserManager userManager = getContext().getSystemService(UserManager.class);
            if (userManager == null) {
                Log.e(TAG, "Unable to get user manager service");
                return;
            }
            UserInfo guestUserInfo = userManager.getUserInfo(mGuestUserId);
            // only do action for guests and when enabling the preference
            if (guestUserInfo == null || !guestUserInfo.isGuest() || !mEnabling) {
                Log.w(TAG, "Removing guest user ... failed, id=" + mGuestUserId);
                return;
            }
            if (mPreference != null) {
                // Using markGuestForDeletion allows us to create a new guest before this one is
                // fully removed.
                boolean isSuccess = userManager.markGuestForDeletion(guestUserInfo.id);
                if (!isSuccess) {
                    Log.w(TAG, "Couldn't mark the guest for deletion for user "
                            + guestUserInfo.id);
                    return;
                }
                userManager.removeUser(guestUserInfo.id);
                if (setChecked(getContext(), mEnabling)) {
                    mPreference.setChecked(mEnabling);
                    mHandler.sendEmptyMessage(
                                UserSettings
                                    .MESSAGE_REMOVE_GUEST_ON_EXIT_CONTROLLER_GUEST_REMOVED);
                }
            }
        }
    }
}
