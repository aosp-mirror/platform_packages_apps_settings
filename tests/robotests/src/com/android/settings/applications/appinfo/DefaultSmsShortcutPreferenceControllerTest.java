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

import com.android.settings.applications.defaultapps.DefaultSmsPreferenceController;

import org.junit.Before;
import org.junit.Ignore;
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
public class DefaultSmsShortcutPreferenceControllerTest {

    @Mock
    private PackageManager mPackageManager;

    private Context mContext;
    private DefaultSmsShortcutPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        mController = new DefaultSmsShortcutPreferenceController(mContext, "Package1");
    }

    @Test
    public void getPreferenceKey_shouldReturnDefaultSms() {
        assertThat(mController.getPreferenceKey()).isEqualTo("default_sms_app");
    }

    @Test
    @Config(shadows = ShadowDefaultSmsPreferenceController.class)
    public void hasAppCapability_hasSmsCapability_shouldReturnTrue() {
        assertThat(mController.hasAppCapability()).isTrue();
    }

    @Test
    public void hasAppCapability_noSmsCapability_shouldReturnFalse() {
        assertThat(mController.hasAppCapability()).isFalse();
    }

    @Test
    @Config(shadows = ShadowDefaultSmsPreferenceController.class)
    public void isDefaultApp_isDefaultSms_shouldReturnTrue() {
        assertThat(mController.isDefaultApp()).isTrue();
    }

    @Test
    @Ignore("b/122824614")
    public void isDefaultApp_notDefaultSms_shouldReturnFalse() {
        assertThat(mController.isDefaultApp()).isFalse();
    }

    @Implements(DefaultSmsPreferenceController.class)
    public static class ShadowDefaultSmsPreferenceController {
        @Implementation
        protected static boolean hasSmsPreference(String pkg, Context context) {
            return true;
        }

        @Implementation
        protected static boolean isSmsDefault(String pkg, Context context) {
            return true;
        }
    }
}
