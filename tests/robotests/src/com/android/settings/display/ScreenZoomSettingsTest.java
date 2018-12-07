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
 */

package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class ScreenZoomSettingsTest {

    private ScreenZoomSettings mSettings;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mSettings = spy(new ScreenZoomSettings());
        doReturn(mContext).when(mSettings).getContext();
    }

    @Test
    public void getPreviewSampleResIds_default_return3Previews() {
        assertThat(mSettings.getPreviewSampleResIds()).hasLength(3);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void getPreviewSampleResIds_extraPreviewDisabled_return1Preview() {
        assertThat(mSettings.getPreviewSampleResIds()).hasLength(1);
    }
}
