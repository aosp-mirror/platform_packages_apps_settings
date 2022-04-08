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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableForAllAppsPreferenceController.GAME_DRIVER_ALL_APPS;
import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableForAllAppsPreferenceController.GAME_DRIVER_DEFAULT;
import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableForAllAppsPreferenceController.GAME_DRIVER_OFF;
import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableForAllAppsPreferenceController.GAME_DRIVER_PRERELEASE_ALL_APPS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class GraphicsDriverEnableForAllAppsPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private ListPreference mPreference;
    @Mock
    private GraphicsDriverContentObserver mGraphicsDriverContentObserver;

    private Context mContext;
    private ContentResolver mResolver;
    private GraphicsDriverEnableForAllAppsPreferenceController mController;
    private String mPreferenceDefault;
    private String mPreferenceGameDriver;
    private String mPreferencePrereleaseDriver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mResolver = mContext.getContentResolver();

        final Resources resources = mContext.getResources();
        mPreferenceDefault = resources.getString(R.string.graphics_driver_app_preference_default);
        mPreferenceGameDriver =
                resources.getString(R.string.graphics_driver_app_preference_game_driver);
        mPreferencePrereleaseDriver =
                resources.getString(R.string.graphics_driver_app_preference_prerelease_driver);

        Settings.Global.putInt(mResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
        Settings.Global.putInt(
                mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT);

        mController = new GraphicsDriverEnableForAllAppsPreferenceController(mContext, "testKey");
        mController.mEntryList = mContext.getResources().getStringArray(
                R.array.graphics_driver_all_apps_preference_values);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void getAvailability_developmentSettingsEnabledAndGameDriverSettingsOn_available() {
        Settings.Global.putInt(
                mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailability_developmentSettingsDisabled_conditionallyUnavailable() {
        Settings.Global.putInt(mResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailability_gameDriverOff_conditionallyUnavailable() {
        Settings.Global.putInt(mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_OFF);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void displayPreference_shouldAddListPreference() {
        Settings.Global.putInt(
                mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT);
        mController.updateState(mPreference);

        verify(mPreference).setValue(mPreferenceDefault);
        verify(mPreference).setSummary(mPreferenceDefault);
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
    public void updateState_availableAndDefault_visibleAndDefault() {
        Settings.Global.putInt(
                mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT);
        mController.updateState(mPreference);

        verify(mPreference, atLeastOnce()).setVisible(true);
        verify(mPreference).setValue(mPreferenceDefault);
        verify(mPreference).setSummary(mPreferenceDefault);
    }

    @Test
    public void updateState_availableAndGameDriver_visibleAndGameDriver() {
        Settings.Global.putInt(
                mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_ALL_APPS);
        mController.updateState(mPreference);

        verify(mPreference, atLeastOnce()).setVisible(true);
        verify(mPreference).setValue(mPreferenceGameDriver);
        verify(mPreference).setSummary(mPreferenceGameDriver);
    }

    @Test
    public void updateState_availableAndPrereleaseDriver_visibleAndPrereleaseDriver() {
        Settings.Global.putInt(
                mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_PRERELEASE_ALL_APPS);
        mController.updateState(mPreference);

        verify(mPreference, atLeastOnce()).setVisible(true);
        verify(mPreference).setValue(mPreferencePrereleaseDriver);
        verify(mPreference).setSummary(mPreferencePrereleaseDriver);
    }

    @Test
    public void updateState_gameDriverOff_notVisibleAndSystemDriver() {
        Settings.Global.putInt(mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_OFF);
        mController.updateState(mPreference);

        verify(mPreference).setVisible(false);
        verify(mPreference).setValue(mPreferenceDefault);
        verify(mPreference).setSummary(mPreferenceDefault);
    }

    @Test
    public void onPreferenceChange_default_shouldUpdateSettingsGlobal() {
        Settings.Global.putInt(
                mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_ALL_APPS);
        mController.onPreferenceChange(mPreference, mPreferenceDefault);

        assertThat(Settings.Global.getInt(
                mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT))
                .isEqualTo(GAME_DRIVER_DEFAULT);
    }

    @Test
    public void onPreferenceChange_gameDriver_shouldUpdateSettingsGlobal() {
        Settings.Global.putInt(
                mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT);
        mController.onPreferenceChange(mPreference, mPreferenceGameDriver);

        assertThat(Settings.Global.getInt(
                mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT))
                .isEqualTo(GAME_DRIVER_ALL_APPS);
    }

    @Test
    public void onPreferenceChange_prereleaseDriver_shouldUpdateSettingsGlobal() {
        Settings.Global.putInt(
                mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT);
        mController.onPreferenceChange(mPreference, mPreferencePrereleaseDriver);

        assertThat(Settings.Global.getInt(
                mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT))
                .isEqualTo(GAME_DRIVER_PRERELEASE_ALL_APPS);
    }
}
