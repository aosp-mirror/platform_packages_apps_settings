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
package com.android.settings.deviceinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class RegulatoryInfoPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private Preference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private PackageManager mPackageManager;
    private RegulatoryInfoPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);

        mController = new RegulatoryInfoPreferenceController(mContext);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreference);
    }

    @Test
    public void isAvailable_hasIntent_returnTrue() {
        final List<ResolveInfo> activities = new ArrayList<>();
        activities.add(new ResolveInfo());
        when(mPackageManager.queryIntentActivities(any(Intent.class), eq(0)))
                .thenReturn(activities);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_hasNoIntent_returnFalse() {
        final List<ResolveInfo> activities = new ArrayList<>();
        when(mPackageManager.queryIntentActivities(any(Intent.class), eq(0)))
                .thenReturn(activities);

        assertThat(mController.isAvailable()).isFalse();
    }
}
