/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.applications.appinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.applications.ApplicationsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AdvancedAppInfoPreferenceCategoryControllerTest {

    @Mock
    private PreferenceScreen mPreferenceScreen;

    private Preference mPreference;

    private AdvancedAppInfoPreferenceCategoryController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Context context = spy(RuntimeEnvironment.application);
        String preferenceKey = "preference_key";
        mPreference = spy(new Preference(context));
        mController = spy(new AdvancedAppInfoPreferenceCategoryController(context, preferenceKey));
        when(mPreferenceScreen.findPreference(preferenceKey)).thenReturn(mPreference);
    }

    @Test
    public void displayPreference_shouldNotBeEnabledWhenAppIsUninstalledForUser() {
        final ApplicationInfo info = new ApplicationInfo();
        info.flags = 0;
        final ApplicationsState.AppEntry appEntry = mock(ApplicationsState.AppEntry.class);
        appEntry.info = info;
        mController.setAppEntry(appEntry);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreference.isEnabled()).isFalse();
    }
}
