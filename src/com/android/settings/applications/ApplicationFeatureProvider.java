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

package com.android.settings.applications;

import android.annotation.UserIdInt;
import android.content.Intent;

import java.util.List;
import java.util.Set;

public interface ApplicationFeatureProvider {

    /**
     * Calculates the total number of apps installed on the device via policy in the current user
     * and all its managed profiles.
     *
     * @param async    Whether to count asynchronously in a background thread
     * @param callback The callback to invoke with the result
     */
    void calculateNumberOfPolicyInstalledApps(boolean async, NumberOfAppsCallback callback);

    /**
     * Asynchronously builds the list of apps installed on the device via policy in the current user
     * and all its managed profiles.
     *
     * @param callback The callback to invoke with the result
     */
    void listPolicyInstalledApps(ListOfAppsCallback callback);

    /**
     * Asynchronously calculates the total number of apps installed in the current user and all its
     * managed profiles that have been granted one or more of the given permissions by the admin.
     *
     * @param permissions Only consider apps that have been granted one or more of these
     *                    permissions by the admin, either at run-time or install-time
     * @param async       Whether to count asynchronously in a background thread
     * @param callback    The callback to invoke with the result
     */
    void calculateNumberOfAppsWithAdminGrantedPermissions(String[] permissions, boolean async,
            NumberOfAppsCallback callback);

    /**
     * Asynchronously builds the list of apps installed in the current user and all its
     * managed profiles that have been granted one or more of the given permissions by the admin.
     *
     * @param permissions Only consider apps that have been granted one or more of these
     *                    permissions by the admin, either at run-time or install-time
     * @param callback    The callback to invoke with the result
     */
    void listAppsWithAdminGrantedPermissions(String[] permissions, ListOfAppsCallback callback);

    /**
     * Return the persistent preferred activities configured by the admin for the given user.
     * A persistent preferred activity is an activity that the admin configured to always handle a
     * given intent (e.g. open browser), even if the user has other apps installed that would also
     * be able to handle the intent.
     *
     * @param userId  ID of the user for which to find persistent preferred activities
     * @param intents The intents for which to find persistent preferred activities
     * @return the persistent preferred activities for the given intents, ordered first by user id,
     * then by package name
     */
    List<UserAppInfo> findPersistentPreferredActivities(@UserIdInt int userId, Intent[] intents);

    /**
     * Returns a list of package names that should be kept enabled.
     */
    Set<String> getKeepEnabledPackages();

    /**
     * Returns a user readable text explaining how much time user has spent in an app at a
     * pre-specified duration.
     */
    default CharSequence getTimeSpentInApp(String packageName) {
        return null;
    }

    /**
     * Callback that receives the number of packages installed on the device.
     */
    interface NumberOfAppsCallback {
        void onNumberOfAppsResult(int num);
    }

    /**
     * Callback that receives the list of packages installed on the device.
     */
    interface ListOfAppsCallback {
        void onListOfAppsResult(List<UserAppInfo> result);
    }
}
