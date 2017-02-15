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

package com.android.settings.applications;

import android.content.pm.ApplicationInfo;

import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public final class InstalledAppDetailsTest {

    @Test
    public void getInstallationStatus_notInstalled_shouldReturnUninstalled() {
        final InstalledAppDetails mAppDetail = new InstalledAppDetails();

        assertThat(mAppDetail.getInstallationStatus(new ApplicationInfo()))
            .isEqualTo(R.string.not_installed);
    }

    @Test
    public void getInstallationStatus_enabled_shouldReturnInstalled() {
        final InstalledAppDetails mAppDetail = new InstalledAppDetails();
        final ApplicationInfo info = new ApplicationInfo();
        info.flags = ApplicationInfo.FLAG_INSTALLED;
        info.enabled = true;

        assertThat(mAppDetail.getInstallationStatus(info)).isEqualTo(R.string.installed);
    }

    @Test
    public void getInstallationStatus_disabled_shouldReturnDisabled() {
        final InstalledAppDetails mAppDetail = new InstalledAppDetails();
        final ApplicationInfo info = new ApplicationInfo();
        info.flags = ApplicationInfo.FLAG_INSTALLED;
        info.enabled = false;

        assertThat(mAppDetail.getInstallationStatus(info)).isEqualTo(R.string.disabled);
    }

}
