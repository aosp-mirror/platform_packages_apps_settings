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
import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableForAllAppsPreferenceController.GAME_DRIVER_DEFAULT;
import static com.android.settings.development.graphicsdriver.GraphicsDriverEnableForAllAppsPreferenceController.GAME_DRIVER_OFF;
import static com.android.settings.testutils.ApplicationTestUtils.buildInfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class GraphicsDriverAppPreferenceControllerTest {

    private static final int DEFAULT = 0;
    private static final int PRERELEASE_DRIVER = 1;
    private static final int GAME_DRIVER = 2;
    private static final int SYSTEM = 3;
    private static final String TEST_APP_NAME = "testApp";
    private static final String TEST_PKG_NAME = "testPkg";

    // Pre-installed Apps in the Mock PackageManager
    private static final String APP_1 = "app1";
    private static final String APP_2 = "app2";
    private static final String APP_3 = "app3";

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private GraphicsDriverContentObserver mGraphicsDriverContentObserver;

    private Context mContext;
    private PreferenceGroup mGroup;
    private PreferenceManager mPreferenceManager;
    private ContentResolver mResolver;
    private GraphicsDriverAppPreferenceController mController;
    private CharSequence[] mValueList;
    private String mDialogTitle;
    private String mPreferencePrereleaseDriver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mResolver = mContext.getContentResolver();
        mValueList =
                mContext.getResources().getStringArray(
                        R.array.graphics_driver_app_preference_values);
        mDialogTitle = mContext.getResources().getString(
                R.string.graphics_driver_app_preference_title);
        mPreferencePrereleaseDriver =
                mContext.getResources().getString(
                        R.string.graphics_driver_app_preference_prerelease_driver);
    }

    @Test
    public void getAvailability_developmentSettingsEnabledAndGameDriverOn_available() {
        loadDefaultConfig();
        Settings.Global.putInt(mResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
        Settings.Global.putInt(
                mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailability_developmentSettingsDisabled_conditionallyUnavailable() {
        loadDefaultConfig();
        Settings.Global.putInt(mResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailability_graphicsDriverOff_conditionallyUnavailable() {
        loadDefaultConfig();
        Settings.Global.putInt(mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_OFF);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void displayPreference_shouldAddTwoPreferencesAndSortAscendingly() {
        mockPackageManager();
        loadDefaultConfig();

        // Only non-system app has preference
        assertThat(mGroup.getPreferenceCount()).isEqualTo(2);
        assertThat(mGroup.getPreference(0).getKey()).isEqualTo(APP_1);
        assertThat(mGroup.getPreference(1).getKey()).isEqualTo(APP_3);
    }

    @Test
    public void onStart_shouldRegister() {
        loadDefaultConfig();
        mController.mGraphicsDriverContentObserver = mGraphicsDriverContentObserver;
        mController.onStart();

        verify(mGraphicsDriverContentObserver).register(mResolver);
    }

    @Test
    public void onStop_shouldUnregister() {
        loadDefaultConfig();
        mController.mGraphicsDriverContentObserver = mGraphicsDriverContentObserver;
        mController.onStop();

        verify(mGraphicsDriverContentObserver).unregister(mResolver);
    }

    @Test
    public void updateState_available_visible() {
        Settings.Global.putInt(mResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
        Settings.Global.putInt(
                mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_DEFAULT);
        loadDefaultConfig();

        assertThat(mGroup.isVisible()).isTrue();
    }

    @Test
    public void updateState_graphicsDriverOff_notVisible() {
        Settings.Global.putInt(mResolver, Settings.Global.GAME_DRIVER_ALL_APPS, GAME_DRIVER_OFF);
        loadDefaultConfig();

        assertThat(mGroup.isVisible()).isFalse();
    }

    @Test
    public void createPreference_configDefault_shouldSetDefaultAttributes() {
        loadDefaultConfig();
        final ListPreference preference =
                mController.createListPreference(mContext, TEST_PKG_NAME, TEST_APP_NAME);

        assertThat(preference.getKey()).isEqualTo(TEST_PKG_NAME);
        assertThat(preference.getTitle()).isEqualTo(TEST_APP_NAME);
        assertThat(preference.getDialogTitle()).isEqualTo(mDialogTitle);
        assertThat(preference.getEntries()).isEqualTo(mValueList);
        assertThat(preference.getEntryValues()).isEqualTo(mValueList);
        assertThat(preference.getEntry()).isEqualTo(mValueList[DEFAULT]);
        assertThat(preference.getValue()).isEqualTo(mValueList[DEFAULT]);
        assertThat(preference.getSummary()).isEqualTo(mValueList[DEFAULT]);
    }

    @Test
    public void createPreference_configGAME_DRIVER_shouldSetGameDriverAttributes() {
        loadConfig(TEST_PKG_NAME, "", "");
        final ListPreference preference =
                mController.createListPreference(mContext, TEST_PKG_NAME, TEST_APP_NAME);

        assertThat(preference.getKey()).isEqualTo(TEST_PKG_NAME);
        assertThat(preference.getTitle()).isEqualTo(TEST_APP_NAME);
        assertThat(preference.getDialogTitle()).isEqualTo(mDialogTitle);
        assertThat(preference.getEntries()).isEqualTo(mValueList);
        assertThat(preference.getEntryValues()).isEqualTo(mValueList);
        assertThat(preference.getEntry()).isEqualTo(mValueList[GAME_DRIVER]);
        assertThat(preference.getValue()).isEqualTo(mValueList[GAME_DRIVER]);
        assertThat(preference.getSummary()).isEqualTo(mValueList[GAME_DRIVER]);
    }

    @Test
    public void createPreference_configPRERELEASE_DRIVER_shouldSetPrereleaseDriverAttributes() {
        loadConfig("", TEST_PKG_NAME, "");
        final ListPreference preference =
                mController.createListPreference(mContext, TEST_PKG_NAME, TEST_APP_NAME);

        assertThat(preference.getKey()).isEqualTo(TEST_PKG_NAME);
        assertThat(preference.getTitle()).isEqualTo(TEST_APP_NAME);
        assertThat(preference.getDialogTitle()).isEqualTo(mDialogTitle);
        assertThat(preference.getEntries()).isEqualTo(mValueList);
        assertThat(preference.getEntryValues()).isEqualTo(mValueList);
        assertThat(preference.getEntry()).isEqualTo(mValueList[PRERELEASE_DRIVER]);
        assertThat(preference.getValue()).isEqualTo(mValueList[PRERELEASE_DRIVER]);
        assertThat(preference.getSummary()).isEqualTo(mPreferencePrereleaseDriver);
    }

    @Test
    public void createPreference_configSystem_shouldSetSystemAttributes() {
        loadConfig("", "", TEST_PKG_NAME);
        final ListPreference preference =
                mController.createListPreference(mContext, TEST_PKG_NAME, TEST_APP_NAME);

        assertThat(preference.getKey()).isEqualTo(TEST_PKG_NAME);
        assertThat(preference.getTitle()).isEqualTo(TEST_APP_NAME);
        assertThat(preference.getDialogTitle()).isEqualTo(mDialogTitle);
        assertThat(preference.getEntries()).isEqualTo(mValueList);
        assertThat(preference.getEntryValues()).isEqualTo(mValueList);
        assertThat(preference.getEntry()).isEqualTo(mValueList[SYSTEM]);
        assertThat(preference.getValue()).isEqualTo(mValueList[SYSTEM]);
        assertThat(preference.getSummary()).isEqualTo(mValueList[SYSTEM]);
    }

    @Test
    public void onPreferenceChange_selectDefault_shouldUpdateAttributesAndSettingsGlobal() {
        loadDefaultConfig();
        final ListPreference preference =
                mController.createListPreference(mContext, TEST_PKG_NAME, TEST_APP_NAME);
        mController.onPreferenceChange(preference, mValueList[DEFAULT]);

        assertThat(preference.getEntry()).isEqualTo(mValueList[DEFAULT]);
        assertThat(preference.getValue()).isEqualTo(mValueList[DEFAULT]);
        assertThat(preference.getSummary()).isEqualTo(mValueList[DEFAULT]);
        assertThat(Settings.Global.getString(mResolver, Settings.Global.GAME_DRIVER_OPT_IN_APPS))
                .isEqualTo("");
        assertThat(Settings.Global.getString(mResolver, Settings.Global.GAME_DRIVER_OPT_OUT_APPS))
                .isEqualTo("");
    }

    @Test
    public void onPreferenceChange_selectPRERELEASE_DRIVER_shouldUpdateAttrAndSettingsGlobal() {
        loadDefaultConfig();
        final ListPreference preference =
                mController.createListPreference(mContext, TEST_PKG_NAME, TEST_APP_NAME);
        mController.onPreferenceChange(preference, mValueList[PRERELEASE_DRIVER]);

        assertThat(preference.getEntry()).isEqualTo(mValueList[PRERELEASE_DRIVER]);
        assertThat(preference.getValue()).isEqualTo(mValueList[PRERELEASE_DRIVER]);
        assertThat(preference.getSummary()).isEqualTo(mValueList[PRERELEASE_DRIVER]);
        assertThat(Settings.Global.getString(mResolver,
                Settings.Global.GAME_DRIVER_PRERELEASE_OPT_IN_APPS))
                .isEqualTo(TEST_PKG_NAME);
        assertThat(Settings.Global.getString(mResolver, Settings.Global.GAME_DRIVER_OPT_OUT_APPS))
                .isEqualTo("");
    }

    @Test
    public void onPreferenceChange_selectGAME_DRIVER_shouldUpdateAttributesAndSettingsGlobal() {
        loadDefaultConfig();
        final ListPreference preference =
                mController.createListPreference(mContext, TEST_PKG_NAME, TEST_APP_NAME);
        mController.onPreferenceChange(preference, mValueList[GAME_DRIVER]);

        assertThat(preference.getEntry()).isEqualTo(mValueList[GAME_DRIVER]);
        assertThat(preference.getValue()).isEqualTo(mValueList[GAME_DRIVER]);
        assertThat(preference.getSummary()).isEqualTo(mValueList[GAME_DRIVER]);
        assertThat(Settings.Global.getString(mResolver, Settings.Global.GAME_DRIVER_OPT_IN_APPS))
                .isEqualTo(TEST_PKG_NAME);
        assertThat(Settings.Global.getString(mResolver, Settings.Global.GAME_DRIVER_OPT_OUT_APPS))
                .isEqualTo("");
    }

    @Test
    public void onPreferenceChange_selectSystem_shouldUpdateAttributesAndSettingsGlobal() {
        loadDefaultConfig();
        final ListPreference preference =
                mController.createListPreference(mContext, TEST_PKG_NAME, TEST_APP_NAME);
        mController.onPreferenceChange(preference, mValueList[SYSTEM]);

        assertThat(preference.getEntry()).isEqualTo(mValueList[SYSTEM]);
        assertThat(preference.getValue()).isEqualTo(mValueList[SYSTEM]);
        assertThat(preference.getSummary()).isEqualTo(mValueList[SYSTEM]);
        assertThat(Settings.Global.getString(mResolver, Settings.Global.GAME_DRIVER_OPT_IN_APPS))
                .isEqualTo("");
        assertThat(Settings.Global.getString(mResolver, Settings.Global.GAME_DRIVER_OPT_OUT_APPS))
                .isEqualTo(TEST_PKG_NAME);
    }

    private void mockPackageManager() {
        final int uid = mContext.getUserId();
        final ApplicationInfo app1 = buildInfo(uid, APP_1, 0 /* flags */, 0 /* targetSdkVersion */);
        final ApplicationInfo app2 =
                buildInfo(uid, APP_2, ApplicationInfo.FLAG_SYSTEM, 0 /* targetSdkVersion */);
        final ApplicationInfo app3 = buildInfo(uid, APP_3, 0 /* flags */, 0 /* targetSdkVersion */);

        when(mPackageManager.getInstalledApplications(0 /* flags */))
                .thenReturn(Arrays.asList(app3, app2, app1));
        when(mPackageManager.getApplicationLabel(app1)).thenReturn(APP_1);
        when(mPackageManager.getApplicationLabel(app2)).thenReturn(APP_2);
        when(mPackageManager.getApplicationLabel(app3)).thenReturn(APP_3);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
    }

    private void loadDefaultConfig() {
        loadConfig("", "", "");
    }

    private void loadConfig(String optIn, String prereleaseOptIn, String optOut) {
        Settings.Global.putString(mResolver, Settings.Global.GAME_DRIVER_OPT_IN_APPS, optIn);
        Settings.Global.putString(
                mResolver, Settings.Global.GAME_DRIVER_PRERELEASE_OPT_IN_APPS, prereleaseOptIn);
        Settings.Global.putString(mResolver, Settings.Global.GAME_DRIVER_OPT_OUT_APPS, optOut);

        mController = new GraphicsDriverAppPreferenceController(mContext, "testKey");
        mController.mEntryList = mContext.getResources().getStringArray(
                R.array.graphics_driver_app_preference_values);
        mGroup = spy(new PreferenceCategory(mContext));
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        when(mGroup.getContext()).thenReturn(mContext);
        when(mGroup.getPreferenceManager()).thenReturn(preferenceManager);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mGroup);
        mController.displayPreference(mScreen);
    }
}
