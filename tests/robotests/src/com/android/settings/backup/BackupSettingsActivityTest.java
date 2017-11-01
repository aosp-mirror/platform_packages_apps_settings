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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.UserHandle;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.search.SearchIndexableRaw;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;
import org.robolectric.res.builder.RobolectricPackageManager;
import org.robolectric.util.ActivityController;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;


@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {BackupSettingsActivityTest.ShadowBackupSettingsHelper.class,
                BackupSettingsActivityTest.ShadowUserHandle.class})
public class BackupSettingsActivityTest {
    private ActivityController<BackupSettingsActivity> mActivityController;
    private BackupSettingsActivity mActivity;
    private Application mApplication;
    private RobolectricPackageManager mPackageManager;
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
        mActivityController = Robolectric.buildActivity(BackupSettingsActivity.class);
        mActivity = mActivityController.get();
        mPackageManager = (RobolectricPackageManager) mApplication.getPackageManager();
        doReturn(mComponent).when(mIntent).getComponent();
    }

    @Test
    public void onCreate_launchActivity() {
        mIsBackupProvidedByOEM = false;

        // Testing the scenario when the activity is disabled
        mPackageManager.setComponentEnabledSetting(mComponent,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);

        mActivityController.create();

        // Verify that the component to launch was enabled.
        assertThat(mPackageManager.getComponentState(mComponent).newState)
                .isEqualTo(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);

        // Verify that the intent returned by BackupSettingsHelper.getIntentForBackupSettings()
        // was launched.
        assertThat(shadowOf(mApplication).getNextStartedActivity()).isEqualTo(mIntent);
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

        assertThat(shadowOf(mApplication).getNextStartedActivity()).isNull();
        verify(mFragmentTransaction).replace(anyInt(), isA(BackupSettingsFragment.class));

    }

    @Test
    public void getNonIndexableKeys_SystemUser() {
        final List<SearchIndexableRaw> indexableRaws =
                BackupSettingsActivity.SEARCH_INDEX_DATA_PROVIDER.getRawDataToIndex(
                        mApplication.getApplicationContext(), true);
        final List<String> nonIndexableKeys =
                BackupSettingsActivity.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(
                        mApplication.getApplicationContext());

        assertThat(indexableRaws).isNotNull();
        assertThat(indexableRaws).isNotEmpty();
        assertThat(nonIndexableKeys).isEmpty();
    }

    @Test
    public void getNonIndexableKeys_NonSystemUser() {
        ShadowUserHandle.setUid(1); // Non-SYSTEM user.

        final List<SearchIndexableRaw> indexableRaws =
                BackupSettingsActivity.SEARCH_INDEX_DATA_PROVIDER.getRawDataToIndex(
                        mApplication.getApplicationContext(), true);
        final List<String> nonIndexableKeys =
                BackupSettingsActivity.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(
                        mApplication.getApplicationContext());

        assertThat(indexableRaws).isNotNull();
        assertThat(indexableRaws).isNotEmpty();
        assertThat(nonIndexableKeys).isNotEmpty();
    }

    @After
    public void resetShadows() {
        ShadowUserHandle.reset();
    }

    @Implements(BackupSettingsHelper.class)
    public static class ShadowBackupSettingsHelper {
        @Implementation
        public Intent getIntentForBackupSettings() {
            return mIntent;
        }

        @Implementation
        public boolean isBackupProvidedByManufacturer() {
            return mIsBackupProvidedByOEM;
        }
    }

    @Implements(UserHandle.class)
    public static class ShadowUserHandle {
        private static int sUid = 0; // SYSTEM by default

        public static void setUid(int uid) {
            sUid = uid;
        }

        @Implementation
        public static int myUserId() {
            return sUid;
        }

        @Resetter
        public static void reset() {
            sUid = 0;
        }
    }
}
