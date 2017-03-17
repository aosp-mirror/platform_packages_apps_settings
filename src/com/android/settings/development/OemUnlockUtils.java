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

package com.android.settings.development;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.persistentdata.PersistentDataBlockManager;
import android.util.Log;

public class OemUnlockUtils {
    private static final String TAG = "OemUnlockUtils";

    /**
     * Returns whether or not this device is able to be OEM unlocked.
     */
    static boolean isOemUnlockEnabled(Context context) {
        PersistentDataBlockManager manager = (PersistentDataBlockManager)
                context.getSystemService(Context.PERSISTENT_DATA_BLOCK_SERVICE);
        return manager.getOemUnlockEnabled();
    }

    /**
     * Allows enabling or disabling OEM unlock on this device. OEM unlocked
     * devices allow users to flash other OSes to them.
     */
    static void setOemUnlockEnabled(Context context, boolean enabled) {
        try {
            PersistentDataBlockManager manager = (PersistentDataBlockManager)
                    context.getSystemService(Context.PERSISTENT_DATA_BLOCK_SERVICE);
            manager.setOemUnlockEnabled(enabled);
        } catch (SecurityException e) {
            Log.e(TAG, "Fail to set oem unlock.", e);
        }
    }

    /**
     * Returns {@code true} if OEM unlock is disallowed by user restriction
     * {@link UserManager#DISALLOW_FACTORY_RESET} or {@link UserManager#DISALLOW_OEM_UNLOCK}.
     * Otherwise, returns {@code false}.
     */
    static boolean isOemUnlockAllowed(UserManager um) {
        final UserHandle userHandle = UserHandle.of(UserHandle.myUserId());
        return !(um.hasBaseUserRestriction(UserManager.DISALLOW_OEM_UNLOCK, userHandle)
                || um.hasBaseUserRestriction(UserManager.DISALLOW_FACTORY_RESET, userHandle));
    }
}
