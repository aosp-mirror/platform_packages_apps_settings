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

package com.android.settings.fuelgauge.batteryusage.db;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link AppUsageEventEntity}. */
@RunWith(RobolectricTestRunner.class)
public final class AppUsageEventEntityTest {
    @Test
    public void testBuilder_returnsExpectedResult() {
        final long uid = 101L;
        final long userId = 1001L;
        final long timestamp = 10001L;
        final int appUsageEventType = 1;
        final String packageName = "com.android.settings1";
        final int instanceId = 100001;
        final String taskRootPackageName = "com.android.settings2";

        AppUsageEventEntity entity =
                AppUsageEventEntity.newBuilder()
                        .setUid(uid)
                        .setUserId(userId)
                        .setTimestamp(timestamp)
                        .setAppUsageEventType(appUsageEventType)
                        .setPackageName(packageName)
                        .setInstanceId(instanceId)
                        .setTaskRootPackageName(taskRootPackageName)
                        .build();

        // Verifies the app relative information.
        assertThat(entity.uid).isEqualTo(uid);
        assertThat(entity.userId).isEqualTo(userId);
        assertThat(entity.timestamp).isEqualTo(timestamp);
        assertThat(entity.appUsageEventType).isEqualTo(appUsageEventType);
        assertThat(entity.packageName).isEqualTo(packageName);
        assertThat(entity.instanceId).isEqualTo(instanceId);
        assertThat(entity.taskRootPackageName).isEqualTo(taskRootPackageName);
    }
}
