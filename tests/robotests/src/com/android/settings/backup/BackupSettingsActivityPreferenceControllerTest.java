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

import static org.mockito.Mockito.verify;

import android.app.backup.BackupManager;
import android.content.Context;
import android.os.UserManager;

import androidx.preference.Preference;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowUserManager;

@RunWith(RobolectricTestRunner.class)
public class BackupSettingsActivityPreferenceControllerTest {

    private static final String KEY_BACKUP_SETTINGS = "backup_settings";

    private Context mContext;
    private BackupManager mBackupManager;
    private UserManager mUserManager;

    @Mock
    private Preference mBackupPreference;

    private BackupSettingsActivityPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mBackupManager = new BackupManager(mContext);

        final ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.grantPermissions(android.Manifest.permission.BACKUP);
        shadowApplication.setSystemService(Context.BACKUP_SERVICE, mBackupManager);

        mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);

        mController = new BackupSettingsActivityPreferenceController(mContext, KEY_BACKUP_SETTINGS);
    }

    @Test
    public void updateState_backupOn() {
        mBackupManager.setBackupEnabled(true);

        mController.updateState(mBackupPreference);
        String summaryString = mContext.getString(R.string.backup_summary_state_on);
        verify(mBackupPreference).setSummary(summaryString);
    }

    @Test
    public void updateState_backupOff() {
        mBackupManager.setBackupEnabled(false);

        mController.updateState(mBackupPreference);
        String summaryString = mContext.getString(R.string.backup_summary_state_off);
        verify(mBackupPreference).setSummary(summaryString);
    }

    @Test
    public void isAvailable_systemUser() {
        final ShadowUserManager sum = Shadow.extract(mUserManager);
        sum.setIsAdminUser(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_nonSystemUser() {
        final ShadowUserManager sum = Shadow.extract(mUserManager);
        sum.setIsAdminUser(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void getPreferenceKey() {
        assertThat(mController.getPreferenceKey()).isEqualTo(KEY_BACKUP_SETTINGS);
    }
}
