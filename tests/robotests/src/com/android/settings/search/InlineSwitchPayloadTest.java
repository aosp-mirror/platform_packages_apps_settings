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

import android.os.Parcel;
import android.util.ArrayMap;
import android.content.Context;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.search2.InlineSwitchPayload;
import com.android.settings.search2.ResultPayload;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class InlineSwitchPayloadTest {

    @Test
    public void testGetSwitch_EmptyMap_ExceptionThrown() {
        final String uri = "test.com";
        final int source = ResultPayload.SettingsSource.SECURE;

        final Context context = ShadowApplication.getInstance().getApplicationContext();
        InlineSwitchPayload payload = new InlineSwitchPayload(uri, source, null);
        try {
            payload.getSwitchValue(context);
            fail("Should have thrown exception for null map");
        } catch (IllegalStateException e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    public void testGetSwitch_BadMap_ExceptionThrown() {
        final String uri = "test.com";
        final int source = ResultPayload.SettingsSource.SECURE;
        final ArrayMap<Integer, Boolean> map = new ArrayMap<>();

        final Context context = ShadowApplication.getInstance().getApplicationContext();
        InlineSwitchPayload payload = new InlineSwitchPayload(uri, source, map);
        try {
            payload.getSwitchValue(context);
            fail("Should have thrown exception for bad map");
        } catch (IllegalStateException e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    public void testConstructor_DataRetained() {
        final String uri = "test.com";
        final int type = ResultPayload.PayloadType.INLINE_SWITCH;
        final int source = ResultPayload.SettingsSource.SECURE;
        final ArrayMap<Integer, Boolean> map = new ArrayMap<>();
        map.put(1, true);
        map.put(0, false);

        InlineSwitchPayload payload = new InlineSwitchPayload(uri, source, map);
        assertThat(payload.settingsUri).isEqualTo(uri);
        assertThat(payload.inlineType).isEqualTo(type);
        assertThat(payload.settingSource).isEqualTo(source);
        assertThat(payload.valueMap.get(1)).isTrue();
        assertThat(payload.valueMap.get(0)).isFalse();
    }

    @Test
    public void testParcelConstructor_DataRetained() {
        String uri = "test.com";
        int type = ResultPayload.PayloadType.INLINE_SWITCH;
        int source = ResultPayload.SettingsSource.SECURE;
        final ArrayMap<Integer, Boolean> map = new ArrayMap<>();
        map.put(1, true);
        map.put(0, false);

        Parcel parcel = Parcel.obtain();
        parcel.writeString(uri);
        parcel.writeInt(type);
        parcel.writeInt(source);
        parcel.writeMap(map);
        parcel.setDataPosition(0);

        InlineSwitchPayload payload = InlineSwitchPayload.CREATOR.createFromParcel(parcel);
        assertThat(payload.settingsUri).isEqualTo(uri);
        assertThat(payload.inlineType).isEqualTo(type);
        assertThat(payload.settingSource).isEqualTo(source);
        assertThat(payload.valueMap.get(1)).isTrue();
        assertThat(payload.valueMap.get(0)).isFalse();
    }


}
