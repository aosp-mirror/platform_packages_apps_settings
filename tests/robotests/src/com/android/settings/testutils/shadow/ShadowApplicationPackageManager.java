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

package com.android.settings.testutils.shadow;

import android.annotation.NonNull;
import android.app.ApplicationPackageManager;
import android.content.pm.PackageInfo;
import android.os.IRemoteCallback;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Implements(ApplicationPackageManager.class)
public class ShadowApplicationPackageManager
        extends org.robolectric.shadows.ShadowApplicationPackageManager {
    static final Map<Integer, List<String>> packagesForUserId = new HashMap<>();

    public void setInstalledPackagesForUserId(int userId, List<String> packages) {
        packagesForUserId.put(userId, packages);
        for (String packageName : packages) {
            addPackage(packageName);
        }
    }

    protected List<PackageInfo> getInstalledPackagesAsUser(int flags, int userId) {
        List<PackageInfo> packages = new ArrayList<>();
        for (String packageName : packagesForUserId.getOrDefault(userId, new ArrayList<>())) {
            try {
                packages.add(getPackageInfo(packageName, flags));
            } catch (Exception e) {
                // ignore
            }
        }
        return packages;
    }

    @Implementation
    public void registerPackageMonitorCallback(@NonNull IRemoteCallback callback, int userId) {}

    @Implementation
    public void unregisterPackageMonitorCallback(@NonNull IRemoteCallback callback) {}
}
