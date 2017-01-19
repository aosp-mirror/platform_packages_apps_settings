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

package com.android.settings;

import android.app.backup.BackupManager;
import android.app.backup.IBackupManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;




@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {BackupSettingsHelperTest.ShadowBackupManagerStub.class})
public class BackupSettingsHelperTest {

    private BackupSettingsHelper mBackupSettingsHelper;

    @Mock
    private static IBackupManager mBackupManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(mBackupManager.getCurrentTransport()).thenReturn("test_transport");
        mBackupSettingsHelper = new BackupSettingsHelper();
    }

    @Test
    public void testGetIntentFromBackupTransport() throws Exception {
        Intent intent = new Intent();

        when(mBackupManager.getDataManagementIntent(anyString())).thenReturn(intent);

        Intent backupIntent = mBackupSettingsHelper.getIntentForBackupSettings();

        verify(mBackupManager).getDataManagementIntent(anyString());
    }

    @Test
    public void testGetIntentFromBackupTransport_WithIntent() throws Exception {
        Intent intent = mock(Intent.class);

        when(mBackupManager.getDataManagementIntent(anyString())).thenReturn(intent);

        Intent backupIntent = mBackupSettingsHelper.getIntentForBackupSettings();

        assertThat(backupIntent).isEqualTo(intent);
    }

    @Test
    public void testGetIntentFromBackupTransport_WithNullIntent() throws Exception {
        when(mBackupManager.getDataManagementIntent(anyString())).thenReturn(null);

        Intent backupIntent = mBackupSettingsHelper.getIntentForBackupSettings();

        assertThat(backupIntent).isNull();
    }

    @Test
    public void testGetIntentFromBackupTransport_RemoteException() throws Exception {
        when(mBackupManager.getDataManagementIntent(anyString())).thenThrow(new RemoteException());

        Intent backupIntent = mBackupSettingsHelper.getIntentForBackupSettings();

        assertThat(backupIntent).isNull();
    }

    @Test
    public void testGetIntentFromBackupTransport_BackupEnabled() throws Exception {
        Intent intent = new Intent("test_intent");

        when(mBackupManager.getDataManagementIntent(anyString())).thenReturn(intent);
        when(mBackupManager.isBackupServiceActive(anyInt())).thenReturn(true);

        Intent backupIntent = mBackupSettingsHelper.getIntentForBackupSettings();

        assertThat(backupIntent.getExtras().get(BackupManager.EXTRA_BACKUP_SERVICES_AVAILABLE))
                .isEqualTo(true);
    }

    @Test
    public void testGetIntentFromBackupTransport_BackupDisabled() throws Exception {
        Intent intent = new Intent("test_intent");

        when(mBackupManager.getDataManagementIntent(anyString())).thenReturn(intent);
        when(mBackupManager.isBackupServiceActive(anyInt())).thenReturn(false);

        Intent backupIntent = mBackupSettingsHelper.getIntentForBackupSettings();

        assertThat(backupIntent.getExtras().get(BackupManager.EXTRA_BACKUP_SERVICES_AVAILABLE))
                .isEqualTo(false);
    }

    @Test
    public void testGetIntentFromBackupTransport_BackupStatusException() throws Exception {
        Intent intent = new Intent("test_intent");

        when(mBackupManager.getDataManagementIntent(anyString())).thenReturn(intent);
        when(mBackupManager.isBackupServiceActive(anyInt())).thenThrow(new RemoteException());

        Intent backupIntent = mBackupSettingsHelper.getIntentForBackupSettings();

        assertThat(backupIntent.getExtras().get(BackupManager.EXTRA_BACKUP_SERVICES_AVAILABLE))
                .isEqualTo(false);
    }

    @Test
    public void testIsIntentProvidedByTransport_WithNullIntent() throws Exception {
        when(mBackupManager.getDataManagementIntent(anyString())).thenReturn(null);

        PackageManager packageManager = mock(PackageManager.class);

        boolean isIntentProvided = mBackupSettingsHelper.isIntentProvidedByTransport(packageManager);

        assertThat(isIntentProvided).isFalse();
    }

    @Test
    public void testIsIntentProvidedByTransport_WithInvalidIntent() throws Exception {
        Intent intent = mock(Intent.class);

        when(mBackupManager.getDataManagementIntent(anyString())).thenReturn(intent);

        PackageManager packageManager = mock(PackageManager.class);
        when(intent.resolveActivity(packageManager)).thenReturn(null);

        boolean isIntentProvided = mBackupSettingsHelper.isIntentProvidedByTransport(packageManager);

        assertThat(isIntentProvided).isFalse();
    }

    @Test
    public void testIsIntentProvidedByTransport_WithIntent() throws Exception {
        Intent intent = mock(Intent.class);

        when(mBackupManager.getDataManagementIntent(anyString())).thenReturn(intent);

        PackageManager packageManager = mock(PackageManager.class);
        when(intent.resolveActivity(packageManager)).thenReturn(mock(ComponentName.class));

        boolean isIntentProvided = mBackupSettingsHelper.isIntentProvidedByTransport(packageManager);

        assertThat(isIntentProvided).isTrue();
    }

    @Implements(IBackupManager.Stub.class)
    public static class ShadowBackupManagerStub {
        @Implementation
        public static IBackupManager asInterface(IBinder iBinder) {
            return mBackupManager;
        }
    }
}
