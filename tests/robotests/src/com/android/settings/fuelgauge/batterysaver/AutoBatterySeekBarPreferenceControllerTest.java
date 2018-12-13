/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.fuelgauge.batterysaver;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import androidx.lifecycle.LifecycleOwner;

import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AutoBatterySeekBarPreferenceControllerTest {

    private static final int TRIGGER_LEVEL = 20;
    private static final int INTERVAL = 5;

    private AutoBatterySeekBarPreferenceController mController;
    private Context mContext;
    private SeekBarPreference mPreference;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);

        mContext = RuntimeEnvironment.application;
        mPreference = new SeekBarPreference(mContext);
        mPreference.setMax(100);
        mController = new AutoBatterySeekBarPreferenceController(mContext, mLifecycle);
    }

    @Test
    public void testPreference_lowPowerLevelZero_preferenceInvisible() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, 0);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void testPreference_defaultValue_preferenceNotVisible() {
        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void testPreference_lowPowerLevelNotZero_updatePreference() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, TRIGGER_LEVEL);
        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getTitle()).isEqualTo("20%");
        assertThat(mPreference.getProgress()).isEqualTo(TRIGGER_LEVEL / INTERVAL);
    }

    @Test
    public void testOnPreferenceChange_updateValue() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, 0);

        mController.onPreferenceChange(mPreference, TRIGGER_LEVEL / INTERVAL);

        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, 0)).isEqualTo(TRIGGER_LEVEL);
    }

    @Test
    public void testOnPreferenceChange_changeMax() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL_MAX, 50);

        mController.updateState(mPreference);

        assertThat(mPreference.getMax()).isEqualTo(50 / INTERVAL);
    }

    @Test
    public void testOnPreferenceChange_noChangeMax() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL_MAX, 0);

        mController.updateState(mPreference);

        assertThat(mPreference.getMax()).isEqualTo(100);
    }
}
