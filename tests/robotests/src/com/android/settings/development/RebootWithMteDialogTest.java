/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.development;

import static junit.framework.Assert.assertEquals;

import android.content.Context;
import android.os.PowerManager;
import android.os.SystemProperties;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPowerManager;
import org.robolectric.shadows.ShadowSystemProperties;

@RunWith(RobolectricTestRunner.class)
public class RebootWithMteDialogTest {
    private Context mContext;
    private ShadowPowerManager mShadowPowerManager;
    private RebootWithMteDialog mDialog;

    @Before
    public void setup() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
        mShadowPowerManager = Shadows.shadowOf(mContext.getSystemService(PowerManager.class));
        mDialog = new RebootWithMteDialog(mContext);
    }

    @Test
    @Config(
            shadows = {
                ShadowSystemProperties.class,
                ShadowPowerManager.class,
            })
    public void onClick_shouldSetPropAndReboot() {
        mDialog.onClick(null, 0);
        assertEquals(SystemProperties.get("arm64.memtag.bootctl"), "memtag-once");
        assertEquals(mShadowPowerManager.getTimesRebooted(), 1);
    }
}
