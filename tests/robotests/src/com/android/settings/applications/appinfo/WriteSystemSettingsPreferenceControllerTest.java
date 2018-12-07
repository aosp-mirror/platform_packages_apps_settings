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

import static android.Manifest.permission.SYSTEM_ALERT_WINDOW;
import static android.Manifest.permission.WRITE_SETTINGS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.UserManager;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WriteSystemSettingsPreferenceControllerTest {

    @Mock
    private UserManager mUserManager;
    @Mock
    private AppInfoDashboardFragment mFragment;
    @Mock
    private Preference mPreference;

    private Context mContext;
    private WriteSystemSettingsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        mController = spy(new WriteSystemSettingsPreferenceController(mContext, "test_key"));
        mController.setParentFragment(mFragment);
        final String key = mController.getPreferenceKey();
        when(mPreference.getKey()).thenReturn(key);
    }

    @Test
    public void getAvailabilityStatus_managedProfile_shouldReturnDisabled() {
        when(mUserManager.isManagedProfile()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus())
            .isEqualTo(BasePreferenceController.DISABLED_FOR_USER);
    }

    @Test
    public void getAvailabilityStatus_noPermissionRequested_shouldReturnDisabled() {
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mFragment.getPackageInfo()).thenReturn(new PackageInfo());

        assertThat(mController.getAvailabilityStatus())
            .isEqualTo(BasePreferenceController.DISABLED_FOR_USER);
    }

    @Test
    public void getAvailabilityStatus_noWriteSettingsPermission_shouldReturnDisabled() {
        when(mUserManager.isManagedProfile()).thenReturn(false);
        final PackageInfo info = new PackageInfo();
        info.requestedPermissions = new String[] {SYSTEM_ALERT_WINDOW};
        when(mFragment.getPackageInfo()).thenReturn(info);

        assertThat(mController.getAvailabilityStatus())
            .isEqualTo(BasePreferenceController.DISABLED_FOR_USER);
    }

    @Test
    public void getAvailabilityStatus_hasWriteSettingsPermission_shouldReturnAvailable() {
        when(mUserManager.isManagedProfile()).thenReturn(false);
        final PackageInfo info = new PackageInfo();
        info.requestedPermissions = new String[] {WRITE_SETTINGS};
        when(mFragment.getPackageInfo()).thenReturn(info);

        assertThat(mController.getAvailabilityStatus())
            .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getDetailFragmentClass_shouldReturnWriteSettingsDetails() {
        assertThat(mController.getDetailFragmentClass()).isEqualTo(WriteSettingsDetails.class);
    }

    @Test
    public void updateState_shouldSetSummary() {
        final String summary = "test summary";
        doReturn(summary).when(mController).getSummary();

        mController.updateState(mPreference);

        verify(mPreference).setSummary(summary);
    }
}
