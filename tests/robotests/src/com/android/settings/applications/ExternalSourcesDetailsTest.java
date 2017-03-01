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

package com.android.settings.applications;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.applications.ApplicationsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ExternalSourcesDetailsTest {

    @Mock
    private Context mContext;
    @Mock
    private AppStateInstallAppsBridge.InstallAppsState mInstallAppsStateAllowed;
    @Mock
    private AppStateInstallAppsBridge.InstallAppsState mInstallAppsStateBlocked;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mInstallAppsStateAllowed.canInstallApps()).thenReturn(true);
        when(mInstallAppsStateBlocked.canInstallApps()).thenReturn(false);
    }

    @Test
    public void testGetPreferenceSummary() {
        ApplicationsState.AppEntry appEntry = mock(ApplicationsState.AppEntry.class);
        appEntry.extraInfo = mInstallAppsStateBlocked;
        ExternalSourcesDetails.getPreferenceSummary(mContext, appEntry);
        verify(mContext).getString(R.string.external_source_untrusted);
        appEntry.extraInfo = mInstallAppsStateAllowed;
        ExternalSourcesDetails.getPreferenceSummary(mContext, appEntry);
        verify(mContext).getString(R.string.external_source_trusted);
    }
}
