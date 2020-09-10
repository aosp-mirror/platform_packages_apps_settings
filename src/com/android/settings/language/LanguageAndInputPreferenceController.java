/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.language;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import com.android.settings.core.BasePreferenceController;

import java.util.List;

public class LanguageAndInputPreferenceController extends BasePreferenceController {

    private PackageManager mPackageManager;
    private InputMethodManager mInputMethodManager;

    public LanguageAndInputPreferenceController(Context context, String key) {
        super(context, key);
        mPackageManager = mContext.getPackageManager();
        mInputMethodManager = mContext.getSystemService(InputMethodManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        final String flattenComponent = Settings.Secure.getString(
                mContext.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        if (!TextUtils.isEmpty(flattenComponent)) {
            final String pkg = ComponentName.unflattenFromString(flattenComponent)
                    .getPackageName();
            final List<InputMethodInfo> imis = mInputMethodManager.getInputMethodList();
            for (InputMethodInfo imi : imis) {
                if (TextUtils.equals(imi.getPackageName(), pkg)) {
                    return imi.loadLabel(mPackageManager);
                }
            }
        }
        return "";
    }
}
