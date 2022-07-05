/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for {@link TextReadingResetPreference}.
 */
@RunWith(RobolectricTestRunner.class)
public class TextReadingResetPreferenceTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private TextReadingResetPreference mResetPreference;
    private PreferenceViewHolder mHolder;
    private View mButtonView;

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private View.OnClickListener mOnResetClickListener;

    @Before
    public void setUp() {
        mResetPreference = new TextReadingResetPreference(mContext, /* attrs= */ null);
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(mResetPreference.getLayoutResource(),
                        new FrameLayout(mContext), false);
        mHolder = PreferenceViewHolder.createInstanceForTests(view);
        mButtonView = view.findViewById(R.id.reset_button);
    }

    @Test
    public void setResetListener_success() {
        mResetPreference.setOnResetClickListener(mOnResetClickListener);
        mResetPreference.onBindViewHolder(mHolder);

        assertThat(mButtonView.hasOnClickListeners()).isTrue();
    }
}
