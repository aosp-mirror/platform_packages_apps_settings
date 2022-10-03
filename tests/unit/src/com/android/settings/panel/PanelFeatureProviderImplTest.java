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
package com.android.settings.panel;

import static com.android.settings.panel.SettingsPanelActivity.KEY_MEDIA_PACKAGE_NAME;
import static com.android.settings.panel.SettingsPanelActivity.KEY_PANEL_TYPE_ARGUMENT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.FeatureFlagUtils;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

@RunWith(AndroidJUnit4.class)
public class PanelFeatureProviderImplTest {

    private static final String TEST_PACKAGENAME = "com.test.packagename";

    private static final String SYSTEMUI_PACKAGE_NAME = "com.android.systemui";
    private Context mContext;
    private PanelFeatureProviderImpl mProvider;
    private Bundle mBundle;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mProvider = new PanelFeatureProviderImpl();
        mBundle = new Bundle();
        mBundle.putString(KEY_MEDIA_PACKAGE_NAME, TEST_PACKAGENAME);
    }

    @Test
    public void getPanel_internetConnectivityKey_sendsCorrectBroadcast() {
        mBundle.putString(KEY_PANEL_TYPE_ARGUMENT, Settings.Panel.ACTION_INTERNET_CONNECTIVITY);
        mProvider.getPanel(mContext, mBundle);
        Intent intent = new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                .setPackage(SYSTEMUI_PACKAGE_NAME);

        verify(mContext, never()).sendBroadcast(intent);
    }

    @Test
    public void getPanel_volumePanel_returnsCorrectPanel() {
        FeatureFlagUtils.setEnabled(mContext, FeatureFlagUtils.SETTINGS_VOLUME_PANEL_IN_SYSTEMUI,
                false);
        mBundle.putString(KEY_PANEL_TYPE_ARGUMENT, Settings.Panel.ACTION_VOLUME);

        final PanelContent panel = mProvider.getPanel(mContext, mBundle);

        assertThat(panel).isInstanceOf(VolumePanel.class);
    }

    @Test
    public void getPanel_volumePanelFlagEnabled_sendRedirectIntent() {
        FeatureFlagUtils.setEnabled(mContext, FeatureFlagUtils.SETTINGS_VOLUME_PANEL_IN_SYSTEMUI,
                true);
        mBundle.putString(KEY_PANEL_TYPE_ARGUMENT, Settings.Panel.ACTION_VOLUME);
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);

        mProvider.getPanel(mContext, mBundle);

        verify(mContext).sendBroadcast(intentCaptor.capture());
        assertThat(intentCaptor.getValue().getAction()).isEqualTo(Settings.Panel.ACTION_VOLUME);
        assertThat(intentCaptor.getValue().getPackage()).isEqualTo(SYSTEMUI_PACKAGE_NAME);
    }
}
