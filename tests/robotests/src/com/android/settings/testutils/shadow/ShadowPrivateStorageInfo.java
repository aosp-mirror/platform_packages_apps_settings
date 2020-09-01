/*
 * Copyright (C) 2019 The Android Open Source Project
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

import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.deviceinfo.StorageVolumeProvider;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

@Implements(PrivateStorageInfo.class)
public class ShadowPrivateStorageInfo {

    private static PrivateStorageInfo sPrivateStorageInfo = null;

    @Resetter
    public static void reset() {
        sPrivateStorageInfo = null;
    }

    @Implementation
    protected static PrivateStorageInfo getPrivateStorageInfo(
            StorageVolumeProvider storageVolumeProvider) {
        return sPrivateStorageInfo;
    }

    public static void setPrivateStorageInfo(
            PrivateStorageInfo privateStorageInfo) {
        sPrivateStorageInfo = privateStorageInfo;
    }
}
