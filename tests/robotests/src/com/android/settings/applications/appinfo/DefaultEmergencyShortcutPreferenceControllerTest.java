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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class DefaultEmergencyShortcutPreferenceControllerTest {

    @Mock
    private PackageManager mPackageManager;

    private Context mContext;
    private DefaultEmergencyShortcutPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        mController = new DefaultEmergencyShortcutPreferenceController(mContext, "Package1");
    }

    @Test
    public void getPreferenceKey_shouldReturnDefaultEmergency() {
        assertThat(mController.getPreferenceKey()).isEqualTo("default_emergency_app");
    }

    @Test
    public void hasAppCapability_hasEmergencyCapability_shouldReturnTrue() {
        List<ResolveInfo> resolveInfos = new ArrayList<>();
        resolveInfos.add(new ResolveInfo());
        when(mPackageManager.queryIntentActivities(argThat(intent-> intent != null
                && intent.getAction().equals(TelephonyManager.ACTION_EMERGENCY_ASSISTANCE)),
                anyInt())).thenReturn(resolveInfos);

        assertThat(mController.hasAppCapability()).isTrue();
    }

    @Test
    public void hasAppCapability_noEmergencyCapability_shouldReturnFalse() {
        assertThat(mController.hasAppCapability()).isFalse();
    }

    @Test
    public void isDefaultApp_isDefaultEmergency_shouldReturnTrue() {
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.EMERGENCY_ASSISTANCE_APPLICATION, "Package1");

        assertThat(mController.isDefaultApp()).isTrue();
    }

    @Test
    public void isDefaultApp_notDefaultEmergency_shouldReturnFalse() {
        assertThat(mController.isDefaultApp()).isFalse();
    }
}
