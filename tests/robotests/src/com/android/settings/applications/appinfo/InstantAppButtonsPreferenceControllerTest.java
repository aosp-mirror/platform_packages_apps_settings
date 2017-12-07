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

package com.android.settings.applications.appinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v7.preference.PreferenceScreen;
import android.view.View;

import com.android.settings.TestConfig;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.applications.instantapps.InstantAppButtonsController;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.instantapps.InstantAppDataProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class InstantAppButtonsPreferenceControllerTest {

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ApplicationInfo mAppInfo;
    @Mock
    private AppInfoDashboardFragment mFragment;

    private Context mContext;
    private InstantAppButtonsPreferenceController mController;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mContext = spy(RuntimeEnvironment.application);
        final PackageInfo packageInfo = mock(PackageInfo.class);
        packageInfo.applicationInfo = mAppInfo;
        when(mFragment.getPackageInfo()).thenReturn(packageInfo);
        mController =
                spy(new InstantAppButtonsPreferenceController(mContext, mFragment, "Package1"));
    }

    @Test
    public void getAvailabilityStatus_notInstantApp_shouldReturnDisabled() {
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
                (InstantAppDataProvider) (i -> false));

        assertThat(mController.getAvailabilityStatus()).isEqualTo(mController.DISABLED_FOR_USER);
    }

    @Test
    public void getAvailabilityStatus_isInstantApp_shouldReturnAvailable() {
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
                (InstantAppDataProvider) (i -> true));

        assertThat(mController.getAvailabilityStatus()).isEqualTo(mController.AVAILABLE);
    }

    @Test
    public void displayPreference_shouldSetPreferenceTitle() {
        final PreferenceScreen screen = mock(PreferenceScreen.class);
        final LayoutPreference preference = mock(LayoutPreference.class);
        when(screen.findPreference(mController.getPreferenceKey())).thenReturn(preference);
        when(mController.getApplicationFeatureProvider())
                .thenReturn(mFeatureFactory.applicationFeatureProvider);
        final InstantAppButtonsController buttonsController =
                mock(InstantAppButtonsController.class);
        when(buttonsController.setPackageName(nullable(String.class)))
                .thenReturn(buttonsController);
        when(mFeatureFactory.applicationFeatureProvider.newInstantAppButtonsController(
                nullable(Fragment.class), nullable(View.class),
                nullable(InstantAppButtonsController.ShowDialogDelegate.class)))
                .thenReturn(buttonsController);

        mController.displayPreference(screen);

        verify(buttonsController).setPackageName(nullable(String.class));
        verify(buttonsController).show();
    }

    @Test
    public void createDialog_shouldReturnDialogFromButtonController() {
        final InstantAppButtonsController buttonsController =
                mock(InstantAppButtonsController.class);
        ReflectionHelpers.setField(
                mController, "mInstantAppButtonsController", buttonsController);
        final AlertDialog mockDialog = mock(AlertDialog.class);
        when(buttonsController.createDialog(InstantAppButtonsController.DLG_CLEAR_APP))
                .thenReturn(mockDialog);

        assertThat(mController.createDialog(InstantAppButtonsController.DLG_CLEAR_APP))
                .isEqualTo(mockDialog);
    }

}
