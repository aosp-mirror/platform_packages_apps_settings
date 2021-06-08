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

package com.android.settings.widget;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import com.google.android.material.card.MaterialCardView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class CardPreferenceTest {

    private Context mContext;
    private CardPreference mCardPreference;
    @Mock
    private PreferenceViewHolder mPreferenceViewHolder;
    @Mock
    private MaterialCardView mCardView;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mContext.setTheme(R.style.SettingsPreferenceTheme);
        mCardPreference = new CardPreference(mContext);
        mPreferenceViewHolder = spy(PreferenceViewHolder.createInstanceForTests(mock(View.class)));
        doReturn(mCardView).when(mPreferenceViewHolder).findViewById(R.id.container);
    }

    @Test
    public void getLayoutResource() {
        assertThat(mCardPreference.getLayoutResource()).isEqualTo(R.layout.card_preference_layout);
    }

    @Test
    public void setCardBackgroundColor_shouldUseCorrectColor() {
        final @ColorInt int testColor = 0xABCDEF;

        mCardPreference.setCardBackgroundColor(testColor);
        mCardPreference.onBindViewHolder(mPreferenceViewHolder);

        verify(mCardView).setCardBackgroundColor(testColor);
    }
}
