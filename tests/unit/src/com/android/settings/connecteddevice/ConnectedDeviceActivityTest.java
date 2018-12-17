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

package com.android.settings.connecteddevice;

import static com.google.common.truth.Truth.assertThat;

import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ConnectedDeviceActivityTest {
    private static final String INTENT_ACTION = "android.intent.action.MAIN";
    private static final String CONNECTED_DEVICE_TITLE = "Connected devices";

    private Instrumentation mInstrumentation;

    @Before
    public void setUp() throws Exception {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
    }

    @Test
    public void queryConnectedDeviceActivity_onlyOneResponse() {
        final PackageManager packageManager = mInstrumentation.getContext().getPackageManager();
        final Intent intent = new Intent(INTENT_ACTION);

        int count = 0;
        final List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(intent,
                PackageManager.GET_META_DATA);
        for (ResolveInfo info : resolveInfoList) {
            if (TextUtils.equals(info.activityInfo.loadLabel(packageManager).toString(),
                    CONNECTED_DEVICE_TITLE)) {
                count++;
            }
        }

        assertThat(count).isEqualTo(1);
    }

}
