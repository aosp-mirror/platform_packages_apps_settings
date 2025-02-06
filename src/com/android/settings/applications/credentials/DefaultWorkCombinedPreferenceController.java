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

package com.android.settings.applications.credentials;

import android.annotation.Nullable;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

public class DefaultWorkCombinedPreferenceController extends DefaultCombinedPreferenceController {

    @Nullable
    private final UserHandle mUserHandle;

    public DefaultWorkCombinedPreferenceController(Context context) {
        super(context, /*isWorkProfile=*/ true, /*isPrivateSpace=*/ false);
        mUserHandle = UserUtils.getManagedProfile(mUserManager);
    }

    @Override
    public boolean isAvailable() {
        if (mUserHandle == null) {
            return false;
        }
        return super.isAvailable();
    }

    @Override
    public String getPreferenceKey() {
        return "default_credman_autofill_main_work";
    }

    @Override
    protected void startActivity(Intent intent) {
        mContext.startActivityAsUser(intent, mUserHandle);
    }
}
