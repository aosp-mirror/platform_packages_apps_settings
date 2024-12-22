/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.settings.inputmethod;

import static android.view.PointerIcon.POINTER_ICON_VECTOR_STYLE_STROKE_BLACK;
import static android.view.PointerIcon.POINTER_ICON_VECTOR_STYLE_STROKE_WHITE;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.provider.Settings;
import android.view.View;
import android.widget.RadioButton;

import androidx.preference.PreferenceDataStore;
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

/** Tests for {@link PointerStrokeStylePreference} */
@RunWith(RobolectricTestRunner.class)
public class PointerStrokeStylePreferenceTest {
    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    PreferenceDataStore mPreferenceDataStore;

    private Context mContext;
    private PreferenceViewHolder mViewHolder;
    private PointerStrokeStylePreference mPreference;


    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mPreference = new PointerStrokeStylePreference(mContext, null);
    }

    @Test
    public void onBindViewHolder_getCurrentStrokeStyleFromDataStore() {
        final View view = spy(View.inflate(mContext, mPreference.getLayoutResource(), null));
        mViewHolder = PreferenceViewHolder.createInstanceForTests(view);
        mPreference.setPreferenceDataStore(mPreferenceDataStore);

        mPreference.onBindViewHolder(mViewHolder);

        verify(mPreferenceDataStore).getInt(Settings.System.POINTER_STROKE_STYLE,
                POINTER_ICON_VECTOR_STYLE_STROKE_WHITE);
    }

    @Test
    public void setChecked_radioButtonUpdatesDataStore() {
        final View view = spy(View.inflate(mContext, mPreference.getLayoutResource(), null));
        mViewHolder = PreferenceViewHolder.createInstanceForTests(view);
        mPreference.setPreferenceDataStore(mPreferenceDataStore);
        RadioButton radioButton = (RadioButton) view.findViewById(R.id.stroke_style_black);
        mPreference.onBindViewHolder(mViewHolder);

        radioButton.setChecked(true);

        verify(mPreferenceDataStore).getInt(Settings.System.POINTER_STROKE_STYLE,
                POINTER_ICON_VECTOR_STYLE_STROKE_WHITE);
        verify(mPreferenceDataStore).putInt(Settings.System.POINTER_STROKE_STYLE,
                POINTER_ICON_VECTOR_STYLE_STROKE_BLACK);
    }
}
