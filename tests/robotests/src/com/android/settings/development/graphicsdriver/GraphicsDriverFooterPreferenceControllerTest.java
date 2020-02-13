/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.settings.development.graphicsdriver;

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableForAllAppsPreferenceController.GAME_DRIVER_ALL_APPS;
import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableForAllAppsPreferenceController.GAME_DRIVER_DEFAULT;
import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableForAllAppsPreferenceController.GAME_DRIVER_OFF;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;

import com.android.settingslib.widget.FooterPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class GraphicsDriverFooterPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private FooterPreference mPreference;
    @Mock
    private GraphicsDriverContentObserver mGraphicsDriverContentObserver;

    private Context mContext;
    private ContentResolver mResolver;
    private GraphicsDriverFooterPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mResolver = mContext.getContentResolver();
        mController = spy(new GraphicsDriverFooterPreferenceController(mContext, "key"));
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @Test
    public void getAvailabilityStatus_gameDriverOff_availableUnsearchable() {
        Settings.Global.putInt(mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_OFF);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void getAvailabilityStatus_gameDriverDefault_conditionallyUnavailable() {
        Settings.Global.putInt(
                mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_gameDriverAllApps_conditionallyUnavailable() {
        Settings.Global.putInt(
                mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_ALL_APPS);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void onStart_shouldRegister() {
        mController.mGraphicsDriverContentObserver = mGraphicsDriverContentObserver;
        mController.onStart();

        verify(mGraphicsDriverContentObserver).register(mResolver);
    }

    @Test
    public void onStop_shouldUnregister() {
        mController.mGraphicsDriverContentObserver = mGraphicsDriverContentObserver;
        mController.onStop();

        verify(mGraphicsDriverContentObserver).unregister(mResolver);
    }

    @Test
    public void updateState_available_visible() {
        when(mController.getAvailabilityStatus()).thenReturn(AVAILABLE_UNSEARCHABLE);
        mController.updateState(mPreference);

        verify(mPreference).setVisible(true);
    }

    @Test
    public void updateState_unavailable_invisible() {
        when(mController.getAvailabilityStatus()).thenReturn(CONDITIONALLY_UNAVAILABLE);
        mController.updateState(mPreference);

        verify(mPreference).setVisible(false);
    }
}
