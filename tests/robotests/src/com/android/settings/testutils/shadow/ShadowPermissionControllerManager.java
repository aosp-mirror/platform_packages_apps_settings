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

import android.annotation.CallbackExecutor;
import android.content.Context;
import android.os.Handler;
import android.permission.PermissionControllerManager;

import androidx.annotation.NonNull;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.concurrent.Executor;

@Implements(PermissionControllerManager.class)
public class ShadowPermissionControllerManager {

    protected void __constructor__(Context contexts, Handler handler) {
        // no nothing, everything is shadowed
    }

    @Implementation
    public void getPermissionUsages(boolean countSystem, long numMillis,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull PermissionControllerManager.OnPermissionUsageResultCallback callback) {

        // Do nothing
    }
}
