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

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArraySet;
import android.util.Log;

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
    final synchronized boolean createPrivateSpace() {
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
                am.startProfile(mUserHandle.getIdentifier());
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to start private profile");
                return false;
            }

            Log.i(TAG, "Private space created with id: " + mUserHandle.getIdentifier());
        }
        return true;
    }

    /** Returns the {@link ErrorDeletingPrivateSpace} enum representing the result of operation.
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
    }
}
