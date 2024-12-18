/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.bluetooth.BluetoothLeBroadcastMetadataExt;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AudioStreamsRepositoryTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    private static final String METADATA_STR =
            "BLUETOOTH:UUID:184F;BN:VGVzdA==;AT:1;AD:00A1A1A1A1A1;BI:1E240;BC:VGVzdENvZGU=;"
                    + "MD:BgNwVGVzdA==;AS:1;PI:A0;NS:1;BS:3;NB:2;SM:BQNUZXN0BARlbmc=;;";
    private static final String TEST_SHARED_PREFERENCE = "AudioStreamsRepositoryTestPref";
    private final BluetoothLeBroadcastMetadata mMetadata =
            BluetoothLeBroadcastMetadataExt.INSTANCE.convertToBroadcastMetadata(METADATA_STR);
    private Context mContext;
    private AudioStreamsRepository mAudioStreamsRepository;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        doReturn(getSharedPreferences()).when(mContext).getSharedPreferences(anyString(), anyInt());
        mAudioStreamsRepository = AudioStreamsRepository.getInstance();
    }

    @Test
    public void cacheAndGetMetadata_sameId() {
        mAudioStreamsRepository.cacheMetadata(mMetadata);

        assertThat(mMetadata).isNotNull();
        assertThat(mAudioStreamsRepository.getCachedMetadata(mMetadata.getBroadcastId()))
                .isEqualTo(mMetadata);
    }

    @Test
    public void cacheAndGetMetadata_differentId() {
        mAudioStreamsRepository.cacheMetadata(mMetadata);

        assertThat(mMetadata).isNotNull();
        assertThat(mAudioStreamsRepository.getCachedMetadata(1)).isNull();
    }

    @Test
    public void saveAndGetMetadata_sameId() {
        mAudioStreamsRepository.saveMetadata(mContext, mMetadata);

        assertThat(mMetadata).isNotNull();
        assertThat(mAudioStreamsRepository.getSavedMetadata(mContext, mMetadata.getBroadcastId()))
                .isEqualTo(mMetadata);
    }

    @Test
    public void saveAndGetMetadata_differentId() {
        mAudioStreamsRepository.saveMetadata(mContext, mMetadata);

        assertThat(mMetadata).isNotNull();
        assertThat(mAudioStreamsRepository.getSavedMetadata(mContext, 1)).isNull();
    }

    private SharedPreferences getSharedPreferences() {
        return mContext.getSharedPreferences(TEST_SHARED_PREFERENCE, Context.MODE_PRIVATE);
    }
}
