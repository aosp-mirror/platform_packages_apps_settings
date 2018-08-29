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

package com.android.settings.applications.defaultapps;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.settings.Utils;
import com.android.settingslib.applications.DefaultAppInfo;

public class DefaultWorkAutofillPreferenceController extends DefaultAutofillPreferenceController {
    private final UserHandle mUserHandle;

    public DefaultWorkAutofillPreferenceController(Context context) {
        super(context);
        mUserHandle = Utils.getManagedProfile(mUserManager);
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
        return "default_autofill_work";
    }

    @Override
    protected DefaultAppInfo getDefaultAppInfo() {
        final String flattenComponent = Settings.Secure.getStringForUser(
                mContext.getContentResolver(),
                DefaultAutofillPicker.SETTING,
                mUserHandle.getIdentifier());
        if (!TextUtils.isEmpty(flattenComponent)) {
            DefaultAppInfo appInfo = new DefaultAppInfo(
                    mContext,
                    mPackageManager,
                    mUserHandle.getIdentifier(),
                    ComponentName.unflattenFromString(flattenComponent));
            return appInfo;
        }
        return null;
    }

    @Override
    protected Intent getSettingIntent(DefaultAppInfo info) {
        if (info == null) {
            return null;
        }
        final DefaultAutofillPicker.AutofillSettingIntentProvider intentProvider =
                new DefaultAutofillPicker.AutofillSettingIntentProvider(
                        mContext, mUserHandle.getIdentifier(), info.getKey());
        return intentProvider.getIntent();
    }

    @Override
    protected void startActivity(Intent intent) {
        mContext.startActivityAsUser(intent, mUserHandle);
    }
}
