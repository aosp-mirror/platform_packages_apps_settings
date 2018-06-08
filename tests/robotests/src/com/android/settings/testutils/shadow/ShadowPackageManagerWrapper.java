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

package com.android.settings.testutils.shadow;

import android.content.pm.PackageManager.NameNotFoundException;

import com.android.settingslib.wrapper.PackageManagerWrapper;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.HashMap;

/**
 * Shadow for {@link PackageManagerWrapper} to allow stubbing hidden methods.
 */
@Implements(PackageManagerWrapper.class)
public class ShadowPackageManagerWrapper {
    private static final HashMap<String, Integer> packageUids = new HashMap<>();

    @Implementation
    public int getPackageUidAsUser(String packageName, int userId) throws NameNotFoundException {
        Integer res = packageUids.get(packageName + userId);
        if (res == null) {
            throw new NameNotFoundException();
        }
        return res;
    }

    public static void setPackageUidAsUser(String packageName, int userId, int uid) {
        packageUids.put(packageName + userId, uid);
    }
}
