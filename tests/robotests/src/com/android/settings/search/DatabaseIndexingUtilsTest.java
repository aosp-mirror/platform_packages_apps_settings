/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.search;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.util.ArrayMap;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.deviceinfo.SystemUpdatePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Map;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DatabaseIndexingUtilsTest {

    private Context mContext;
    @Mock
    private AmbientDisplayConfiguration mAmbientDisplayConfiguration;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void testGetPreferenceControllerUriMap_BadClassName_ReturnsNull() {
        Map map = DatabaseIndexingUtils.getPreferenceControllerUriMap("dummy", mContext);
        assertThat(map).isNull();
    }

    @Test
    public void testGetPreferenceControllerUriMap_NullContext_ReturnsNull() {
        Map map = DatabaseIndexingUtils.getPreferenceControllerUriMap("dummy", null);
        assertThat(map).isNull();
    }

    @Test
    public void testGetPreferenceControllerUriMap_CompatibleClass_ReturnsValidMap() {
        final String className = "com.android.settings.system.SystemDashboardFragment";
        final Map<String, PreferenceControllerMixin> map =
                DatabaseIndexingUtils.getPreferenceControllerUriMap(className, mContext);
        assertThat(map.get("system_update_settings"))
                .isInstanceOf(SystemUpdatePreferenceController.class);
    }

    @Test
    public void testGetPayloadFromMap_NullMap_ReturnsNull() {
        ResultPayload payload = DatabaseIndexingUtils.getPayloadFromUriMap(null, "");
        assertThat(payload).isNull();
    }

    @Test
    public void testGetPayloadFromMap_MatchingKey_ReturnsPayload() {
        final String key = "key";
        PreferenceControllerMixin prefController = new PreferenceControllerMixin() {
            @Override
            public ResultPayload getResultPayload() {
                return new ResultPayload(null);
            }
        };
        ArrayMap<String, PreferenceControllerMixin> map = new ArrayMap<>();
        map.put(key, prefController);

        ResultPayload payload = DatabaseIndexingUtils.getPayloadFromUriMap(map, key);
        assertThat(payload).isInstanceOf(ResultPayload.class);
    }
}