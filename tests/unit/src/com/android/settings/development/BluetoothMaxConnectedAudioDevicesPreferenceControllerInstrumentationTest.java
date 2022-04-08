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

package com.android.settings.development;

import android.content.Context;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.settings.R;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BluetoothMaxConnectedAudioDevicesPreferenceControllerInstrumentationTest {

    private Context mTargetContext;
    private String[] mListValues;
    private String[] mListEntries;
    private String mDefaultMaxConnectedAudioDevices;

    @Before
    public void setUp() throws Exception {
        mTargetContext = InstrumentationRegistry.getTargetContext();
        // Get XML values without mock
        mListValues = mTargetContext.getResources()
                .getStringArray(R.array.bluetooth_max_connected_audio_devices_values);
        mListEntries = mTargetContext.getResources()
                .getStringArray(R.array.bluetooth_max_connected_audio_devices);
        mDefaultMaxConnectedAudioDevices = String.valueOf(mTargetContext.getResources()
                .getInteger(
                        com.android.internal.R.integer
                                .config_bluetooth_max_connected_audio_devices));
    }

    @Test
    public void verifyResource() {
        // Verify normal list entries and default preference entries have the same size
        Assert.assertEquals(mListEntries.length, mListValues.length);
        Assert.assertThat(Arrays.asList(mListValues),
                CoreMatchers.hasItem(mDefaultMaxConnectedAudioDevices));
    }
}
