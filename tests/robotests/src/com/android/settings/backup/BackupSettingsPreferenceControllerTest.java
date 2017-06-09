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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {BackupSettingsPreferenceControllerTest.ShadowBackupSettingsHelper.class})
public class BackupSettingsPreferenceControllerTest {
    private static final String BACKUP_SETTINGS = "backup_settings";
    private static final String MANUFACTURER_SETTINGS = "manufacturer_backup";

    private Context mContext;

    @Mock
    private BackupSettingsHelper mBackupHelper;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Preference mBackupPreference;
    @Mock
    private Preference mManufacturerPreference;

    @Mock
    private static Intent mBackupIntent;

    private static String mBackupLabel = "Test Backup Label";
    private static String mBackupSummary = "Test Backup Summary";
    private static String mManufacturerLabel = "Test Manufacturer Label";

    @Mock
    private static Intent mManufacturerIntent;

    private BackupSettingsPreferenceController mController;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application.getApplicationContext());
        mController = new BackupSettingsPreferenceController(mContext);
    }

    @Test
    public void testDisplayPreference() {
        when(mScreen.findPreference(BACKUP_SETTINGS)).thenReturn(mBackupPreference);
        when(mScreen.findPreference(MANUFACTURER_SETTINGS)).thenReturn(mManufacturerPreference);

        mController.displayPreference(mScreen);

        verify(mBackupPreference).setIntent(mBackupIntent);
        verify(mBackupPreference).setTitle(mBackupLabel);
        verify(mBackupPreference).setSummary(mBackupSummary);
        verify(mManufacturerPreference).setIntent(mManufacturerIntent);
        verify(mManufacturerPreference).setTitle(mManufacturerLabel);
    }

    @Test
    public void testIsAvailable_shouldAlwaysReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void getPreferenceKey_shouldReturnNull() {
        assertThat(mController.getPreferenceKey()).isNull();
    }

    @Implements(BackupSettingsHelper.class)
    public static class ShadowBackupSettingsHelper {

        @Implementation
        public Intent getIntentForBackupSettings() {
            return mBackupIntent;
        }

        @Implementation
        public String getLabelForBackupSettings() {
            return mBackupLabel;
        }

        @Implementation
        public String getSummaryForBackupSettings() {
            return mBackupSummary;
        }

        @Implementation
        public Intent getIntentProvidedByManufacturer() {
            return mManufacturerIntent;
        }

        @Implementation
        public String getLabelProvidedByManufacturer() {
            return mManufacturerLabel;
        }
    }
}
