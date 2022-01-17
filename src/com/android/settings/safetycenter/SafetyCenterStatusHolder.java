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

package com.android.settings.safetycenter;

import android.content.Context;
import android.safetycenter.SafetyCenterManager;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

/** Knows whether safety center is enabled or disabled. */
public class SafetyCenterStatusHolder {

    private static final String TAG = "SafetyCenterStatusHolder";

    @VisibleForTesting
    public static SafetyCenterStatusHolder sInstance;

    private SafetyCenterStatusHolder() {}

    /** Returns an instance of {@link SafetyCenterStatusHolder}. */
    public static SafetyCenterStatusHolder get() {
        if (sInstance == null) {
            sInstance = new SafetyCenterStatusHolder();
        }
        return sInstance;
    }

    /** Returns true is SafetyCenter page is enabled, false otherwise. */
    public boolean isEnabled(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null at SafetyCenterStatusHolder#isEnabled");
            return false;
        }
        SafetyCenterManager safetyCenterManager =
                context.getSystemService(SafetyCenterManager.class);
        if (safetyCenterManager == null) {
            Log.w(TAG, "System service SAFETY_CENTER_SERVICE (SafetyCenterManager) is null");
            return false;
        }
        try {
            return safetyCenterManager.isSafetyCenterEnabled();
        } catch (RuntimeException e) {
            Log.e(TAG, "Calling isSafetyCenterEnabled failed.", e);
            return false;
        }
    }
}
