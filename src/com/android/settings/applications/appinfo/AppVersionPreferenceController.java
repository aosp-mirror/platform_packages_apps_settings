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

package com.android.settings.applications.appinfo;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.text.BidiFormatter;

import com.android.settings.R;

public class AppVersionPreferenceController extends AppInfoPreferenceControllerBase {

    public AppVersionPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public CharSequence getSummary() {
        // TODO(b/168333280): Review the null case in detail since this is just a quick
        // workaround to fix NPE.
        final PackageInfo packageInfo = mParent.getPackageInfo();
        if (packageInfo == null) {
            return null;
        }
        return mContext.getString(R.string.version_text,
                BidiFormatter.getInstance().unicodeWrap(packageInfo.versionName));
    }
}
