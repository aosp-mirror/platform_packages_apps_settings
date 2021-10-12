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
 * limitations under the License
 */

package com.android.settings.backup;

import static com.google.common.truth.Truth.assertThat;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BackupIntentTest {
    private static final String INTENT_PRIVACY_SETTINGS = "android.settings.PRIVACY_SETTINGS";
    private static final String BACKUP_SETTINGS_ACTIVITY =
            "com.android.settings.Settings$PrivacyDashboardActivity";

    private Context mContext;

    @Before
    public void setUp() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        mContext = instrumentation.getTargetContext();
    }

    @Test
    public void testPrivacySettingsIntentResolvesToOnlyOneActivity(){
        PackageManager pm = mContext.getPackageManager();
        Intent intent = new Intent(INTENT_PRIVACY_SETTINGS);
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
        assertThat(activities).isNotNull();
        assertThat(activities.size()).isEqualTo(1);
        assertThat(activities.get(0).activityInfo.getComponentName().getClassName()).
                isEqualTo(BACKUP_SETTINGS_ACTIVITY);
    }
}
