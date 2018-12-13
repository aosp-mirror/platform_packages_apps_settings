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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;

import com.android.settings.applications.defaultapps.DefaultPhonePreferenceController;

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
public class DefaultPhoneShortcutPreferenceControllerTest {

    @Mock
    private PackageManager mPackageManager;

    private Context mContext;
    private DefaultPhoneShortcutPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        mController = new DefaultPhoneShortcutPreferenceController(mContext, "Package1");
    }

    @Test
    public void getPreferenceKey_shouldReturnDefaultPhone() {
        assertThat(mController.getPreferenceKey()).isEqualTo("default_phone_app");
    }

    @Test
    @Config(shadows = ShadowDefaultPhonePreferenceController.class)
    public void hasAppCapability_hasPhoneCapability_shouldReturnTrue() {
        assertThat(mController.hasAppCapability()).isTrue();
    }

    @Test
    public void hasAppCapability_noPhoneCapability_shouldReturnFalse() {
        assertThat(mController.hasAppCapability()).isFalse();
    }

    @Test
    @Config(shadows = ShadowDefaultPhonePreferenceController.class)
    public void isDefaultApp_isDefaultPhone_shouldReturnTrue() {
        assertThat(mController.isDefaultApp()).isTrue();
    }

    @Test
    public void isDefaultApp_notDefaultPhone_shouldReturnFalse() {
        assertThat(mController.isDefaultApp()).isFalse();
    }

    @Implements(DefaultPhonePreferenceController.class)
    public static class ShadowDefaultPhonePreferenceController {
        @Implementation
        protected static boolean hasPhonePreference(String pkg, Context context) {
            return true;
        }

        @Implementation
        protected static boolean isPhoneDefault(String pkg, Context context) {
            return true;
        }
    }
}
