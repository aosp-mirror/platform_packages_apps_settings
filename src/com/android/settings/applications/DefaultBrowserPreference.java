/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.applications;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.UserHandle;
import android.util.AttributeSet;

import com.android.settings.AppListPreference;

import java.util.ArrayList;
import java.util.List;

public class DefaultBrowserPreference extends AppListPreference {

    final private PackageManager mPm;

    public DefaultBrowserPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPm = context.getPackageManager();
        refreshBrowserApps();
    }

    public void refreshBrowserApps() {
        List<String> browsers = resolveBrowserApps();

        setPackageNames(browsers.toArray(new String[browsers.size()]), null);
    }

    private List<String> resolveBrowserApps() {
        List<String> result = new ArrayList<>();

        // Create an Intent that will match ALL Browser Apps
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse("http:"));

        // Resolve that intent and check that the handleAllWebDataURI boolean is set
        List<ResolveInfo> list = mPm.queryIntentActivitiesAsUser(intent, PackageManager.MATCH_ALL,
                UserHandle.myUserId());

        final int count = list.size();
        for (int i=0; i<count; i++) {
            ResolveInfo info = list.get(i);
            if (info.activityInfo == null || result.contains(info.activityInfo.packageName)
                    || !info.handleAllWebDataURI) {
                continue;
            }

            result.add(info.activityInfo.packageName);
        }

        return result;
    }
}
