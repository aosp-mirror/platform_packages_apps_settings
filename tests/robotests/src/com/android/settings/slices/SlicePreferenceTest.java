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

package com.android.settings.slices;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.Context;
import android.net.Uri;
import android.view.View;

import androidx.slice.Slice;
import androidx.slice.widget.SliceView;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SlicePreferenceTest {

    private SlicePreference mSlicePreference;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        mSlicePreference = new SlicePreference(mContext, Robolectric.buildAttributeSet()
                .setStyleAttribute("@style/SlicePreference")
                .build());
    }

    @Test
    public void onSliceUpdated_null_hideSliceView() {
        final SliceView sliceView = mSlicePreference.findViewById(R.id.slice_view);

        mSlicePreference.onSliceUpdated(null);

        assertThat(sliceView.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void onSliceUpdated_notNull_showSliceView() {
        final SliceView sliceView = mSlicePreference.findViewById(R.id.slice_view);

        mSlicePreference.onSliceUpdated(new Slice.Builder(Uri.parse("uri")).build());

        assertThat(sliceView.getVisibility()).isEqualTo(View.VISIBLE);
    }
}