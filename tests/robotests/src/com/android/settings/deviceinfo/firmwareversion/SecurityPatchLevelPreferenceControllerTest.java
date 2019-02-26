/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.deviceinfo.firmwareversion;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class SecurityPatchLevelPreferenceControllerTest {

    @Mock
    private PackageManager mPackageManager;

    private Context mContext;
    private SecurityPatchLevelPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(Robolectric.setupActivity(Activity.class));
    }

    @Test
    public void getAvailabilityStatus_noPatchInfo_unavailable() {
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SECURITY_PATCH", "");
        mController = new SecurityPatchLevelPreferenceController(mContext, "key");

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_hasPatchInfo_available() {
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SECURITY_PATCH", "foobar");
        mController = new SecurityPatchLevelPreferenceController(mContext, "key");

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void onClick_noActivityIntent_shouldDoNothing() {
        when(mPackageManager.queryIntentActivities(any(), anyInt()))
                .thenReturn(Collections.emptyList());
        mController = new SecurityPatchLevelPreferenceController(mContext, "key");
        ReflectionHelpers.setField(mController, "mPackageManager", mPackageManager);

        final Preference pref = new Preference(mContext);
        pref.setKey(mController.getPreferenceKey());

        mController.handlePreferenceTreeClick(pref);

        verify(mContext, never()).startActivity(any());
    }

    @Test
    public void onClick_activityIntentFound_shouldStartActivity() {
        when(mPackageManager.queryIntentActivities(any(), anyInt()))
                .thenReturn(Collections.singletonList(null));

        mController = new SecurityPatchLevelPreferenceController(mContext, "key");
        ReflectionHelpers.setField(mController, "mPackageManager", mPackageManager);

        final Preference pref = new Preference(mContext);
        pref.setKey(mController.getPreferenceKey());

        mController.handlePreferenceTreeClick(pref);

        verify(mContext).startActivity(any());
    }
}
