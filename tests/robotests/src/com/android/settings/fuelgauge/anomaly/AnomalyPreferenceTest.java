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

import android.content.Context;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AnomalyPreferenceTest {
    @Anomaly.AnomalyType
    private static final int ANOMALY_TYPE = Anomaly.AnomalyType.WAKE_LOCK;
    private static final String PACKAGE_NAME = "com.android.app";
    private static final String DISPLAY_NAME = "app";
    private static final int UID = 111;

    private Context mContext;
    private Anomaly mAnomaly;
    private AnomalyPreference mAnomalyPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;

        mAnomaly = new Anomaly.Builder()
                .setType(ANOMALY_TYPE)
                .setPackageName(PACKAGE_NAME)
                .setDisplayName(DISPLAY_NAME)
                .setUid(UID)
                .build();
    }

    @Test
    public void testAnomalyPreference_containsCorrectData() {
        mAnomalyPreference = new AnomalyPreference(mContext, mAnomaly);

        assertThat(mAnomalyPreference.getTitle()).isEqualTo(DISPLAY_NAME);
    }
}
