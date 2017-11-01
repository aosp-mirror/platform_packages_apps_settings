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

package com.android.settings.core;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.android.settings.core.gateway.SettingsGateway;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertFalse;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class SettingsGatewayTest {

    private static final String TAG = "SettingsGatewayTest";

    @Test
    public void allRestrictedActivityMustBeDefinedInManifest() {
        final Context context = InstrumentationRegistry.getTargetContext();
        final PackageManager packageManager = context.getPackageManager();
        final String packageName = context.getPackageName();
        for (String className : SettingsGateway.SETTINGS_FOR_RESTRICTED) {
            final Intent intent = new Intent();
            intent.setComponent(new ComponentName(packageName, className));
            List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent,
                    PackageManager.MATCH_DISABLED_COMPONENTS);
            Log.d(TAG, packageName + "/" + className + "; resolveInfo size: "
                    + resolveInfos.size());
            assertFalse(className + " is not-defined in manifest", resolveInfos.isEmpty());
        }
    }
}
