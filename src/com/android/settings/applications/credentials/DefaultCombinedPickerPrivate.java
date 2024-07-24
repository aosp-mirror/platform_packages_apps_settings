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

package com.android.settings.applications.credentials;

import android.os.UserManager;
import android.util.Slog;

import com.android.settings.Utils;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment.ProfileType;

public class DefaultCombinedPickerPrivate extends DefaultCombinedPicker {
    private static final String TAG = "DefaultCombinedPickerPrivate";

    @Override
    protected int getUser() {
        UserManager userManager = getContext().getSystemService(UserManager.class);
        return Utils.getCurrentUserIdOfType(userManager, ProfileType.PRIVATE);
    }

    /** Returns whether the user is handled by this fragment. */
    public static boolean isUserHandledByFragment(UserManager userManager) {
        try {
            // If there is no private profile then this will throw an exception.
            Utils.getCurrentUserIdOfType(userManager, ProfileType.PRIVATE);
            return true;
        } catch (IllegalStateException e) {
            Slog.e(TAG, "Failed to get private profile user id", e);
            return false;
        }
    }
}
