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

package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;

import androidx.preference.SwitchPreference;
import androidx.preference.TwoStatePreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ShowOperatorNamePreferenceControllerTest {

    private static final String KEY_SHOW_OPERATOR_NAME = "show_operator_name";

    @Mock
    private SwitchPreference mPreference;
    @Mock
    private CarrierConfigManager mConfigManager;

    private ShowOperatorNamePreferenceController mController;
    @Mock
    private PersistableBundle mConfig;
    private Context mContext;
    private Resources mResources;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mResources = spy(mContext.getResources());
        when(mContext.getResources()).thenReturn(mResources);

        when(mConfigManager.getConfigForSubId(anyInt())).thenReturn(mConfig);
        when(mContext.getSystemService(CarrierConfigManager.class)).thenReturn(mConfigManager);

        doReturn(mock(DevicePolicyManager.class)).when(mContext)
                .getSystemService(Context.DEVICE_POLICY_SERVICE);
        mController = new ShowOperatorNamePreferenceController(mContext);
    }

    @Test
    public void testIsAvailable_configIsTrue_ReturnTrue() {
        when(mConfig.getBoolean(CarrierConfigManager
                .KEY_SHOW_OPERATOR_NAME_IN_STATUSBAR_BOOL, false)).thenReturn(true);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testIsAvailable_configIsFalse_ReturnFalse() {
        when(mConfig.getBoolean(CarrierConfigManager
                .KEY_SHOW_OPERATOR_NAME_IN_STATUSBAR_BOOL, false)).thenReturn(false);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testOnPreferenceChange_TurnOn_ReturnOn() {
        mController.onPreferenceChange(mPreference, true);

        final int mode =
            Settings.Secure.getInt(mContext.getContentResolver(), KEY_SHOW_OPERATOR_NAME, 0);
        assertThat(mode).isEqualTo(1);
    }

    @Test
    public void testOnPreferenceChange_TurnOff_ReturnOff() {
        mController.onPreferenceChange(mPreference, false);

        final int mode =
            Settings.Secure.getInt(mContext.getContentResolver(), KEY_SHOW_OPERATOR_NAME, 1);
        assertThat(mode).isEqualTo(0);
    }

    @Test
    public void testUpdateState_DefaultValueEnabled() {
        when(mResources.getInteger(com.android.internal.R.integer.config_showOperatorNameDefault))
                .thenReturn(1);

        TwoStatePreference testPreference = mock(TwoStatePreference.class);
        mController.updateState(testPreference);
        verify(testPreference).setChecked(true);
    }

    @Test
    public void testUpdateState_DefaultValueDisabled() {
        when(mResources.getInteger(com.android.internal.R.integer.config_showOperatorNameDefault))
                .thenReturn(0);

        TwoStatePreference testPreference = mock(TwoStatePreference.class);
        mController.updateState(testPreference);
        verify(testPreference).setChecked(false);
    }
}
