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

import static android.content.pm.PackageManager.GET_ACTIVITIES;
import static android.content.pm.PackageManager.GET_META_DATA;
import static android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS;

import static com.android.settings.SettingsActivity.META_DATA_KEY_FRAGMENT_CLASS;
import static com.google.common.truth.Truth.assertThat;

import static junit.framework.Assert.fail;

import static org.junit.Assert.assertFalse;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.platform.test.annotations.Presubmit;
import android.text.TextUtils;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.settings.core.gateway.SettingsGateway;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class SettingsGatewayTest {

    private static final String TAG = "SettingsGatewayTest";

    private Context mContext;
    private PackageManager mPackageManager;
    private String mPackageName;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        mPackageManager = mContext.getPackageManager();
        mPackageName = mContext.getPackageName();
    }

    @Test
    @Presubmit
    public void allRestrictedActivityMustBeDefinedInManifest() {
        for (String className : SettingsGateway.SETTINGS_FOR_RESTRICTED) {
            final Intent intent = new Intent();
            intent.setComponent(new ComponentName(mPackageName, className));
            List<ResolveInfo> resolveInfos = mPackageManager.queryIntentActivities(intent,
                    MATCH_DISABLED_COMPONENTS);
            Log.d(TAG, mPackageName + "/" + className + "; resolveInfo size: "
                    + resolveInfos.size());
            assertFalse(className + " is not-defined in manifest", resolveInfos.isEmpty());
        }
    }

    @Test
    @Presubmit
    public void publicFragmentMustAppearInSettingsGateway()
            throws PackageManager.NameNotFoundException {
        final List<String> whitelistedFragment = new ArrayList<>();
        final StringBuilder error = new StringBuilder();

        for (String fragment : SettingsGateway.ENTRY_FRAGMENTS) {
            whitelistedFragment.add(fragment);
        }
        final PackageInfo pi = mPackageManager.getPackageInfo(mPackageName,
                GET_META_DATA | MATCH_DISABLED_COMPONENTS | GET_ACTIVITIES);
        final List<ActivityInfo> activities = Arrays.asList(pi.activities);

        for (ActivityInfo activity : activities) {
            final Bundle metaData = activity.metaData;
            if (metaData == null || !metaData.containsKey(META_DATA_KEY_FRAGMENT_CLASS)) {
                continue;
            }
            final String fragmentName = metaData.getString(META_DATA_KEY_FRAGMENT_CLASS);

            assertThat(fragmentName).isNotNull();
            if (!whitelistedFragment.contains(fragmentName)) {
                error.append("SettingsGateway.ENTRY_FRAGMENTS must contain " + fragmentName
                        + " because this fragment is used in manifest for " + activity.name)
                        .append("\n");
            }
        }
        final String message = error.toString();
        if (!TextUtils.isEmpty(message)) {
            fail(message);
        }
    }
}
