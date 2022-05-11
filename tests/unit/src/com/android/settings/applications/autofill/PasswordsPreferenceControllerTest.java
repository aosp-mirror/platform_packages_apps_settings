/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.applications.autofill;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.os.UserHandle;
import android.service.autofill.AutofillServiceInfo;

import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class PasswordsPreferenceControllerTest {

    private Context mContext;
    private PreferenceScreen mScreen;
    private PreferenceCategory mPasswordsPreferenceCategory;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        if (Looper.myLooper() == null) {
            Looper.prepare(); // needed to create the preference screen
        }
        mScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mPasswordsPreferenceCategory = new PreferenceCategory(mContext);
        mPasswordsPreferenceCategory.setKey("passwords");
        mScreen.addPreference(mPasswordsPreferenceCategory);
    }

    @Test
    // Tests that getAvailabilityStatus() does not throw an exception if it's called before the
    // Controller is initialized (this can happen during indexing).
    public void getAvailabilityStatus_withoutInit_returnsUnavailable() {
        PasswordsPreferenceController controller =
                new PasswordsPreferenceController(mContext, mPasswordsPreferenceCategory.getKey());
        assertThat(controller.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noServices_returnsUnavailable() {
        PasswordsPreferenceController controller =
                createControllerWithServices(Collections.emptyList());
        assertThat(controller.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noPasswords_returnsUnavailable() {
        AutofillServiceInfo service = new AutofillServiceInfo.TestDataBuilder().build();
        PasswordsPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(service));
        assertThat(controller.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_withPasswords_returnsAvailable() {
        PasswordsPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(createServiceWithPasswords()));
        assertThat(controller.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void displayPreference_noServices_noPreferencesAdded() {
        PasswordsPreferenceController controller =
                createControllerWithServices(Collections.emptyList());
        controller.displayPreference(mScreen);
        assertThat(mPasswordsPreferenceCategory.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void displayPreference_noPasswords_noPreferencesAdded() {
        AutofillServiceInfo service = new AutofillServiceInfo.TestDataBuilder().build();
        PasswordsPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(service));
        controller.displayPreference(mScreen);
        assertThat(mPasswordsPreferenceCategory.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    @UiThreadTest
    public void displayPreference_withPasswords_addsPreference() {
        AutofillServiceInfo service = createServiceWithPasswords();
        service.getServiceInfo().packageName = "";
        service.getServiceInfo().name = "";
        PasswordsPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(service));
        doReturn(false).when(mContext).bindServiceAsUser(any(), any(), anyInt(), any());

        controller.displayPreference(mScreen);

        assertThat(mPasswordsPreferenceCategory.getPreferenceCount()).isEqualTo(1);
        Preference pref = mPasswordsPreferenceCategory.getPreference(0);
        assertThat(pref.getIcon()).isNotNull();
        pref.performClick();
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        UserHandle user = mContext.getUser();
        verify(mContext).startActivityAsUser(intentCaptor.capture(), eq(user));
        assertThat(intentCaptor.getValue().getComponent())
                .isEqualTo(
                        new ComponentName(
                                service.getServiceInfo().packageName,
                                service.getPasswordsActivity()));
    }

    private PasswordsPreferenceController createControllerWithServices(
            List<AutofillServiceInfo> availableServices) {
        PasswordsPreferenceController controller =
                new PasswordsPreferenceController(mContext, mPasswordsPreferenceCategory.getKey());
        controller.init(() -> mock(Lifecycle.class), availableServices);
        return controller;
    }

    private AutofillServiceInfo createServiceWithPasswords() {
        return new AutofillServiceInfo.TestDataBuilder()
                .setPasswordsActivity("com.android.test.Passwords")
                .build();
    }
}
