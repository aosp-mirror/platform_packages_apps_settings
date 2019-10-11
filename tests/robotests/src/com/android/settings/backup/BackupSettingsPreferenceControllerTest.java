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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = BackupSettingsPreferenceControllerTest.ShadowBackupSettingsHelper.class)
public class BackupSettingsPreferenceControllerTest {

    private static final String BACKUP_SETTINGS = "backup_settings";
    private static final String MANUFACTURER_SETTINGS = "manufacturer_backup";

    private static final String sBackupLabel = "Test Backup Label";
    private static final String sBackupSummary = "Test Backup Summary";
    private static final String sManufacturerLabel = "Test Manufacturer Label";

    @Mock
    private static Intent sBackupIntent;

    @Mock
    private static Intent sManufacturerIntent;

    private Context mContext;

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Preference mBackupPreference;
    @Mock
    private Preference mManufacturerPreference;

    private BackupSettingsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mController = new BackupSettingsPreferenceController(mContext);
    }

    @Test
    public void testDisplayPreference() {
        when(mScreen.findPreference(BACKUP_SETTINGS)).thenReturn(mBackupPreference);
        when(mScreen.findPreference(MANUFACTURER_SETTINGS)).thenReturn(mManufacturerPreference);

        mController.displayPreference(mScreen);

        verify(mBackupPreference).setIntent(sBackupIntent);
        verify(mBackupPreference).setTitle(sBackupLabel);
        verify(mBackupPreference).setSummary(sBackupSummary);
        verify(mManufacturerPreference).setIntent(sManufacturerIntent);
        verify(mManufacturerPreference).setTitle(sManufacturerLabel);
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
            return sBackupIntent;
        }

        @Implementation
        public String getLabelForBackupSettings() {
            return sBackupLabel;
        }

        @Implementation
        public String getSummaryForBackupSettings() {
            return sBackupSummary;
        }

        @Implementation
        public Intent getIntentProvidedByManufacturer() {
            return sManufacturerIntent;
        }

        @Implementation
        public String getLabelProvidedByManufacturer() {
            return sManufacturerLabel;
        }
    }
}
