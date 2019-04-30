/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.content.Context;

import com.android.settings.core.BasePreferenceController;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.android.settings.backup.UserBackupSettingsActivityTest.ShadowBackupSettingsHelper;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowPrivacySettingsUtils.class, ShadowBackupSettingsHelper.class})
public class BackupInactivePreferenceControllerTest {
    private Context mContext;
    private BackupInactivePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new BackupInactivePreferenceController(mContext,
                PrivacySettingsUtils.BACKUP_INACTIVE);
    }

    @After
    public void tearDown() {
        ShadowPrivacySettingsUtils.reset();
        ShadowBackupSettingsHelper.reset();
    }

    @Test
    public void getAvailabilityStatus_isnotInvisibleKey_backupActive_shouldBeAvailable() {
        ShadowPrivacySettingsUtils.setIsInvisibleKey(false);
        ShadowBackupSettingsHelper.isBackupServiceActive = true;

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_isnotInvisibleKey_backupNotActive_shouldBeUnsearchable() {
        ShadowPrivacySettingsUtils.setIsInvisibleKey(false);
        ShadowBackupSettingsHelper.isBackupServiceActive = false;

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void getAvailabilityStatus_isInvisibleKey_shouldBeDisabledUnsupported() {
        ShadowPrivacySettingsUtils.setIsInvisibleKey(true);
        ShadowBackupSettingsHelper.isBackupServiceActive = true;

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }
}
