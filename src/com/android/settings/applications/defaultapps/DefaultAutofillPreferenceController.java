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
import android.view.autofill.AutofillManager;

import com.android.settingslib.applications.DefaultAppInfo;

public class DefaultAutofillPreferenceController extends DefaultAppPreferenceController {

    private final AutofillManager mAutofillManager;

    public DefaultAutofillPreferenceController(Context context) {
        super(context);

        mAutofillManager = mContext.getSystemService(AutofillManager.class);
    }

    @Override
    public boolean isAvailable() {
        return mAutofillManager != null
                && mAutofillManager.hasAutofillFeature()
                && mAutofillManager.isAutofillSupported();
    }

    @Override
    public String getPreferenceKey() {
        return "default_autofill_main";
    }

    @Override
    protected Intent getSettingIntent(DefaultAppInfo info) {
        if (info == null) {
            return null;
        }
        final DefaultAutofillPicker.AutofillSettingIntentProvider intentProvider =
                new DefaultAutofillPicker.AutofillSettingIntentProvider(
                        mContext, mUserId, info.getKey());
        return intentProvider.getIntent();
    }

    @Override
    protected DefaultAppInfo getDefaultAppInfo() {
        final String flattenComponent = Settings.Secure.getString(mContext.getContentResolver(),
                DefaultAutofillPicker.SETTING);
        if (!TextUtils.isEmpty(flattenComponent)) {
            DefaultAppInfo appInfo = new DefaultAppInfo(mContext, mPackageManager,
                    mUserId, ComponentName.unflattenFromString(flattenComponent));
            return appInfo;
        }
        return null;
    }
}
