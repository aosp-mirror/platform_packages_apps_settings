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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settingslib.applications.ApplicationsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
public class AppPermissionPreferenceControllerTest {

    @Mock
    private SettingsActivity mActivity;
    @Mock
    private AppInfoDashboardFragment mFragment;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Preference mPreference;
    @Mock
    private PackageManager mPackageManager;

    private Context mContext;
    private AppPermissionPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new AppPermissionPreferenceController(mContext, "permission_settings");
        mController.setPackageName("package1");
        mController.setParentFragment(mFragment);
        ReflectionHelpers.setField(mController, "mPackageManager", mPackageManager);

        when(mScreen.findPreference(any())).thenReturn(mPreference);
        final String key = mController.getPreferenceKey();
        when(mPreference.getKey()).thenReturn(key);
        when(mFragment.getActivity()).thenReturn(mActivity);
    }

    @Test
    public void onStart_shouldAddPermissionsChangeListener() {
        mController.onStart();

        verify(mPackageManager).addOnPermissionsChangeListener(
                any(PackageManager.OnPermissionsChangedListener.class));
    }

    @Test
    public void onStop_shouldRemovePermissionsChangeListener() {
        mController.onStop();

        verify(mPackageManager).removeOnPermissionsChangeListener(
                any(PackageManager.OnPermissionsChangedListener.class));
    }

    @Test
    public void getAvailabilityStatus_isAlwaysAvailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(AppPermissionPreferenceController.AVAILABLE);
    }

    @Test
    public void onPermissionSummaryResult_noRequestedPermission_shouldDisablePreference() {
        mController.displayPreference(mScreen);

        mController.mPermissionCallback.onPermissionSummaryResult(1, 0, 1, new ArrayList<>());

        verify(mPreference).setEnabled(false);
        verify(mPreference).setSummary(mContext.getString(
                R.string.runtime_permissions_summary_no_permissions_requested));
    }

    @Test
    public void onPermissionSummaryResult_noGrantedPermission_shouldSetNoPermissionSummary() {
        mController.displayPreference(mScreen);

        mController.mPermissionCallback.onPermissionSummaryResult(1, 5, 0, new ArrayList<>());

        verify(mPreference).setEnabled(true);
        verify(mPreference).setSummary(mContext.getString(
                R.string.runtime_permissions_summary_no_permissions_granted));
    }

    @Test
    public void onPermissionSummaryResult_hasRuntimePermission_shouldSetPermissionAsSummary() {
        mController.displayPreference(mScreen);
        final String permission = "Storage";
        final ArrayList<CharSequence> labels = new ArrayList<>();
        labels.add(permission);

        mController.mPermissionCallback.onPermissionSummaryResult(1, 5, 0, labels);

        verify(mPreference).setEnabled(true);
        verify(mPreference).setSummary(permission);
    }

    @Test
    public void onPermissionSummaryResult_hasAdditionalPermission_shouldSetAdditionalSummary() {
        mController.displayPreference(mScreen);
        final String permission = "Storage";
        final ArrayList<CharSequence> labels = new ArrayList<>();
        labels.add(permission);

        mController.mPermissionCallback.onPermissionSummaryResult(1, 5, 2, labels);

        verify(mPreference).setEnabled(true);
        verify(mPreference).setSummary("Storage and 2 additional permissions");
    }

    @Test
    public void handlePreferenceTreeClick_shouldStartManagePermissionsActivity() {
        final ApplicationsState.AppEntry appEntry = mock(ApplicationsState.AppEntry.class);
        appEntry.info = new ApplicationInfo();
        when(mFragment.getAppEntry()).thenReturn(appEntry);

        mController.handlePreferenceTreeClick(mPreference);

        verify(mActivity).startActivityForResult(argThat(intent -> intent != null &&
                Intent.ACTION_MANAGE_APP_PERMISSIONS.equals(intent.getAction())), anyInt());
    }
}
