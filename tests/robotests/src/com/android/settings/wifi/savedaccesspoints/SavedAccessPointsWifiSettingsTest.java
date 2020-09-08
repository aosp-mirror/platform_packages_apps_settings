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

package com.android.settings.wifi.savedaccesspoints;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SavedAccessPointsWifiSettingsTest {

    @Mock
    private SubscribedAccessPointsPreferenceController mSubscribedApController;
    @Mock
    private SavedAccessPointsPreferenceController mSavedApController;

    private TestFragment mSettings;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mSettings = spy(new TestFragment());

        doReturn(mSubscribedApController).when(mSettings)
                .use(SubscribedAccessPointsPreferenceController.class);
        doReturn(mSavedApController).when(mSettings)
                .use(SavedAccessPointsPreferenceController.class);
    }

    @Test
    public void verifyConstants() {
        assertThat(mSettings.getMetricsCategory()).isEqualTo(MetricsEvent.WIFI_SAVED_ACCESS_POINTS);
        assertThat(mSettings.getPreferenceScreenResId())
                .isEqualTo(R.xml.wifi_display_saved_access_points);
    }

    public static class TestFragment extends SavedAccessPointsWifiSettings {

        public <T extends AbstractPreferenceController> T use(Class<T> clazz) {
            return super.use(clazz);
        }
    }
}
