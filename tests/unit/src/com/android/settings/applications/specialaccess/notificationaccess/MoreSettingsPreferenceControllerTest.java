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

package com.android.settings.applications.specialaccess.notificationaccess;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.service.notification.NotificationListenerService;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class MoreSettingsPreferenceControllerTest {

    Context mContext;
    private MoreSettingsPreferenceController mController;
    @Mock
    PackageManager mPm;
    final String mPkg = "pkg";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();

        mController = new MoreSettingsPreferenceController(mContext);
        mController.setPackage(mPkg);
        mController.setPackageManager(mPm);

    }

    @Test
    public void getAvailabilityStatus_available() {
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        when(mPm.queryIntentActivities(captor.capture(), any())).thenReturn(
                ImmutableList.of(mock(ResolveInfo.class)));

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(captor.getValue().getPackage()).isEqualTo(mPkg);
        assertThat(captor.getValue().getAction()).contains(
                NotificationListenerService.ACTION_SETTINGS_HOME);
    }

    @Test
    public void getAvailabilityStatus_notAvailable() {
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        when(mPm.queryIntentActivities(captor.capture(), any())).thenReturn(ImmutableList.of());

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void updateState() {
        Preference preference = new Preference(mContext);
        mController.updateState(preference);

        assertThat(preference.getIntent().getPackage()).isEqualTo(mPkg);
        assertThat(preference.getIntent().getAction()).isEqualTo(
                NotificationListenerService.ACTION_SETTINGS_HOME);
    }
}
