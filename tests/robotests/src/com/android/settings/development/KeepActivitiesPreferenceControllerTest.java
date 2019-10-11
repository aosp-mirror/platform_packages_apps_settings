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

package com.android.settings.development;

import static com.android.settings.development.KeepActivitiesPreferenceController.SETTING_VALUE_OFF;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.IActivityManager;
import android.content.Context;
import android.os.RemoteException;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class KeepActivitiesPreferenceControllerTest {

    private static final int SETTING_VALUE_ON = 1;

    @Mock
    private SwitchPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private IActivityManager mActivityManager;

    private Context mContext;
    private KeepActivitiesPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = spy(new KeepActivitiesPreferenceController(mContext));
        doReturn(mActivityManager).when(mController).getActivityManager();
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
            .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onPreferenceChanged_settingEnabled_turnOnDestroyActivities()
            throws RemoteException {
        mController.onPreferenceChange(mPreference, true /* new value */);

        verify(mActivityManager).setAlwaysFinish(true);
    }

    @Test
    public void onPreferenceChanged_settingDisabled_turnOffDestroyActivities()
            throws RemoteException {
        mController.onPreferenceChange(mPreference, false /* new value */);

        verify(mActivityManager).setAlwaysFinish(false);
    }

    @Test
    public void updateState_settingEnabled_preferenceShouldBeChecked() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ALWAYS_FINISH_ACTIVITIES, SETTING_VALUE_ON);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_settingDisabled_preferenceShouldNotBeChecked() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ALWAYS_FINISH_ACTIVITIES, SETTING_VALUE_OFF);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onDeveloperOptionsDisabled_shouldDisablePreference() throws RemoteException {
        mController.onDeveloperOptionsSwitchDisabled();

        verify(mActivityManager).setAlwaysFinish(false);
        verify(mPreference).setEnabled(false);
        verify(mPreference).setChecked(false);
    }
}
