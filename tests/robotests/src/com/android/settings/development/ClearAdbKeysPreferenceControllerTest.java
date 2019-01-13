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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.debug.IAdbManager;
import android.os.RemoteException;
import android.sysprop.AdbProperties;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowUtils.class)
public class ClearAdbKeysPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private SwitchPreference mPreference;
    @Mock
    private IAdbManager mAdbManager;
    @Mock
    private DevelopmentSettingsDashboardFragment mFragment;

    private ClearAdbKeysPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final Context context = RuntimeEnvironment.application;
        mController = spy(new ClearAdbKeysPreferenceController(context, mFragment));
        ReflectionHelpers.setField(mController, "mAdbManager", mAdbManager);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @After
    public void tearDown() {
        ShadowClearAdbKeysWarningDialog.resetDialog();
        ShadowUtils.reset();
    }

    @Test
    public void isAvailable_roAdbSecureEnabled_shouldBeTrue() {
        AdbProperties.secure(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_roAdbSecureDisabled_shouldBeFalse() {
        AdbProperties.secure(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void displayPreference_isNotAdminUser_preferenceShouldBeDisabled() {
        AdbProperties.secure(true);
        doReturn(false).when(mController).isAdminUser();

        mController.displayPreference(mScreen);

        verify(mPreference).setEnabled(false);
    }

    @Test
    @Config(shadows = ShadowClearAdbKeysWarningDialog.class)
    public void handlePreferenceTreeClick_clearAdbKeysPreference_shouldShowWarningDialog() {
        AdbProperties.secure(true);
        doReturn(true).when(mController).isAdminUser();
        mController.displayPreference(mScreen);
        final String preferenceKey = mController.getPreferenceKey();
        when(mPreference.getKey()).thenReturn(preferenceKey);
        final boolean isHandled = mController.handlePreferenceTreeClick(mPreference);

        assertThat(ShadowClearAdbKeysWarningDialog.sIsShowing).isTrue();
        assertThat(isHandled).isTrue();
    }

    @Test
    public void handlePreferenceTreeClick_notClearAdbKeysPreference_shouldReturnFalse() {
        AdbProperties.secure(true);
        doReturn(true).when(mController).isAdminUser();
        mController.displayPreference(mScreen);
        when(mPreference.getKey()).thenReturn("Some random key!!!");
        final boolean isHandled = mController.handlePreferenceTreeClick(mPreference);

        assertThat(isHandled).isFalse();
    }

    @Test
    public void handlePreferenceTreeClick_monkeyUser_shouldReturnFalse() {
        AdbProperties.secure(true);
        doReturn(true).when(mController).isAdminUser();
        ShadowUtils.setIsUserAMonkey(true);
        mController.displayPreference(mScreen);
        final String preferenceKey = mController.getPreferenceKey();
        when(mPreference.getKey()).thenReturn(preferenceKey);

        final boolean isHandled = mController.handlePreferenceTreeClick(mPreference);

        assertThat(isHandled).isFalse();
    }

    @Test
    public void onDeveloperOptionsSwitchEnabled_isAdminUser_shouldEnablePreference() {
        AdbProperties.secure(true);
        doReturn(true).when(mController).isAdminUser();
        mController.displayPreference(mScreen);
        mController.onDeveloperOptionsSwitchEnabled();

        verify(mPreference).setEnabled(true);
    }

    @Test
    public void onDeveloperOptionsSwitchEnabled_isNotAdminUser_shouldNotEnablePreference() {
        AdbProperties.secure(true);
        doReturn(false).when(mController).isAdminUser();
        mController.displayPreference(mScreen);
        mController.onDeveloperOptionsSwitchEnabled();

        verify(mPreference, never()).setEnabled(true);
    }

    @Test
    public void onClearAdbKeysConfirmed_shouldClearKeys() throws RemoteException {
        mController.onClearAdbKeysConfirmed();

        verify(mAdbManager).clearDebuggingKeys();
    }

    @Implements(ClearAdbKeysWarningDialog.class)
    public static class ShadowClearAdbKeysWarningDialog {

        private static boolean sIsShowing;

        @Implementation
        public static void show(Fragment host) {
            sIsShowing = true;
        }

        private static void resetDialog() {
            sIsShowing = false;
        }
    }
}
