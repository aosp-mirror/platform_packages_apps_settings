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

import android.app.settings.SettingsEnums;
import android.content.pm.UserInfo;

/**
 * Utils class for metrics to avoid user characteristics checks in code
 */
public class UserMetricsUtils {

    /**
     * Returns relevant remove SettingsEnum key depending on UserInfo
     * @param userInfo information about user
     * @return list of RestrictionEntry objects with user-visible text.
     */
    public static int getRemoveUserMetricCategory(UserInfo userInfo) {
        if (userInfo.isGuest()) {
            return  SettingsEnums.ACTION_REMOVE_GUEST_USER;
        }
        if (userInfo.isRestricted()) {
            return SettingsEnums.ACTION_REMOVE_RESTRICTED_USER;
        }
        return SettingsEnums.ACTION_REMOVE_USER;
    }

    /**
     * Returns relevant switch user SettingsEnum key depending on UserInfo
     * @param userInfo information about user
     * @return SettingsEnums.
     */
    public static int getSwitchUserMetricCategory(UserInfo userInfo) {
        if (userInfo.isGuest()) {
            return  SettingsEnums.ACTION_SWITCH_TO_GUEST;
        }
        if (userInfo.isRestricted()) {
            return SettingsEnums.ACTION_SWITCH_TO_RESTRICTED_USER;
        }
        return SettingsEnums.ACTION_SWITCH_TO_USER;
    }
}
