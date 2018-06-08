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

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.util.Map;

@RunWith(SettingsRobolectricTestRunner.class)
public class DatabaseIndexingUtilsTest {

    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void testGetPreferenceControllerUriMap_BadClassName_ReturnsNull() {
        Map map = DatabaseIndexingUtils.getPayloadKeyMap("dummy", mContext);
        assertThat(map).isEmpty();
    }

    @Test
    public void testGetPreferenceControllerUriMap_NullContext_ReturnsNull() {
        Map map = DatabaseIndexingUtils.getPayloadKeyMap("dummy", null);
        assertThat(map).isEmpty();
    }

    @Test
    public void testGetPayloadFromMap_NullMap_ReturnsNull() {
        final String className = "com.android.settings.system.SystemDashboardFragment";
        final Map<String, ResultPayload> map =
                DatabaseIndexingUtils.getPayloadKeyMap(className, mContext);
        ResultPayload payload = map.get(null);
        assertThat(payload).isNull();
    }
}
