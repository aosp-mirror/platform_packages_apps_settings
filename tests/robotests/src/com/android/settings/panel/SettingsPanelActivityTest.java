/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.view.MotionEvent;

import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SettingsPanelActivityTest {

    private FakeFeatureFactory mFakeFeatureFactory;
    private FakeSettingsPanelActivity mSettingsPanelActivity;
    private PanelFeatureProvider mPanelFeatureProvider;
    private FakePanelContent mFakePanelContent;

    @Before
    public void setUp() {
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mSettingsPanelActivity = spy(
                Robolectric.buildActivity(FakeSettingsPanelActivity.class).create().get());
        mPanelFeatureProvider = spy(new PanelFeatureProviderImpl());
        mFakeFeatureFactory.panelFeatureProvider = mPanelFeatureProvider;
        mFakePanelContent = new FakePanelContent();
        doReturn(mFakePanelContent).when(mPanelFeatureProvider).getPanel(any(), any(), any());
    }

    @Test
    public void startMediaOutputSlice_withPackageName_bundleShouldHaveValue() {
        final Intent intent = new Intent()
                .setAction("com.android.settings.panel.action.MEDIA_OUTPUT")
                .putExtra("com.android.settings.panel.extra.PACKAGE_NAME",
                        "com.google.android.music");

        final SettingsPanelActivity activity =
                Robolectric.buildActivity(SettingsPanelActivity.class, intent).create().get();

        assertThat(activity.mBundle.getString(KEY_MEDIA_PACKAGE_NAME))
                .isEqualTo("com.google.android.music");
        assertThat(activity.mBundle.getString(KEY_PANEL_TYPE_ARGUMENT))
                .isEqualTo("com.android.settings.panel.action.MEDIA_OUTPUT");
    }

    @Test
    public void startMediaOutputSlice_withoutPackageName_bundleShouldHaveValue() {
        final Intent intent = new Intent()
                .setAction("com.android.settings.panel.action.MEDIA_OUTPUT");

        final SettingsPanelActivity activity =
                Robolectric.buildActivity(SettingsPanelActivity.class, intent).create().get();

        assertThat(activity.mBundle.containsKey(KEY_MEDIA_PACKAGE_NAME)).isTrue();
        assertThat(activity.mBundle.getString(KEY_PANEL_TYPE_ARGUMENT))
                .isEqualTo("com.android.settings.panel.action.MEDIA_OUTPUT");
    }
}
