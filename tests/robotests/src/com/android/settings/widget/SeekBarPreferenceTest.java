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

package com.android.settings.widget;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Parcelable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SeekBarPreferenceTest {

    private static final int MAX = 75;
    private static final int MIN = 5;
    private static final int PROGRESS = 16;

    private Context mContext;
    private SeekBarPreference mSeekBarPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        mSeekBarPreference = spy(new SeekBarPreference(mContext));
        mSeekBarPreference.setMax(MAX);
        mSeekBarPreference.setMin(MIN);
        mSeekBarPreference.setProgress(PROGRESS);
        mSeekBarPreference.setPersistent(false);
    }

    @Test
    public void testSaveAndRestoreInstanceState() {
        final Parcelable parcelable = mSeekBarPreference.onSaveInstanceState();

        final SeekBarPreference preference = new SeekBarPreference(mContext);
        preference.onRestoreInstanceState(parcelable);

        assertThat(preference.getMax()).isEqualTo(MAX);
        assertThat(preference.getMin()).isEqualTo(MIN);
        assertThat(preference.getProgress()).isEqualTo(PROGRESS);
    }

    @Test
    public void isSelectable_disabledByAdmin_returnTrue() {
        when(mSeekBarPreference.isDisabledByAdmin()).thenReturn(true);

        assertThat(mSeekBarPreference.isSelectable()).isTrue();
    }

    @Test
    public void isSelectable_notDisabledByAdmin_returnFalse() {
        when(mSeekBarPreference.isDisabledByAdmin()).thenReturn(false);

        assertThat(mSeekBarPreference.isSelectable()).isFalse();
    }
}
