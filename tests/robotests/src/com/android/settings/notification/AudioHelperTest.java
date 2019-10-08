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
 * limitations under the License
 */

package com.android.settings.notification;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AudioHelperTest {

    private static final int START = -10;
    private static final int END = 10;
    private static final int DEFAULT = -100;

    private Context mContext;
    private AudioHelper mAudioHelper;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mAudioHelper = new AudioHelper(mContext);
    }

    @Test
    public void getMaxVolume_anyStreamType_getValue() {
        int volume = DEFAULT;

        for (int i = START; i < END; i++) {
            volume = mAudioHelper.getMaxVolume(i);
            assertThat(volume).isNotEqualTo(DEFAULT);
        }
    }

    @Test
    public void getMinVolume_anyStreamType_getValue() {
        int volume = DEFAULT;

        for (int i = START; i < END; i++) {
            volume = mAudioHelper.getMinVolume(i);
            assertThat(volume).isNotEqualTo(DEFAULT);
        }
    }
}
