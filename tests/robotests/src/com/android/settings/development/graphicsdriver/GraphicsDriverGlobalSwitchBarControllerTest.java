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

import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableForAllAppsPreferenceController.UPDATABLE_DRIVER_DEFAULT;
import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableForAllAppsPreferenceController.UPDATABLE_DRIVER_OFF;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import com.android.settings.widget.SwitchWidgetController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class GraphicsDriverGlobalSwitchBarControllerTest {

    @Mock
    private SwitchWidgetController mSwitchWidgetController;
    @Mock
    private GraphicsDriverContentObserver mGraphicsDriverContentObserver;

    private Context mContext;
    private ContentResolver mResolver;
    private GraphicsDriverGlobalSwitchBarController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mResolver = mContext.getContentResolver();
    }

    @Test
    public void constructor_updatableDriverOn_shouldCheckSwitchBar() {
        Settings.Global.putInt(
                mResolver, Settings.Global.UPDATABLE_DRIVER_ALL_APPS, UPDATABLE_DRIVER_DEFAULT);
        mController = new GraphicsDriverGlobalSwitchBarController(
                mContext, mSwitchWidgetController);

        verify(mSwitchWidgetController).setChecked(true);
    }

    @Test
    public void constructor_updatableDriverOff_shouldUncheckSwitchBar() {
        Settings.Global.putInt(mResolver, Settings.Global.UPDATABLE_DRIVER_ALL_APPS,
                UPDATABLE_DRIVER_OFF);
        mController = new GraphicsDriverGlobalSwitchBarController(
                mContext, mSwitchWidgetController);

        verify(mSwitchWidgetController).setChecked(false);
    }

    @Test
    public void constructor_developmentSettingsEnabled_shouldEnableSwitchBar() {
        Settings.Global.putInt(mResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
        mController = new GraphicsDriverGlobalSwitchBarController(
                mContext, mSwitchWidgetController);

        verify(mSwitchWidgetController).setEnabled(true);
    }

    @Test
    public void constructor_developmentSettingsDisabled_shouldDisableSwitchBar() {
        Settings.Global.putInt(mResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
        mController = new GraphicsDriverGlobalSwitchBarController(
                mContext, mSwitchWidgetController);

        verify(mSwitchWidgetController).setEnabled(false);
    }

    @Test
    public void onStart_shouldStartListeningAndRegister() {
        mController = new GraphicsDriverGlobalSwitchBarController(
                mContext, mSwitchWidgetController);
        mController.mGraphicsDriverContentObserver = mGraphicsDriverContentObserver;
        mController.onStart();

        verify(mSwitchWidgetController).startListening();
        verify(mGraphicsDriverContentObserver).register(mResolver);
    }

    @Test
    public void onStop_shouldStopListeningAndUnregister() {
        mController = new GraphicsDriverGlobalSwitchBarController(
                mContext, mSwitchWidgetController);
        mController.mGraphicsDriverContentObserver = mGraphicsDriverContentObserver;
        mController.onStop();

        verify(mSwitchWidgetController).stopListening();
        verify(mGraphicsDriverContentObserver).unregister(mResolver);
    }

    @Test
    public void onSwitchToggled_checked_shouldTurnOnUpdatableDriver() {
        Settings.Global.putInt(mResolver, Settings.Global.UPDATABLE_DRIVER_ALL_APPS,
                UPDATABLE_DRIVER_OFF);
        mController = new GraphicsDriverGlobalSwitchBarController(
                mContext, mSwitchWidgetController);
        mController.onSwitchToggled(true);

        assertThat(Settings.Global.getInt(
                           mResolver, Settings.Global.UPDATABLE_DRIVER_ALL_APPS,
                           UPDATABLE_DRIVER_DEFAULT))
                .isEqualTo(UPDATABLE_DRIVER_DEFAULT);
    }

    @Test
    public void onSwitchToggled_unchecked_shouldTurnOffUpdatableDriver() {
        Settings.Global.putInt(
                mResolver, Settings.Global.UPDATABLE_DRIVER_ALL_APPS, UPDATABLE_DRIVER_DEFAULT);
        mController = new GraphicsDriverGlobalSwitchBarController(
                mContext, mSwitchWidgetController);
        mController.onSwitchToggled(false);

        assertThat(Settings.Global.getInt(
                           mResolver, Settings.Global.UPDATABLE_DRIVER_ALL_APPS,
                           UPDATABLE_DRIVER_DEFAULT))
                .isEqualTo(UPDATABLE_DRIVER_OFF);
    }
}
