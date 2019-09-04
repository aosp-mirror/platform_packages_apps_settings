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

import static com.android.settingslib.media.MediaOutputSliceConstants.ACTION_MEDIA_OUTPUT;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PanelFeatureProviderImplTest {

    private static final String TEST_PACKAGENAME = "com.test.packagename";

    private Context mContext;
    private PanelFeatureProviderImpl mProvider;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mProvider = new PanelFeatureProviderImpl();
    }

    @Test
    public void getPanel_internetConnectivityKey_returnsCorrectPanel() {
        final PanelContent panel = mProvider.getPanel(mContext,
                Settings.Panel.ACTION_INTERNET_CONNECTIVITY, TEST_PACKAGENAME);

        assertThat(panel).isInstanceOf(InternetConnectivityPanel.class);
    }

    @Test
    public void getPanel_volume_returnsCorrectPanel() {
        final PanelContent panel = mProvider.getPanel(mContext,
                Settings.Panel.ACTION_VOLUME, TEST_PACKAGENAME);

        assertThat(panel).isInstanceOf(VolumePanel.class);
    }

    @Test
    public void getPanel_mediaOutputKey_returnsCorrectPanel() {
        final PanelContent panel = mProvider.getPanel(mContext,
                ACTION_MEDIA_OUTPUT, TEST_PACKAGENAME);

        assertThat(panel).isInstanceOf(MediaOutputPanel.class);
    }
}
