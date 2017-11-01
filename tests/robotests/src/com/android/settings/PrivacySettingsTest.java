/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings;

import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class PrivacySettingsTest {

    @Mock
    private PreferenceScreen mScreen;
    private PrivacySettings mSettings;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mScreen.findPreference(PrivacySettings.BACKUP_DATA))
                .thenReturn(mock(Preference.class));
        when(mScreen.findPreference(PrivacySettings.CONFIGURE_ACCOUNT))
                .thenReturn(mock(Preference.class));
        when(mScreen.findPreference(PrivacySettings.DATA_MANAGEMENT))
                .thenReturn(mock(Preference.class));
        when(mScreen.findPreference(PrivacySettings.AUTO_RESTORE))
                .thenReturn(mock(SwitchPreference.class));
        mSettings = new PrivacySettings();
    }

    @Test
    public void testSetPreference_noCrash() {
        mSettings.setPreferenceReferences(mScreen);
    }

}
