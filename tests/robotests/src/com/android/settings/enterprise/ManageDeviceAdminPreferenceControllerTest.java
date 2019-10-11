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

package com.android.settings.enterprise;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class ManageDeviceAdminPreferenceControllerTest {

    @Mock
    private Resources mResources;

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private ManageDeviceAdminPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mController = new ManageDeviceAdminPreferenceController(mContext, "testkey");
    }

    @Test
    public void testUpdateState() {
        final Preference preference = new Preference(mContext, null, 0, 0);

        when(mFeatureFactory.enterprisePrivacyFeatureProvider
                .getNumberOfActiveDeviceAdminsForCurrentUserAndManagedProfile()).thenReturn(0);
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getString(R.string.number_of_device_admins_none))
                .thenReturn("no apps");
        mController.updateState(preference);
        assertThat(preference.getSummary()).isEqualTo("no apps");

        when(mFeatureFactory.enterprisePrivacyFeatureProvider
                .getNumberOfActiveDeviceAdminsForCurrentUserAndManagedProfile()).thenReturn(5);
        when(mResources.getQuantityString(R.plurals.number_of_device_admins, 5, 5))
                .thenReturn("5 active apps");
        mController.updateState(preference);
        assertThat(preference.getSummary()).isEqualTo("5 active apps");
    }

    @Test
    public void isAvailable_byDefault_isTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isAvailable_whenNotVisible_isFalse() {
        assertThat(mController.isAvailable()).isFalse();
    }
}
