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
 *
 */

package com.android.settings.fuelgauge.batterysaver;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.os.PowerManager;
import android.support.v7.preference.PreferenceScreen;
import android.view.View;
import android.widget.Button;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.widget.TwoStateButtonPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPowerManager;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = ShadowPowerManager.class)
public class BatterySaverButtonPreferenceControllerTest {

    private BatterySaverButtonPreferenceController mController;
    private Context mContext;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private Button mButtonOn;
    private Button mButtonOff;
    private PowerManager mPowerManager;
    @Mock
    private TwoStateButtonPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mContext = spy(RuntimeEnvironment.application);
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        doReturn(mPreference).when(mPreferenceScreen).findPreference(anyString());

        mButtonOn = new Button(mContext);
        mButtonOn.setId(R.id.state_on_button);
        doReturn(mButtonOn).when(mPreference).getStateOnButton();
        mButtonOff = new Button(mContext);
        mButtonOff.setId(R.id.state_off_button);
        doReturn(mButtonOff).when(mPreference).getStateOffButton();

        mController = new BatterySaverButtonPreferenceController(mContext, mLifecycle);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void testUpdateState_lowPowerOn_displayButtonOff() {
        mPowerManager.setPowerSaveMode(true);

        mController.updateState(mPreference);

        assertThat(mButtonOn.getVisibility()).isEqualTo(View.GONE);
        assertThat(mButtonOff.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void testUpdateState_lowPowerOff_displayButtonOn() {
        mPowerManager.setPowerSaveMode(false);

        mController.updateState(mPreference);

        assertThat(mButtonOn.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(mButtonOff.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testOnClick_clickButtonOn_setPowerSaveMode() {
        mController.onClick(mButtonOn);

        assertThat(mPowerManager.isPowerSaveMode()).isTrue();
    }

    @Test
    public void testOnClick_clickButtonOff_clearPowerSaveMode() {
        mController.onClick(mButtonOff);

        assertThat(mPowerManager.isPowerSaveMode()).isFalse();
    }
}
