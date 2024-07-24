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

package com.android.settings.privatespace;

import static android.os.UserManager.USER_TYPE_PROFILE_PRIVATE;
import static android.provider.Settings.Secure.HIDE_PRIVATESPACE_ENTRY_POINT;
import static android.provider.Settings.Secure.PRIVATE_SPACE_AUTO_LOCK;
import static android.provider.Settings.Secure.PRIVATE_SPACE_AUTO_LOCK_NEVER;
import static android.provider.Settings.Secure.USER_SETUP_COMPLETE;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.UserInfo;
import android.os.Flags;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.internal.annotations.GuardedBy;

import java.util.List;

// TODO(b/293569406): Update the javadoc when we have the setup flow in place to create PS

/** A class to help with the creation / deletion of Private Space */
public class PrivateSpaceMaintainer {
    private static final String TAG = "PrivateSpaceMaintainer";
    @GuardedBy("this")
    private static PrivateSpaceMaintainer sPrivateSpaceMaintainer;

    private final Context mContext;
    private final UserManager mUserManager;
    @GuardedBy("this")
    private UserHandle mUserHandle;
    private final KeyguardManager mKeyguardManager;

    /** This is the default value for the hide private space entry point settings. */
    public static final int HIDE_PRIVATE_SPACE_ENTRY_POINT_DISABLED_VAL = 0;
    public static final int HIDE_PRIVATE_SPACE_ENTRY_POINT_ENABLED_VAL = 1;
    /** Default value for private space auto lock settings. */
    @Settings.Secure.PrivateSpaceAutoLockOption
    public static final int PRIVATE_SPACE_AUTO_LOCK_DEFAULT_VAL = PRIVATE_SPACE_AUTO_LOCK_NEVER;

    public enum ErrorDeletingPrivateSpace {
        DELETE_PS_ERROR_NONE,
        DELETE_PS_ERROR_NO_PRIVATE_SPACE,
        DELETE_PS_ERROR_INTERNAL
    }

    /**
     * Returns true if the private space was successfully created.
     *
     * <p> This method should be used by the Private Space Setup Flow ONLY.
     */
    @VisibleForTesting
    public final synchronized boolean createPrivateSpace() {
        if (!Flags.allowPrivateProfile()) {
            return false;
        }
        // Check if Private space already exists
        if (doesPrivateSpaceExist()) {
            return true;
        }
        // a name indicating that the profile was created from the PS Settings page
        final String userName = "psSettingsUser";

        if (mUserHandle == null) {
            try {
                mUserHandle = mUserManager.createProfile(
                        userName, USER_TYPE_PROFILE_PRIVATE, new ArraySet<>());
            } catch (Exception e) {
                Log.e(TAG, "Error creating private space", e);
                return false;
            }

            if (mUserHandle == null) {
                Log.e(TAG, "Failed to create private space");
                return false;
            }

            IActivityManager am = ActivityManager.getService();
            try {
                //TODO(b/313926659): To check and handle failure of startProfile
                am.startProfile(mUserHandle.getIdentifier());
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to start private profile");
                return false;
            }

            Log.i(TAG, "Private space created with id: " + mUserHandle.getIdentifier());
            resetPrivateSpaceSettings();
            setUserSetupComplete();
        }
        return true;
    }

