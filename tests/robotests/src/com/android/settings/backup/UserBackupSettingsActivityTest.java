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
 * limitations under the License
 */

package com.android.settings.backup;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {UserBackupSettingsActivityTest.ShadowBackupSettingsHelper.class})
public class UserBackupSettingsActivityTest {
    private ActivityController<UserBackupSettingsActivity> mActivityController;
    private UserBackupSettingsActivity mActivity;
    private Application mApplication;
    private ShadowPackageManager mPackageManager;
    private static boolean mIsBackupProvidedByOEM;

    @Mock
    private FragmentManager mFragmentManager;

    @Mock
    private FragmentTransaction mFragmentTransaction;
    @Mock
    private static Intent mIntent;
    @Mock
    private ComponentName mComponent;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mApplication = RuntimeEnvironment.application;
        mActivityController = Robolectric.buildActivity(UserBackupSettingsActivity.class);
        mActivity = mActivityController.get();
        mPackageManager = Shadows.shadowOf(mApplication.getPackageManager());
        when(mIntent.getComponent()).thenReturn(mComponent);
    }

    @After
    public void resetShadows() {
        ShadowBackupSettingsHelper.reset();
    }

    @Test
    public void onCreate_launchActivity() {
        mIsBackupProvidedByOEM = false;

        // Testing the scenario when the activity is disabled
        mApplication.getPackageManager().setComponentEnabledSetting(mComponent,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);

        mActivityController.create();

        // Verify that the component to launch was enabled.
        final int flags = mPackageManager.getComponentEnabledSettingFlags(mComponent);
        assertThat(flags & PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
            .isEqualTo(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);

        // Verify that the intent returned by BackupSettingsHelper.getIntentForBackupSettings()
        // was launched.
        assertThat(Shadows.shadowOf(mApplication).getNextStartedActivity()).isEqualTo(mIntent);
    }

    @Test
    public void onCreate_hasManufacturerIntent() {
        mIsBackupProvidedByOEM = true;

        // Fragments are tested separately, so mock out the manager.
        mActivity.setFragmentManager(mFragmentManager);
        doReturn(mFragmentTransaction).when(mFragmentTransaction).replace(anyInt(),
                any(Fragment.class));
        doReturn(mFragmentTransaction).when(mFragmentManager).beginTransaction();

        mActivityController.create();

        assertThat(Shadows.shadowOf(mApplication).getNextStartedActivity()).isNull();
        verify(mFragmentTransaction).replace(anyInt(), isA(BackupSettingsFragment.class));
    }

    @Test
    public void getNonIndexableKeys_whenBackupServiceActive() {
        ShadowBackupSettingsHelper.isBackupServiceActive = true;

        assertThat(UserBackupSettingsActivity.SEARCH_INDEX_DATA_PROVIDER.getRawDataToIndex(
                mApplication, true)).isNotEmpty();
        assertThat(UserBackupSettingsActivity.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(
                mApplication)).isEmpty();
    }

    @Test
    public void getNonIndexableKeys_whenBackupServiceNotActive() {
        ShadowBackupSettingsHelper.isBackupServiceActive = false;

        assertThat(UserBackupSettingsActivity.SEARCH_INDEX_DATA_PROVIDER.getRawDataToIndex(
            mApplication, true)).isNotEmpty();
        assertThat(UserBackupSettingsActivity.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(
            mApplication)).contains("Backup");
    }

    @Implements(BackupSettingsHelper.class)
    public static class ShadowBackupSettingsHelper {
        static boolean isBackupServiceActive = true;

        @Implementation
        public boolean isBackupServiceActive() {
            return isBackupServiceActive;
        }

        @Implementation
        protected Intent getIntentForBackupSettings() {
            return mIntent;
        }

        @Implementation
        protected boolean isBackupProvidedByManufacturer() {
            return mIsBackupProvidedByOEM;
        }

        @Resetter
        public static void reset() {
            isBackupServiceActive = true;
        }
    }
}
