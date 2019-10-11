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

package com.android.settings.core.instrumentation;

import static com.google.common.truth.Truth.assertThat;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.intelligence.LogProto.SettingsLog;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SettingsIntelligenceLogWriterTest {
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void serialize_hasSizeOne_returnCorrectData() throws IOException {
        final SettingsLog event = SettingsLog.newBuilder()
                .setAttribution(SettingsEnums.DASHBOARD_SUMMARY)
                .setAction(SettingsEnums.ACTION_SET_NEW_PASSWORD)
                .setPageId(SettingsEnums.SET_NEW_PASSWORD_ACTIVITY)
                .setChangedPreferenceKey("package")
                .setChangedPreferenceIntValue(100)
                .build();
        List<SettingsLog> events = new ArrayList<>();
        events.add(event);

        // execute
        final byte[] data = SettingsIntelligenceLogWriter.serialize(events);

        // parse data
        final ByteArrayInputStream bin = new ByteArrayInputStream(data);
        final DataInputStream inputStream = new DataInputStream(bin);
        final int size = inputStream.readInt();
        final byte[] change = new byte[inputStream.readInt()];
        inputStream.read(change);
        inputStream.close();
        final SettingsLog settingsLog = SettingsLog.parseFrom(change);

        // assert
        assertThat(events.size()).isEqualTo(size);
        assertThat(settingsLog.getAttribution()).isEqualTo(SettingsEnums.DASHBOARD_SUMMARY);
        assertThat(settingsLog.getAction()).isEqualTo(SettingsEnums.ACTION_SET_NEW_PASSWORD);
        assertThat(settingsLog.getPageId()).isEqualTo(SettingsEnums.SET_NEW_PASSWORD_ACTIVITY);
        assertThat(settingsLog.getChangedPreferenceKey()).isEqualTo("package");
        assertThat(settingsLog.getChangedPreferenceIntValue()).isEqualTo(100);
    }
}
