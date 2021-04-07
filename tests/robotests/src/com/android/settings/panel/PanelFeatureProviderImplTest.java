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

import static com.android.settings.panel.SettingsPanelActivity.KEY_MEDIA_PACKAGE_NAME;
import static com.android.settings.panel.SettingsPanelActivity.KEY_PANEL_TYPE_ARGUMENT;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.Bundle;
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
    private Bundle mBundle;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mProvider = new PanelFeatureProviderImpl();
        mBundle = new Bundle();
        mBundle.putString(KEY_MEDIA_PACKAGE_NAME, TEST_PACKAGENAME);
    }

    @Test
    public void getPanel_internetConnectivityKey_returnsCorrectPanel() {
        mBundle.putString(KEY_PANEL_TYPE_ARGUMENT, Settings.Panel.ACTION_INTERNET_CONNECTIVITY);

        final PanelContent panel = mProvider.getPanel(mContext, mBundle);

        assertThat(panel).isInstanceOf(InternetConnectivityPanel.class);
    }

    @Test
    public void getPanel_volume_returnsCorrectPanel() {
        mBundle.putString(KEY_PANEL_TYPE_ARGUMENT, Settings.Panel.ACTION_VOLUME);

        final PanelContent panel = mProvider.getPanel(mContext, mBundle);

        assertThat(panel).isInstanceOf(VolumePanel.class);
    }
}
