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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

import java.util.Comparator;

@RunWith(RobolectricTestRunner.class)
public class AudioStreamsProgressCategoryPreferenceTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock PreferenceManager mPreferenceManager;
    private Context mContext;
    private AudioStreamsProgressCategoryPreference mPreference;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mPreference = spy(new AudioStreamsProgressCategoryPreference(mContext));
        when(mPreference.getPreferenceManager()).thenReturn(mPreferenceManager);
    }

    @Test
    public void addAudioStreamPreference_singlePreference() {
        mPreference = spy(new AudioStreamsProgressCategoryPreference(mContext, null));
        when(mPreference.getPreferenceManager()).thenReturn(mPreferenceManager);
        AudioStreamPreference first = new AudioStreamPreference(mContext, null);
        mPreference.addAudioStreamPreference(first, (p1, p2) -> 0);

        assertThat(mPreference.getPreferenceCount()).isEqualTo(1);
        assertThat(mPreference.getPreference(0)).isEqualTo(first);
    }

    @Test
    public void addAudioStreamPreference_multiPreference_sorted() {
        mPreference = spy(new AudioStreamsProgressCategoryPreference(mContext, null, 0));
        when(mPreference.getPreferenceManager()).thenReturn(mPreferenceManager);
        Comparator<AudioStreamPreference> c =
                Comparator.comparingInt(AudioStreamPreference::getOrder);
        AudioStreamPreference first = new AudioStreamPreference(mContext, null);
        first.setOrder(1);
        AudioStreamPreference second = new AudioStreamPreference(mContext, null);
        second.setOrder(0);
        mPreference.addAudioStreamPreference(first, c);
        mPreference.addAudioStreamPreference(second, c);

        assertThat(mPreference.getPreferenceCount()).isEqualTo(2);
        assertThat(mPreference.getPreference(0)).isEqualTo(second);
        assertThat(mPreference.getPreference(1)).isEqualTo(first);
    }

    @Test
    public void removeAudioStreamPreferences_shouldBeEmpty() {
        mPreference = spy(new AudioStreamsProgressCategoryPreference(mContext, null, 0, 0));
        when(mPreference.getPreferenceManager()).thenReturn(mPreferenceManager);
        Comparator<AudioStreamPreference> c =
                Comparator.comparingInt(AudioStreamPreference::getOrder);
        AudioStreamPreference first = new AudioStreamPreference(mContext, null);
        first.setOrder(0);
        AudioStreamPreference second = new AudioStreamPreference(mContext, null);
        second.setOrder(1);
        mPreference.addAudioStreamPreference(first, c);
        mPreference.addAudioStreamPreference(second, c);
        mPreference.removeAudioStreamPreferences();

        assertThat(mPreference.getPreferenceCount()).isEqualTo(0);
    }
}
