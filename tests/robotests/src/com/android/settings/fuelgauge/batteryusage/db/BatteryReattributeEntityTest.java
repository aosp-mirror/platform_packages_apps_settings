/*
 * Copyright (C) 2024 The Android Open Source Project
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

import com.android.settings.fuelgauge.batteryusage.BatteryReattribute;
import com.android.settings.fuelgauge.batteryusage.ConvertUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link BatteryReattributeEntity}. */
@RunWith(RobolectricTestRunner.class)
public final class BatteryReattributeEntityTest {

    @Test
    public void constructor_createExpectedData() {
        final BatteryReattribute batteryReattribute =
                BatteryReattribute.newBuilder()
                        .setTimestampStart(100L)
                        .setTimestampEnd(200L)
                        .putReattributeData(1001, 0.2f)
                        .putReattributeData(2001, 0.8f)
                        .build();

        final BatteryReattributeEntity batteryReattributeEntity =
            new BatteryReattributeEntity(batteryReattribute);

        assertThat(batteryReattributeEntity.timestampStart)
            .isEqualTo(batteryReattribute.getTimestampStart());
        assertThat(batteryReattributeEntity.timestampEnd)
            .isEqualTo(batteryReattribute.getTimestampEnd());
        // Verify the BatteryReattribute data.
        final BatteryReattribute decodeResult =
            ConvertUtils.decodeBatteryReattribute(batteryReattributeEntity.reattributeData);
        assertThat(decodeResult).isEqualTo(batteryReattribute);
    }
}
