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
package com.android.settings.users;

import android.content.Context;
import android.os.UserHandle;

import androidx.fragment.app.Fragment;

import com.android.settings.Utils;

public class AutoSyncWorkDataPreferenceController extends AutoSyncPersonalDataPreferenceController {

    private static final String TAG = "AutoSyncWorkData";
    private static final String KEY_AUTO_SYNC_WORK_ACCOUNT = "auto_sync_work_account_data";

    public AutoSyncWorkDataPreferenceController(Context context, Fragment parent) {
        super(context, parent);
        mUserHandle = Utils.getManagedProfileWithDisabled(mUserManager);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_AUTO_SYNC_WORK_ACCOUNT;
    }

    @Override
    public boolean isAvailable() {
        return mUserHandle != null && !mUserManager.isManagedProfile() && !mUserManager.isLinkedUser()
                && mUserManager.getProfiles(UserHandle.myUserId()).size() > 1;
    }
}
