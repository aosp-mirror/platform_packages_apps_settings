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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.backup.BackupManager;
import android.content.Context;
import android.os.RemoteException;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {BackupSettingsActivityPreferenceControllerTest.ShadowBackupManager.class})
public class BackupSettingsActivityPreferenceControllerTest {
    private static final String KEY_BACKUP_SETTINGS = "backup_settings";

    private Context mContext;
    @Mock
    private UserManager mUserManager;

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Preference mBackupPreference;

    private BackupSettingsActivityPreferenceController mController;

    private static boolean mBackupEnabled;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application.getApplicationContext());
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);

        mController = new BackupSettingsActivityPreferenceController(mContext);
    }

    @Test
    public void updateState_backupOn() throws RemoteException {
        mBackupEnabled = true;

        mController.updateState(mBackupPreference);

        verify(mBackupPreference).setSummary(R.string.accessibility_feature_state_on);
    }

    @Test
    public void updateState_backupOff() throws RemoteException {
        mBackupEnabled = false;

        mController.updateState(mBackupPreference);

        verify(mBackupPreference).setSummary(R.string.accessibility_feature_state_off);
    }

    @Test
    public void isAvailable_systemUser() {
        when(mUserManager.isAdminUser()).thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_nonSystemUser() {
        when(mUserManager.isAdminUser()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void getPreferenceKey() {
        assertThat(mController.getPreferenceKey()).isEqualTo(KEY_BACKUP_SETTINGS);
    }

    @Implements(BackupManager.class)
    public static class ShadowBackupManager {

        @Implementation
        public boolean isBackupEnabled() {
            return mBackupEnabled;
        }
    }
}
