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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.Utils;

public class SwipeUpPreferenceController extends GesturePreferenceController {

    private final int ON = 1;
    private final int OFF = 0;

    // TODO(77474484): Update when swipe up illustration video is ready
    private static final String PREF_KEY_VIDEO = "";
    private final UserManager mUserManager;

    public SwipeUpPreferenceController(Context context, String key) {
        super(context, key);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
    }

    static boolean isGestureAvailable(Context context) {
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return isGestureAvailable(mContext) ? AVAILABLE : DISABLED_UNSUPPORTED;
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
        final int swipeUpEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SWIPE_UP_TO_SWITCH_APPS_ENABLED, OFF);
        return swipeUpEnabled != OFF;
    }
}
