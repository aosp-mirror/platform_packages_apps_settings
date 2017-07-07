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

package com.android.settings.fuelgauge.anomaly;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AnomalyTest {
    private static int TYPE = Anomaly.AnomalyType.WAKE_LOCK;
    private static int UID = 111;
    private static int SDK_VERSION = Build.VERSION_CODES.L;
    private static long WAKE_LOCK_TIME_MS = 1500;
    private static String PACKAGE_NAME = "com.android.settings";
    private static String DISPLAY_NAME = "settings";

    @Test
    public void testBuilder_buildCorrectly() {
        Anomaly anomaly = new Anomaly.Builder()
                .setType(TYPE)
                .setUid(UID)
                .setWakeLockTimeMs(WAKE_LOCK_TIME_MS)
                .setPackageName(PACKAGE_NAME)
                .setDisplayName(DISPLAY_NAME)
                .setTargetSdkVersion(SDK_VERSION)
                .setBackgroundRestrictionEnabled(true)
                .build();

        assertThat(anomaly.type).isEqualTo(TYPE);
        assertThat(anomaly.uid).isEqualTo(UID);
        assertThat(anomaly.wakelockTimeMs).isEqualTo(WAKE_LOCK_TIME_MS);
        assertThat(anomaly.packageName).isEqualTo(PACKAGE_NAME);
        assertThat(anomaly.displayName).isEqualTo(DISPLAY_NAME);
        assertThat(anomaly.targetSdkVersion).isEqualTo(SDK_VERSION);
        assertThat(anomaly.backgroundRestrictionEnabled).isTrue();
    }
}
