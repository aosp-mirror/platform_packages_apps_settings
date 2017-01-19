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

package com.android.settings.trustagent;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

/** Implementation for {@code SecurityFeatureProvider}. */
public class TrustAgentManagerImpl implements TrustAgentManager {

    private static final String TAG = "TrustAgentFeature";

    @Override
    public boolean shouldProvideTrust(ResolveInfo resolveInfo, PackageManager pm) {
        final String packageName = resolveInfo.serviceInfo.packageName;
        if (pm.checkPermission(PERMISSION_PROVIDE_AGENT, packageName)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Skipping agent because package " + packageName
                    + " does not have permission " + PERMISSION_PROVIDE_AGENT + ".");
            return false;
        }
        return true;
    }
}
