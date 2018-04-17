/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.gestures;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.UserManager;
import android.provider.Settings;
import androidx.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.android.internal.R;

public class SwipeUpPreferenceController extends GesturePreferenceController {

    private final int ON = 1;
    private final int OFF = 0;

    private static final String ACTION_QUICKSTEP = "android.intent.action.QUICKSTEP_SERVICE";
    private static final String PREF_KEY_VIDEO = "gesture_swipe_up_video";
    private final UserManager mUserManager;

    public SwipeUpPreferenceController(Context context, String key) {
        super(context, key);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
    }

    static boolean isGestureAvailable(Context context) {
        if (!context.getResources().getBoolean(R.bool.config_swipe_up_gesture_setting_available)) {
            return false;
        }

        final ComponentName recentsComponentName = ComponentName.unflattenFromString(
                context.getString(R.string.config_recentsComponentName));
        final Intent quickStepIntent = new Intent(ACTION_QUICKSTEP)
                .setPackage(recentsComponentName.getPackageName());
        if (context.getPackageManager().resolveService(quickStepIntent,
                PackageManager.MATCH_SYSTEM_ONLY) == null) {
            return false;
        }
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return isGestureAvailable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "gesture_swipe_up");
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        setSwipeUpPreference(mContext, mUserManager, isChecked ? ON : OFF);
        return true;
    }

    public static void setSwipeUpPreference(Context context, UserManager userManager,
            int enabled) {
        Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.SWIPE_UP_TO_SWITCH_APPS_ENABLED, enabled);
    }

    @Override
    public boolean isChecked() {
        final int defaultValue = mContext.getResources()
                .getBoolean(R.bool.config_swipe_up_gesture_default) ? ON : OFF;
        final int swipeUpEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SWIPE_UP_TO_SWITCH_APPS_ENABLED, defaultValue);
        return swipeUpEnabled != OFF;
    }
}
