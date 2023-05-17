/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.development.qstile;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.KeyguardManager;
import android.content.Context;
import android.hardware.SensorPrivacyManager;

import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class SensorsOffTest {
    @Mock
    private KeyguardManager mKeyguardManager;
    @Mock
    private MetricsFeatureProvider mMetricsFeatureProvider;
    @Mock
    private SensorPrivacyManager mSensorPrivacyManager;

    private Context mContext;
    private DevelopmentTiles.SensorsOff mSensorsOff;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mSensorsOff = new DevelopmentTiles.SensorsOff();
        ReflectionHelpers.setField(mSensorsOff, "mBase", mContext);
        ReflectionHelpers.setField(mSensorsOff, "mKeyguardManager", mKeyguardManager);
        ReflectionHelpers.setField(mSensorsOff, "mMetricsFeatureProvider", mMetricsFeatureProvider);
        ReflectionHelpers.setField(mSensorsOff, "mSensorPrivacyManager", mSensorPrivacyManager);
    }

    @Test
    public void setIsEnabled_trueWithKeyguardLocked_sensorPrivacyNotModified() {
        ReflectionHelpers.setField(mSensorsOff, "mIsEnabled", false);
        doReturn(true).when(mKeyguardManager).isKeyguardLocked();

        mSensorsOff.setIsEnabled(true);

        verify(mSensorPrivacyManager, never()).setAllSensorPrivacy(true);
        assertThat(mSensorsOff.isEnabled()).isFalse();
    }

    @Test
    public void setIsEnabled_trueWithKeyguardUnlocked_sensorPrivacyModified() {
        ReflectionHelpers.setField(mSensorsOff, "mIsEnabled", false);
        doReturn(false).when(mKeyguardManager).isKeyguardLocked();

        mSensorsOff.setIsEnabled(true);

        verify(mSensorPrivacyManager, times(1)).setAllSensorPrivacy(true);
        assertThat(mSensorsOff.isEnabled()).isTrue();
    }

    @Test
    public void setIsEnabled_falseWithKeyguardLocked_sensorPrivacyNotModified() {
        ReflectionHelpers.setField(mSensorsOff, "mIsEnabled", true);
        doReturn(true).when(mKeyguardManager).isKeyguardLocked();

        mSensorsOff.setIsEnabled(false);

        verify(mSensorPrivacyManager, never()).setAllSensorPrivacy(false);
        assertThat(mSensorsOff.isEnabled()).isTrue();
    }

    @Test
    public void setIsEnabled_falseWithKeyguardUnlocked_sensorPrivacyModified() {
        ReflectionHelpers.setField(mSensorsOff, "mIsEnabled", true);
        doReturn(false).when(mKeyguardManager).isKeyguardLocked();

        mSensorsOff.setIsEnabled(false);

        verify(mSensorPrivacyManager, times(1)).setAllSensorPrivacy(false);
        assertThat(mSensorsOff.isEnabled()).isFalse();
    }
}
