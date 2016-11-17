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

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;

import java.util.List;

/**
 * This interface replicates a subset of the android.content.pm.PackageManager (PM). The interface
 * exists so that we can use a thin wrapper around the PM in production code and a mock in tests.
 * We cannot directly mock or shadow the PM, because some of the methods we rely on are newer than
 * the API version supported by Robolectric.
 */
public interface PackageManagerWrapper {
    /**
     * Calls {@code PackageManager.getInstalledApplicationsAsUser()}.
     *
     * @see android.content.pm.PackageManager.PackageManager#getInstalledApplicationsAsUser
     */
    List<ApplicationInfo> getInstalledApplicationsAsUser(int flags, int userId);

    /**
     * Calls {@code PackageManager.queryIntentActivitiesAsUser()}.
     *
     * @see android.content.pm.PackageManager.PackageManager#queryIntentActivitiesAsUser
     */
    List<ResolveInfo> queryIntentActivitiesAsUser(Intent intent, int flags, int userId);
}
