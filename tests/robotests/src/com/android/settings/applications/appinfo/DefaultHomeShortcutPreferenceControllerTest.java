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

package com.android.settings.applications.appinfo;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import com.android.settings.applications.defaultapps.DefaultHomePreferenceController;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

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
public class DefaultHomeShortcutPreferenceControllerTest {

    @Mock
    private PackageManager mPackageManager;

    private Context mContext;
    private DefaultHomeShortcutPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        mController = new DefaultHomeShortcutPreferenceController(mContext, "Package1");
    }

    @Test
    public void getPreferenceKey_shouldReturnDefaultHome() {
        assertThat(mController.getPreferenceKey()).isEqualTo("default_home");
    }

    @Test
    @Config(shadows = ShadowDefaultHomePreferenceController.class)
    public void hasAppCapability_hasHomeCapability_shouldReturnTrue() {
        assertThat(mController.hasAppCapability()).isTrue();
    }

    @Test
    public void hasAppCapability_noHomeCapability_shouldReturnFalse() {
        assertThat(mController.hasAppCapability()).isFalse();
    }

    @Test
    public void isDefaultApp_isDefaultHome_shouldReturnTrue() {
        when(mPackageManager.getHomeActivities(anyList()))
                .thenReturn(new ComponentName("Package1", "cls1"));
        assertThat(mController.isDefaultApp()).isTrue();
    }

    @Test
    public void isDefaultApp_notDefaultHome_shouldReturnFalse() {
        when(mPackageManager.getHomeActivities(anyList()))
                .thenReturn(new ComponentName("pkg2", "cls1"));
        assertThat(mController.isDefaultApp()).isFalse();
    }

    @Implements(DefaultHomePreferenceController.class)
    public static class ShadowDefaultHomePreferenceController {
        @Implementation
        public static boolean hasHomePreference(String pkg, Context context) {
            return true;
        }
    }
}
