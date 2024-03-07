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

package com.android.settings.users;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;

public class MultiUserPreferenceController extends BasePreferenceController {

    public MultiUserPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return (UserHandle.MU_ENABLED && UserManager.supportsMultipleUsers()
                && !Utils.isMonkeyRunning()) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        UserManager um = mContext.getSystemService(UserManager.class);
        UserInfo info = um.getUserInfo(UserHandle.myUserId());
        return mContext.getString(R.string.users_summary, info.name);
    }
}
