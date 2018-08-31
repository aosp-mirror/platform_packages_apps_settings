/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.accounts;

import android.annotation.UserIdInt;
import android.content.Context;

import com.android.settings.AccessiblePreferenceCategory;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;

import java.util.ArrayList;

public class AccountRestrictionHelper {

    private final Context mContext;

    public AccountRestrictionHelper(Context context) {
        mContext = context;
    }

    /**
     * Configure the UI of the preference by checking user restriction.
     * @param preference The preference we are configuring.
     * @param userRestriction The user restriction related to the preference.
     * @param userId The user that we retrieve user restriction of.
     */
    public void enforceRestrictionOnPreference(RestrictedPreference preference,
        String userRestriction, @UserIdInt int userId) {
        if (preference == null) {
            return;
        }
        if (hasBaseUserRestriction(userRestriction, userId)) {
            preference.setEnabled(false);
        } else {
            preference.checkRestrictionAndSetDisabled(userRestriction, userId);
        }
    }

    public boolean hasBaseUserRestriction(String userRestriction, @UserIdInt int userId) {
        return RestrictedLockUtilsInternal.hasBaseUserRestriction(mContext, userRestriction,
                userId);
    }

    public AccessiblePreferenceCategory createAccessiblePreferenceCategory(Context context) {
        return new AccessiblePreferenceCategory(context);
    }

    /**
     * Checks if the account should be shown based on the required authorities for the account type
     * @param authorities given authority that is passed as activity extra
     * @param auths list of authorities for particular account type
     * @return true if the activity has the required authority to show the account
     */
    public static boolean showAccount(String[] authorities, ArrayList<String> auths) {
        boolean showAccount = true;
        if (authorities != null && auths != null) {
            showAccount = false;
            for (String requestedAuthority : authorities) {
                if (auths.contains(requestedAuthority)) {
                    return true;
                }
            }
        }
        return showAccount;
    }
}
