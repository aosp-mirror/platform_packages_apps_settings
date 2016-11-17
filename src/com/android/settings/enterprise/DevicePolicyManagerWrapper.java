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

package com.android.settings.enterprise;

import android.content.ComponentName;

/**
 * This interface replicates a subset of the android.app.admin.DevicePolicyManager (DPM). The
 * interface exists so that we can use a thin wrapper around the DPM in production code and a mock
 * in tests. We cannot directly mock or shadow the DPM, because some of the methods we rely on are
 * newer than the API version supported by Robolectric.
 */
public interface DevicePolicyManagerWrapper {
    /**
     * Calls {@code DevicePolicyManager.getDeviceOwnerComponentOnAnyUser()}.
     *
     * @see android.app.admin.DevicePolicyManager#getDeviceOwnerComponentOnAnyUser
     */
    ComponentName getDeviceOwnerComponentOnAnyUser();
}
