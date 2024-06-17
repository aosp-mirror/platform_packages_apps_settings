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
import android.safetycenter.SafetyEvent;
import android.safetycenter.SafetySourceData;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.internal.annotations.VisibleForTesting;

/** A wrapper for the SafetyCenterManager system service. */
public class SafetyCenterManagerWrapper {

    /**
     * Tag for logging.
     *
     * <p>The tag is restricted to 23 characters (the maximum allowed for Android logging).
     */
    private static final String TAG = "SafetyCenterManagerWrap";

    @VisibleForTesting
    public static SafetyCenterManagerWrapper sInstance;

    private SafetyCenterManagerWrapper() {}

    /** Returns an instance of {@link SafetyCenterManagerWrapper}. */
    public static SafetyCenterManagerWrapper get() {
        if (sInstance == null) {
            sInstance = new SafetyCenterManagerWrapper();
        }
        return sInstance;
    }

    /** Sets the latest safety source data for Safety Center. */
    public void setSafetySourceData(Context context, String safetySourceId,
            @Nullable SafetySourceData safetySourceData,
            SafetyEvent safetyEvent) {
        SafetyCenterManager safetyCenterManager =
                context.getSystemService(SafetyCenterManager.class);

        if (safetyCenterManager == null) {
            Log.e(TAG, "System service SAFETY_CENTER_SERVICE (SafetyCenterManager) is null");
            return;
        }

        try {
            safetyCenterManager.setSafetySourceData(
                    safetySourceId,
                    safetySourceData,
                    safetyEvent
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to send SafetySourceData", e);
            return;
        }
    }

    /** Returns true is SafetyCenter page is enabled, false otherwise. */
    public boolean isEnabled(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null at SafetyCenterManagerWrapper#isEnabled");
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
