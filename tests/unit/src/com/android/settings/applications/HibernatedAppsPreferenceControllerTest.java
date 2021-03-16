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

package com.android.settings.applications;

import static android.provider.DeviceConfig.NAMESPACE_APP_HIBERNATION;

import static com.android.settings.Utils.PROPERTY_APP_HIBERNATION_ENABLED;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.Context;
import android.provider.DeviceConfig;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * TODO(b/181172051): test getNumberHibernated() when the API implemented
 */
@RunWith(AndroidJUnit4.class)
public class HibernatedAppsPreferenceControllerTest {

    private static final String KEY = "key";
    private Context mContext;
    private HibernatedAppsPreferenceController mController;

    @Before
    public void setUp() {
        DeviceConfig.setProperty(NAMESPACE_APP_HIBERNATION, PROPERTY_APP_HIBERNATION_ENABLED,
                "true", false);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mController = new HibernatedAppsPreferenceController(mContext, KEY);
    }

    @Test
    public void getAvailabilityStatus_featureDisabled_shouldNotReturnAvailable() {
        DeviceConfig.setProperty(NAMESPACE_APP_HIBERNATION, PROPERTY_APP_HIBERNATION_ENABLED,
                "false", true);

        assertThat((mController).getAvailabilityStatus()).isNotEqualTo(AVAILABLE);
    }
}
