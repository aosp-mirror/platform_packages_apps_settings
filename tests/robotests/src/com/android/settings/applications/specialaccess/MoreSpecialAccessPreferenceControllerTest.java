/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.applications.specialaccess;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class MoreSpecialAccessPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "more";
    private static final String DIFFERENT_PREFERENCE_KEY = "different";

    private static final String PERMISSION_CONTROLLER_PACKAGE_NAME =
            "com.android.permissioncontroller";

    @Mock
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);

        when(mContext.getPackageManager()).thenReturn(mPackageManager);
    }

    @Test
    public void constructor_shouldResolveActivityWithPermissionControllerPackageName() {
        when(mPackageManager.getPermissionControllerPackageName()).thenReturn(
                PERMISSION_CONTROLLER_PACKAGE_NAME);
        new MoreSpecialAccessPreferenceController(mContext, PREFERENCE_KEY);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mPackageManager).resolveActivity(intentCaptor.capture(), anyInt());
        final Intent intent = intentCaptor.getValue();
        assertThat(intent.getPackage()).isEqualTo(PERMISSION_CONTROLLER_PACKAGE_NAME);
    }

    @Test
    public void getAvailabilityStatus_noPermissionController_shouldReturnUnsupportedOnDevice() {
        when(mPackageManager.getPermissionControllerPackageName()).thenReturn(null);
        MoreSpecialAccessPreferenceController preferenceController =
                new MoreSpecialAccessPreferenceController(mContext, PREFERENCE_KEY);

        assertThat(preferenceController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_canNotResolveActivity_shouldReturnUnsupportedOnDevice() {
        when(mPackageManager.getPermissionControllerPackageName()).thenReturn(
                PERMISSION_CONTROLLER_PACKAGE_NAME);
        MoreSpecialAccessPreferenceController preferenceController =
                new MoreSpecialAccessPreferenceController(mContext, PREFERENCE_KEY);

        assertThat(preferenceController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_canResolveActivity_shouldReturnAvailableUnsearchable() {
        when(mPackageManager.getPermissionControllerPackageName()).thenReturn(
                PERMISSION_CONTROLLER_PACKAGE_NAME);
        when(mPackageManager.resolveActivity(any(Intent.class), anyInt())).thenReturn(
                new ResolveInfo());
        MoreSpecialAccessPreferenceController preferenceController =
                new MoreSpecialAccessPreferenceController(mContext, PREFERENCE_KEY);

        assertThat(preferenceController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void handlePreferenceTreeClick_differentKey_shouldReturnFalse() {
        when(mPackageManager.getPermissionControllerPackageName())
                .thenReturn(PERMISSION_CONTROLLER_PACKAGE_NAME);
        MoreSpecialAccessPreferenceController preferenceController =
                new MoreSpecialAccessPreferenceController(mContext, PREFERENCE_KEY);
        Preference preference = mock(Preference.class);
        when(preference.getKey()).thenReturn(DIFFERENT_PREFERENCE_KEY);

        assertThat(preferenceController.handlePreferenceTreeClick(preference)).isFalse();
    }

    @Test
    public void handlePreferenceTreeClick_sameKey_shouldReturnTrue() {
        when(mPackageManager.getPermissionControllerPackageName())
                .thenReturn(PERMISSION_CONTROLLER_PACKAGE_NAME);
        MoreSpecialAccessPreferenceController preferenceController =
                new MoreSpecialAccessPreferenceController(mContext, PREFERENCE_KEY);
        Preference preference = mock(Preference.class);
        when(preference.getKey()).thenReturn(PREFERENCE_KEY);

        assertThat(preferenceController.handlePreferenceTreeClick(preference)).isTrue();
    }

    @Test
    public void handlePreferenceTreeClick_noPermissionController_shouldNotStartActivity() {
        when(mPackageManager.getPermissionControllerPackageName()).thenReturn(null);
        MoreSpecialAccessPreferenceController preferenceController =
                new MoreSpecialAccessPreferenceController(mContext, PREFERENCE_KEY);
        Preference preference = mock(Preference.class);
        when(preference.getKey()).thenReturn(PREFERENCE_KEY);
        preferenceController.handlePreferenceTreeClick(preference);

        verify(mContext, never()).startActivity(any(Intent.class));
    }

    @Test
    public void handlePreferenceTreeClick_canNotResolveActivity_shouldNotStartActivity() {
        when(mPackageManager.getPermissionControllerPackageName()).thenReturn(
                PERMISSION_CONTROLLER_PACKAGE_NAME);
        MoreSpecialAccessPreferenceController preferenceController =
                new MoreSpecialAccessPreferenceController(mContext, PREFERENCE_KEY);
        Preference preference = mock(Preference.class);
        when(preference.getKey()).thenReturn(PREFERENCE_KEY);
        preferenceController.handlePreferenceTreeClick(preference);

        verify(mContext, never()).startActivity(any(Intent.class));
    }

    @Test
    public void handlePreferenceTreeClick_canResolveActivity_shouldStartActivityWithIntent() {
        when(mPackageManager.getPermissionControllerPackageName())
                .thenReturn(PERMISSION_CONTROLLER_PACKAGE_NAME);
        when(mPackageManager.resolveActivity(any(Intent.class), anyInt())).thenReturn(
                new ResolveInfo());
        MoreSpecialAccessPreferenceController preferenceController =
                new MoreSpecialAccessPreferenceController(mContext, PREFERENCE_KEY);
        Preference preference = mock(Preference.class);
        when(preference.getKey()).thenReturn(PREFERENCE_KEY);
        preferenceController.handlePreferenceTreeClick(preference);
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);

        verify(mContext).startActivity(intentCaptor.capture());
        final Intent intent = intentCaptor.getValue();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_MANAGE_SPECIAL_APP_ACCESSES);
        assertThat(intent.getPackage()).isEqualTo(PERMISSION_CONTROLLER_PACKAGE_NAME);
    }
}
