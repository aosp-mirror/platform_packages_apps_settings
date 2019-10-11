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

package com.android.settings.security.screenlock;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.security.OwnerInfoPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class ScreenLockSettingsTest {

    private ScreenLockSettings mSettings;

    @Before
    public void setUp() {
        mSettings = new ScreenLockSettings();
    }

    @Test
    public void verifyConstants() {
        assertThat(mSettings.getMetricsCategory())
                .isEqualTo(MetricsProto.MetricsEvent.SCREEN_LOCK_SETTINGS);
        assertThat(mSettings.getPreferenceScreenResId()).isEqualTo(R.xml.screen_lock_settings);
    }

    @Test
    public void onOwnerInfoUpdated_shouldUpdateOwnerInfoController() {
        final Map<Class, List<AbstractPreferenceController>> preferenceControllers =
                ReflectionHelpers.getField(mSettings, "mPreferenceControllers");
        final OwnerInfoPreferenceController controller = mock(OwnerInfoPreferenceController.class);
        List<AbstractPreferenceController> controllerList = new ArrayList<>();
        controllerList.add(controller);
        preferenceControllers.put(OwnerInfoPreferenceController.class, controllerList);

        mSettings.onOwnerInfoUpdated();

        verify(controller).updateSummary();
    }
}
