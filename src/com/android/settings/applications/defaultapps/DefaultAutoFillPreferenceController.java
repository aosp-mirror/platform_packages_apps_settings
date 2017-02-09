/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.provider.Settings;
import android.text.TextUtils;

public class DefaultAutoFillPreferenceController extends DefaultAppPreferenceController {

    public DefaultAutoFillPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return "default_autofill";
    }

    @Override
    protected Intent getSettingIntent(DefaultAppInfo info) {
        if (info == null) {
            return null;
        }
        final DefaultAutoFillPicker.AutoFillSettingIntentProvider intentProvider =
                new DefaultAutoFillPicker.AutoFillSettingIntentProvider(
                        mPackageManager.getPackageManager(), info.getKey());
        return intentProvider.getIntent();
    }

    @Override
    protected DefaultAppInfo getDefaultAppInfo() {
        final String flattenComponent = Settings.Secure.getString(mContext.getContentResolver(),
                DefaultAutoFillPicker.SETTING);
        if (!TextUtils.isEmpty(flattenComponent)) {
            DefaultAppInfo appInfo = new DefaultAppInfo(
                    mUserId, ComponentName.unflattenFromString(flattenComponent), null /*summary*/);
            return appInfo;
        }
        return null;
    }
}
