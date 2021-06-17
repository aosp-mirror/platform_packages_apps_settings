/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.wifi.savedaccesspoints2;

import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
public class SubscribedAccessPointsPreferenceController2Test {

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private PreferenceCategory mPreferenceCategory;

    private Context mContext;
    private SavedAccessPointsWifiSettings2 mSettings;
    private SubscribedAccessPointsPreferenceController2 mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mSettings = spy(new SavedAccessPointsWifiSettings2());
        mController = spy(new SubscribedAccessPointsPreferenceController2(mContext, "test_key"));
        mController.setHost(mSettings);

        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreferenceCategory);
        when(mPreferenceCategory.getContext()).thenReturn(mContext);
    }

    @Test
    public void getAvailability_noSubscripbedAccessPoint_shouldNotAvailable() {
        mController.mWifiEntries = new ArrayList<>();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }
}

