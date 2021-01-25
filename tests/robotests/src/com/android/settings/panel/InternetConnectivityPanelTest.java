/*
 * Copyright (C) 2018 The Android Open Source Project
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
 *
 */

package com.android.settings.panel;

import static com.google.common.truth.Truth.assertThat;

import android.net.Uri;
import android.os.SystemProperties;

import com.android.settings.network.AirplaneModePreferenceController;
import com.android.settings.slices.CustomSliceRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(RobolectricTestRunner.class)

public class InternetConnectivityPanelTest {

    private InternetConnectivityPanel mPanel;
    private static final String SETTINGS_PROVIDER_MODEL =
            "persist.sys.fflag.override.settings_provider_model";
    private boolean mSettingsProviderModelState;

    @Before
    public void setUp() {
        mPanel = InternetConnectivityPanel.create(RuntimeEnvironment.application);
        mSettingsProviderModelState = SystemProperties.getBoolean(SETTINGS_PROVIDER_MODEL, false);
    }

    @After
    public void tearDown() {
        SystemProperties.set(SETTINGS_PROVIDER_MODEL,
                mSettingsProviderModelState ? "true" : "false");
    }

    @Test
    public void getSlices_providerModelDisabled_containsNecessarySlices() {
        SystemProperties.set(SETTINGS_PROVIDER_MODEL, "false");
        final List<Uri> uris = mPanel.getSlices();

        assertThat(uris).containsExactly(
                AirplaneModePreferenceController.SLICE_URI,
                CustomSliceRegistry.MOBILE_DATA_SLICE_URI,
                CustomSliceRegistry.WIFI_SLICE_URI);
    }

    @Test
    public void getSlices_providerModelEnabled_containsNecessarySlices() {
        SystemProperties.set(SETTINGS_PROVIDER_MODEL, "true");
        final List<Uri> uris = mPanel.getSlices();

        assertThat(uris).containsExactly(
                CustomSliceRegistry.PROVIDER_MODEL_SLICE_URI,
                CustomSliceRegistry.AIRPLANE_SAFE_NETWORKS_SLICE_URI);
    }

    @Test
    public void getSeeMoreIntent_notNull() {
        assertThat(mPanel.getSeeMoreIntent()).isNotNull();
    }
}
