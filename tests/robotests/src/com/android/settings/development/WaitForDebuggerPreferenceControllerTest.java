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

import static com.android.settings.development.DevelopmentOptionsActivityRequestCodes
        .REQUEST_CODE_DEBUG_APP;
import static com.android.settings.development.WaitForDebuggerPreferenceController
        .SETTING_VALUE_OFF;
import static com.android.settings.development.WaitForDebuggerPreferenceController.SETTING_VALUE_ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.IActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
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
public class WaitForDebuggerPreferenceControllerTest {

    @Mock
    private SwitchPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private IActivityManager mIActivityManager;

    private Context mContext;
    private ContentResolver mContentResolver;
    private WaitForDebuggerPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mContentResolver = mContext.getContentResolver();
        mController = spy(new WaitForDebuggerPreferenceController(mContext));
        doReturn(mIActivityManager).when(mController).getActivityManagerService();
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
            .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onPreferenceChange_settingEnabledFoobarApp_waitForDebuggerShouldBeOn()
            throws RemoteException {
        final String debugApp = "foobar";
        Settings.Global.putString(mContentResolver, Settings.Global.DEBUG_APP, debugApp);
        mController.onPreferenceChange(mPreference, true /* newValue */);


        verify(mIActivityManager)
            .setDebugApp(debugApp, true /* waitForDebugger */, true /* persistent */);
    }

    @Test
    public void onPreferenceChange_settingDisabledFoobarApp_waitForDebuggerShouldBeOff()
            throws RemoteException {
        final String debugApp = "foobar";
        Settings.Global.putString(mContentResolver, Settings.Global.DEBUG_APP, debugApp);
        mController.onPreferenceChange(mPreference, false /* newValue */);

        verify(mIActivityManager)
            .setDebugApp(debugApp, false /* waitForDebugger */, true /* persistent */);
    }

    @Test
    public void updateState_settingEnabledNullDebugApp_preferenceShouldBeCheckedAndDisabled() {
        final String debugApp = null;
        Settings.Global.putString(mContentResolver, Settings.Global.DEBUG_APP, debugApp);
        Settings.Global
            .putInt(mContentResolver, Settings.Global.WAIT_FOR_DEBUGGER, SETTING_VALUE_ON);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
        verify(mPreference).setEnabled(false);
    }

    @Test
    public void updateState_settingEnabledFoobarApp_preferenceShouldBeCheckedAndDisabled() {
        final String debugApp = "foobar";
        Settings.Global.putString(mContentResolver, Settings.Global.DEBUG_APP, debugApp);
        Settings.Global
            .putInt(mContentResolver, Settings.Global.WAIT_FOR_DEBUGGER, SETTING_VALUE_ON);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
        verify(mPreference).setEnabled(true);
    }

    @Test
    public void updateState_settingDisabledNullDebugApp_preferenceShouldNotBeCheckedAndDisabled() {
        final String debugApp = null;
        Settings.Global.putString(mContentResolver, Settings.Global.DEBUG_APP, debugApp);
        Settings.Global
            .putInt(mContentResolver, Settings.Global.WAIT_FOR_DEBUGGER, SETTING_VALUE_OFF);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
        verify(mPreference).setEnabled(false);
    }

    @Test
    public void updateState_settingDisableFoobarApp_preferenceShouldNotBeCheckedAndEnabled() {
        final String debugApp = "foobar";
        Settings.Global.putString(mContentResolver, Settings.Global.DEBUG_APP, debugApp);
        Settings.Global
            .putInt(mContentResolver, Settings.Global.WAIT_FOR_DEBUGGER, SETTING_VALUE_OFF);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
        verify(mPreference).setEnabled(true);
    }

    @Test
    public void onActivityResult_requestCodeAndSettingEnabled_waitForDebuggerShouldBeChecked() {
        Intent onActivityResultIntent = new Intent(mContext, AppPicker.class);
        Settings.Global
            .putInt(mContentResolver, Settings.Global.WAIT_FOR_DEBUGGER, SETTING_VALUE_ON);
        boolean result = mController
            .onActivityResult(REQUEST_CODE_DEBUG_APP, Activity.RESULT_OK, onActivityResultIntent);

        assertThat(result).isTrue();
        verify(mPreference).setChecked(true);
    }

    @Test
    public void onActivityResult_requestCodeAndSettingDisabled_waitForDebuggerShouldNotBeChecked() {
        Intent onActivityResultIntent = new Intent(mContext, AppPicker.class);
        Settings.Global
            .putInt(mContentResolver, Settings.Global.WAIT_FOR_DEBUGGER, SETTING_VALUE_OFF);
        boolean result = mController
            .onActivityResult(REQUEST_CODE_DEBUG_APP, Activity.RESULT_OK, onActivityResultIntent);

        assertThat(result).isTrue();
        verify(mPreference).setChecked(false);
    }

    @Test
    public void onActivityResult_badRequestCode_shouldReturnFalse() {
        boolean result = mController.onActivityResult(
                -1 /* request code */, -1 /* result code */, null /* intent */);

        assertThat(result).isFalse();
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_shouldDisablePreference() throws RemoteException {
        mController.onDeveloperOptionsSwitchDisabled();

        verify(mIActivityManager).setDebugApp(null /* package name */,
                false /* waitForDebugger */, false /* persistent */);
        verify(mPreference).setChecked(false);
        verify(mPreference).setEnabled(false);
    }
}