    /**
     * Returns the {@link ErrorDeletingPrivateSpace} enum representing the result of operation.
     *
     * <p> This method should be used ONLY by the delete-PS controller in the PS Settings page.
     */
    public synchronized ErrorDeletingPrivateSpace deletePrivateSpace() {
        if (!doesPrivateSpaceExist()) {
            return ErrorDeletingPrivateSpace.DELETE_PS_ERROR_NO_PRIVATE_SPACE;
        }

        try {
            Log.i(TAG, "Deleting Private space with id: " + mUserHandle.getIdentifier());
            if (mUserManager.removeUser(mUserHandle)) {
                Log.i(TAG, "Private space deleted");
                mUserHandle = null;

                return ErrorDeletingPrivateSpace.DELETE_PS_ERROR_NONE;
            } else {
                Log.e(TAG, "Failed to delete private space");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting private space", e);
        }
        return ErrorDeletingPrivateSpace.DELETE_PS_ERROR_INTERNAL;
    }

    /** Returns true if the Private space exists. */
    public synchronized boolean doesPrivateSpaceExist() {
        if (!Flags.allowPrivateProfile()) {
            return false;
        }
        if (mUserHandle != null) {
            return true;
        }

        List<UserInfo> users = mUserManager.getProfiles(0);
        for (UserInfo user : users) {
            if (user.isPrivateProfile()) {
                mUserHandle = user.getUserHandle();
                return true;
            }
        }
        return false;
    }

    /** Returns true when the PS is locked or when PS doesn't exist, false otherwise. */
    public synchronized boolean isPrivateSpaceLocked() {
        if (!doesPrivateSpaceExist()) {
            return true;
        }

        return mUserManager.isQuietModeEnabled(mUserHandle);
    }

    /**
     * Returns an intent to prompt the user to confirm private profile credentials if it is set
     * otherwise returns intent to confirm device credentials.
     */
    @Nullable
    public synchronized Intent getPrivateProfileLockCredentialIntent() {
        //TODO(b/307281644): To replace with check for doesPrivateSpaceExist() method once Auth
        // changes are merged.
        if (isPrivateProfileLockSet()) {
            return mKeyguardManager.createConfirmDeviceCredentialIntent(
                    /* title= */ null,  /* description= */null, mUserHandle.getIdentifier());
        }
        // TODO(b/304796434) Need to try changing this intent to use BiometricPrompt
        return mKeyguardManager.createConfirmDeviceCredentialIntent(
                /* title= */ null, /* description= */ null);
    }

    /** Returns Private profile user handle if private profile exists otherwise returns null. */
    @Nullable
    public synchronized UserHandle getPrivateProfileHandle() {
        if (doesPrivateSpaceExist()) {
            return mUserHandle;
        }
        return null;
    }

    /** Returns the instance of {@link PrivateSpaceMaintainer} */
    public static synchronized PrivateSpaceMaintainer getInstance(Context context) {
        if (sPrivateSpaceMaintainer == null) {
            sPrivateSpaceMaintainer = new PrivateSpaceMaintainer(context);
        }
        return sPrivateSpaceMaintainer;
    }

    private PrivateSpaceMaintainer(Context context) {
        mContext = context.getApplicationContext();
        mUserManager = mContext.getSystemService(UserManager.class);
        mKeyguardManager = mContext.getSystemService(KeyguardManager.class);
    }


    // TODO(b/307281644): Remove this method once new auth change is merged

    /**
     * Returns true if private space exists and a separate private profile lock is set
     * otherwise false when the private space does not exit or exists but does not have a
     * separate profile lock.
     */
    @GuardedBy("this")
    private boolean isPrivateProfileLockSet() {
        return doesPrivateSpaceExist()
                && mKeyguardManager.isDeviceSecure(mUserHandle.getIdentifier());
    }

    /** Sets the setting to show PS entry point to the provided value. */
    public void setHidePrivateSpaceEntryPointSetting(int value) {
        Log.d(TAG, "Setting HIDE_PRIVATE_SPACE_ENTRY_POINT = " + value);
        Settings.Secure.putInt(mContext.getContentResolver(), HIDE_PRIVATESPACE_ENTRY_POINT, value);
    }

    /** Sets the setting for private space auto lock option. */
    public void setPrivateSpaceAutoLockSetting(
            @Settings.Secure.PrivateSpaceAutoLockOption int value) {
        if (isPrivateSpaceAutoLockSupported()) {
            Settings.Secure.putInt(mContext.getContentResolver(), PRIVATE_SPACE_AUTO_LOCK, value);
        }
    }

    /** @return the setting to show PS entry point. */
    public int getHidePrivateSpaceEntryPointSetting() {
        return Settings.Secure.getInt(
                mContext.getContentResolver(),
                HIDE_PRIVATESPACE_ENTRY_POINT,
                HIDE_PRIVATE_SPACE_ENTRY_POINT_DISABLED_VAL);
    }

    /** @return the setting for PS auto lock option. */
    @Settings.Secure.PrivateSpaceAutoLockOption
    public int getPrivateSpaceAutoLockSetting() {
        if (isPrivateSpaceAutoLockSupported()) {
            return Settings.Secure.getInt(
                    mContext.getContentResolver(),
                    PRIVATE_SPACE_AUTO_LOCK,
                    PRIVATE_SPACE_AUTO_LOCK_DEFAULT_VAL);
        }
        return PRIVATE_SPACE_AUTO_LOCK_DEFAULT_VAL;
    }

    /**
     * Returns true if private space exists and quiet mode is successfully enabled, otherwise
     * returns false
     */
    public synchronized boolean lockPrivateSpace() {
        if (isPrivateProfileRunning()) {
            Log.d(TAG, "Calling requestQuietModeEnabled to enableQuietMode");
            return mUserManager.requestQuietModeEnabled(true, mUserHandle);
        }
        return false;
    }

    /**
     * Checks if private space exists and requests to disable quiet mode.
     *
     * @param intentSender target to start when the user is unlocked
     */
    public synchronized void unlockPrivateSpace(IntentSender intentSender) {
        if (mUserHandle != null) {
            Log.d(TAG, "Calling requestQuietModeEnabled to disableQuietMode");
            mUserManager.requestQuietModeEnabled(false, mUserHandle, intentSender);
        }
    }

    /** Returns true if private space exists and is running, otherwise returns false */
    @VisibleForTesting
    synchronized boolean isPrivateProfileRunning() {
        if (doesPrivateSpaceExist() && mUserHandle != null) {
            return mUserManager.isUserRunning(mUserHandle);
        }
        return false;
    }

    @GuardedBy("this")
    private void resetPrivateSpaceSettings() {
        setHidePrivateSpaceEntryPointSetting(HIDE_PRIVATE_SPACE_ENTRY_POINT_DISABLED_VAL);
        setPrivateSpaceAutoLockSetting(PRIVATE_SPACE_AUTO_LOCK_DEFAULT_VAL);
        setPrivateSpaceSensitiveNotificationsDefaultValue();
    }

    /** Sets private space sensitive notifications hidden on lockscreen by default */
    @GuardedBy("this")
    private void setPrivateSpaceSensitiveNotificationsDefaultValue() {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS,
                HidePrivateSpaceSensitiveNotificationsController.DISABLED,
                mUserHandle.getIdentifier());
    }

    /**
     * Sets the USER_SETUP_COMPLETE for private profile on which device theme is applied to the
     * profile.
     */
    @GuardedBy("this")
    private void setUserSetupComplete() {
        Log.d(TAG, "setting USER_SETUP_COMPLETE = 1 for private profile");
        Settings.Secure.putIntForUser(mContext.getContentResolver(), USER_SETUP_COMPLETE,
                1, mUserHandle.getIdentifier());
    }

    private boolean isPrivateSpaceAutoLockSupported() {
        return android.os.Flags.allowPrivateProfile()
                && android.multiuser.Flags.supportAutolockForPrivateSpace();
    }
}
