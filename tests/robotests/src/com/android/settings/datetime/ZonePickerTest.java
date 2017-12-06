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

package com.android.settings.datetime;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.core.instrumentation.VisibilityLoggerMixin;
import com.android.settings.testutils.shadow.ShadowZoneGetter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ZonePickerTest {

    private Activity mActivity;
    private ZonePicker mZonePicker;

    @Before
    public void setUp() {
        mActivity = Robolectric.setupActivity(Activity.class);
        mZonePicker = spy(ZonePicker.class);
        ReflectionHelpers.setField(mZonePicker, "mVisibilityLoggerMixin",
                mock(VisibilityLoggerMixin.class));
    }

    @Test
    @Config(shadows = ShadowZoneGetter.class)
    public void testLaunch() {
        // Shouldn't crash
        mActivity.getFragmentManager()
                .beginTransaction()
                .add(mZonePicker, "test_tag")
                .commit();

        // Should render
        verify(mZonePicker).onCreateView(
                nullable(LayoutInflater.class),
                nullable(ViewGroup.class),
                nullable(Bundle.class));
    }
}
