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

package com.android.settings.development.gup;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;
import static com.android.settings.testutils.ApplicationTestUtils.buildInfo;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
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

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class GupPreferenceControllerTest {
    private static final int DEFAULT = 0;
    private static final int GUP = 1;
    private static final int SYSTEM = 2;
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

    private Context mContext;
    private PreferenceGroup mGroup;
    private PreferenceManager mPreferenceManager;
    private ContentResolver mResolver;
    private GupPreferenceController mController;
    private CharSequence[] mValueList;
    private String mDialogTitle;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mResolver = mContext.getContentResolver();
        mValueList = mContext.getResources().getStringArray(R.array.gup_app_preference_values);
        mDialogTitle = mContext.getResources().getString(R.string.gup_app_preference_title);
    }

    @Test
    public void getAvailability_developmentSettingsEnabled_available() {
        loadDefaultConfig();
        Settings.Global.putInt(mResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailability_developmentSettingsDisabled_disabledDependentSetting() {
        loadDefaultConfig();
        Settings.Global.putInt(mResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
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
    public void createPreference_configDefault_shouldSetDefaultAttributes() {
        loadDefaultConfig();
        final ListPreference preference =
                mController.createListPreference(TEST_PKG_NAME, TEST_APP_NAME);

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
    public void createPreference_configGup_shouldSetGupAttributes() {
        loadConfig(TEST_PKG_NAME, "");
        final ListPreference preference =
                mController.createListPreference(TEST_PKG_NAME, TEST_APP_NAME);

        assertThat(preference.getKey()).isEqualTo(TEST_PKG_NAME);
        assertThat(preference.getTitle()).isEqualTo(TEST_APP_NAME);
        assertThat(preference.getDialogTitle()).isEqualTo(mDialogTitle);
        assertThat(preference.getEntries()).isEqualTo(mValueList);
        assertThat(preference.getEntryValues()).isEqualTo(mValueList);
        assertThat(preference.getEntry()).isEqualTo(mValueList[GUP]);
        assertThat(preference.getValue()).isEqualTo(mValueList[GUP]);
        assertThat(preference.getSummary()).isEqualTo(mValueList[GUP]);
    }

    @Test
    public void createPreference_configSystem_shouldSetSystemAttributes() {
        loadConfig("", TEST_PKG_NAME);
        final ListPreference preference =
                mController.createListPreference(TEST_PKG_NAME, TEST_APP_NAME);

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
                mController.createListPreference(TEST_PKG_NAME, TEST_APP_NAME);
        mController.onPreferenceChange(preference, mValueList[DEFAULT]);

        assertThat(preference.getEntry()).isEqualTo(mValueList[DEFAULT]);
        assertThat(preference.getValue()).isEqualTo(mValueList[DEFAULT]);
        assertThat(preference.getSummary()).isEqualTo(mValueList[DEFAULT]);
        assertThat(Settings.Global.getString(mResolver, Settings.Global.GUP_DEV_OPT_IN_APPS))
                .isEqualTo("");
        assertThat(Settings.Global.getString(mResolver, Settings.Global.GUP_DEV_OPT_OUT_APPS))
                .isEqualTo("");
    }

    @Test
    public void onPreferenceChange_selectGup_shouldUpdateAttributesAndSettingsGlobal() {
        loadDefaultConfig();
        final ListPreference preference =
                mController.createListPreference(TEST_PKG_NAME, TEST_APP_NAME);
        mController.onPreferenceChange(preference, mValueList[GUP]);

        assertThat(preference.getEntry()).isEqualTo(mValueList[GUP]);
        assertThat(preference.getValue()).isEqualTo(mValueList[GUP]);
        assertThat(preference.getSummary()).isEqualTo(mValueList[GUP]);
        assertThat(Settings.Global.getString(mResolver, Settings.Global.GUP_DEV_OPT_IN_APPS))
                .isEqualTo(TEST_PKG_NAME);
        assertThat(Settings.Global.getString(mResolver, Settings.Global.GUP_DEV_OPT_OUT_APPS))
                .isEqualTo("");
    }

    @Test
    public void onPreferenceChange_selectSystem_shouldUpdateAttributesAndSettingsGlobal() {
        loadDefaultConfig();
        final ListPreference preference =
                mController.createListPreference(TEST_PKG_NAME, TEST_APP_NAME);
        mController.onPreferenceChange(preference, mValueList[SYSTEM]);

        assertThat(preference.getEntry()).isEqualTo(mValueList[SYSTEM]);
        assertThat(preference.getValue()).isEqualTo(mValueList[SYSTEM]);
        assertThat(preference.getSummary()).isEqualTo(mValueList[SYSTEM]);
        assertThat(Settings.Global.getString(mResolver, Settings.Global.GUP_DEV_OPT_IN_APPS))
                .isEqualTo("");
        assertThat(Settings.Global.getString(mResolver, Settings.Global.GUP_DEV_OPT_OUT_APPS))
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

    private void loadDefaultConfig() { loadConfig("", ""); }

    private void loadConfig(String optIn, String optOut) {
        Settings.Global.putString(mResolver, Settings.Global.GUP_DEV_OPT_IN_APPS, optIn);
        Settings.Global.putString(mResolver, Settings.Global.GUP_DEV_OPT_OUT_APPS, optOut);

        mController = new GupPreferenceController(mContext, "testKey");
        mGroup = spy(new PreferenceCategory(mContext));
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        when(mGroup.getPreferenceManager()).thenReturn(preferenceManager);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mGroup);
        mController.displayPreference(mScreen);
    }
}
