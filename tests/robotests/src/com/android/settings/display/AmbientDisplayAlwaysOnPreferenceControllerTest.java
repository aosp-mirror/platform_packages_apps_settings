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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.search.InlinePayload;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowSecureSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = ShadowSecureSettings.class)
public class AmbientDisplayAlwaysOnPreferenceControllerTest {

    @Mock
    private AmbientDisplayConfiguration mConfig;
    @Mock
    private SwitchPreference mSwitchPreference;

    private Context mContext;

    private ContentResolver mContentResolver;

    private AmbientDisplayAlwaysOnPreferenceController mController;
    private boolean mCallbackInvoked;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mContentResolver = mContext.getContentResolver();
        mController = new AmbientDisplayAlwaysOnPreferenceController(mContext, mConfig,
                () -> {
                    mCallbackInvoked = true;
                });
    }

    @Test
    public void updateState_enabled() {
        when(mConfig.alwaysOnEnabled(anyInt())).thenReturn(true);

        mController.updateState(mSwitchPreference);

        verify(mSwitchPreference).setChecked(true);
    }

    @Test
    public void updateState_disabled() {
        when(mConfig.alwaysOnEnabled(anyInt())).thenReturn(false);

        mController.updateState(mSwitchPreference);

        verify(mSwitchPreference).setChecked(false);
    }

    @Test
    public void onPreferenceChange_callback() {
        assertThat(mCallbackInvoked).isFalse();
        mController.onPreferenceChange(mSwitchPreference, true);
        assertThat(mCallbackInvoked).isTrue();
    }

    @Test
    public void onPreferenceChange_enable() {
        mController.onPreferenceChange(mSwitchPreference, true);

        assertThat(Settings.Secure.getInt(mContentResolver, Settings.Secure.DOZE_ALWAYS_ON, -1))
            .isEqualTo(1);
    }

    @Test
    public void onPreferenceChange_disable() {
        mController.onPreferenceChange(mSwitchPreference, false);

        assertThat(Settings.Secure.getInt(mContentResolver, Settings.Secure.DOZE_ALWAYS_ON, -1))
            .isEqualTo(0);
    }

    @Test
    public void isAvailable_available() {
        mController = spy(mController);
        doReturn(true).when(mController).alwaysOnAvailableForUser(any());

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_unavailable() {
        mController = spy(mController);
        doReturn(false).when(mController).alwaysOnAvailableForUser(any());

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testPreferenceController_ProperResultPayloadType() {
        mController = spy(mController);
        doReturn(false).when(mController).alwaysOnAvailableForUser(any());

        assertThat(mController.getResultPayload()).isInstanceOf(InlineSwitchPayload.class);
    }

    @Test
    public void testSetValue_updatesCorrectly() {
        mController = spy(mController);
        doReturn(false).when(mController).alwaysOnAvailableForUser(any());
        final int newValue = 1;
        Settings.Secure.putInt(mContentResolver, Settings.Secure.DOZE_ALWAYS_ON, 0 /* value */);

        ((InlinePayload) mController.getResultPayload()).setValue(mContext, newValue);
        final int updatedValue = Settings.Secure.
            getInt(mContentResolver, Settings.Secure.DOZE_ALWAYS_ON, 1 /* default */);

        assertThat(updatedValue).isEqualTo(newValue);
    }

    @Test
    public void testGetValue_correctValueReturned() {
        mController = spy(mController);
        doReturn(false).when(mController).alwaysOnAvailableForUser(any());
        final int currentValue = 1;
        Settings.Secure.putInt(mContentResolver, Settings.Secure.DOZE_ALWAYS_ON, currentValue);

        final int newValue = ((InlinePayload) mController.getResultPayload()).getValue(mContext);

        assertThat(newValue).isEqualTo(currentValue);
    }
}
