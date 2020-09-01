/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static junit.framework.Assert.assertEquals;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AdbAuthorizationTimeoutPreferenceControllerTest {
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private DevelopmentSettingsDashboardFragment mFragment;

    private Context mContext;
    private ContentResolver mContentResolver;
    private SwitchPreference mPreference;
    private AdbAuthorizationTimeoutPreferenceController mPreferenceController;
    private long mInitialAuthTimeout;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mContentResolver = mContext.getContentResolver();

        mPreferenceController = new AdbAuthorizationTimeoutPreferenceController(mContext);
        mPreference = spy(new SwitchPreference(mContext));
        when(mPreferenceScreen.findPreference(mPreferenceController.getPreferenceKey())).thenReturn(
                mPreference);
        mPreferenceController.displayPreference(mPreferenceScreen);

        mInitialAuthTimeout = Settings.Global.getLong(mContext.getContentResolver(),
                Settings.Global.ADB_ALLOWED_CONNECTION_TIME,
                Settings.Global.DEFAULT_ADB_ALLOWED_CONNECTION_TIME);
    }

    @After
    public void tearDown() throws Exception {
        Settings.Global.putLong(mContext.getContentResolver(),
                Settings.Global.ADB_ALLOWED_CONNECTION_TIME, mInitialAuthTimeout);
    }

    @Test
    public void onPreferenceChange_enableSetting_timeoutSetToZero() throws Exception {
        // This developer option disables the automatic adb authorization revocation by setting
        // the timeout value to 0 when enabled.
        mPreferenceController.onPreferenceChange(mPreference, true);
        long authTimeout = Settings.Global.getLong(mContentResolver,
                Settings.Global.ADB_ALLOWED_CONNECTION_TIME);

        assertEquals(0, authTimeout);
    }

    @Test
    public void onPreferenceChange_enableAndDisableSetting_timeoutSetToDefault()
            throws Exception {
        // A non-default setting value is not saved when this developer option is enabled and the
        // setting value is set to 0. If the user subsequently disables the option the setting
        // value is restored to the default value.
        Settings.Global.putLong(mContentResolver, Settings.Global.ADB_ALLOWED_CONNECTION_TIME, 1);

        mPreferenceController.onPreferenceChange(mPreference, true);
        mPreferenceController.onPreferenceChange(mPreference, false);
        long authTimeout = Settings.Global.getLong(mContentResolver,
                Settings.Global.ADB_ALLOWED_CONNECTION_TIME);

        assertEquals(Settings.Global.DEFAULT_ADB_ALLOWED_CONNECTION_TIME, authTimeout);
    }

    @Test
    public void updateState_timeoutSetToZero_preferenceDisplayedEnabled() throws Exception {
        Settings.Global.putLong(mContentResolver, Settings.Global.ADB_ALLOWED_CONNECTION_TIME, 0);

        mPreferenceController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_timeoutSetToDefault_preferenceDisplayedDisabled() throws Exception {
        Settings.Global.putLong(mContentResolver, Settings.Global.ADB_ALLOWED_CONNECTION_TIME,
                Settings.Global.DEFAULT_ADB_ALLOWED_CONNECTION_TIME);

        mPreferenceController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_preferenceAndTimeoutDisabled() throws Exception {
        mPreferenceController.onDeveloperOptionsSwitchDisabled();

        long authTimeout = Settings.Global.getLong(mContentResolver,
                Settings.Global.ADB_ALLOWED_CONNECTION_TIME);

        assertEquals(Settings.Global.DEFAULT_ADB_ALLOWED_CONNECTION_TIME, authTimeout);
        verify(mPreference).setChecked(false);
    }
}

