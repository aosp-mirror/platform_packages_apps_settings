/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ValidatedEditTextPreferenceTest {

    @Mock
    private View mView;
    @Mock
    private ValidatedEditTextPreference.Validator mValidator;

    private PreferenceViewHolder mViewHolder;
    private ValidatedEditTextPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mViewHolder = spy(PreferenceViewHolder.createInstanceForTests(
                new View(RuntimeEnvironment.application)));
        mPreference = new ValidatedEditTextPreference(RuntimeEnvironment.application);
    }

    @Test
    public void bindDialogView_nullEditText_shouldNotCrash() {
        when(mView.findViewById(android.R.id.edit)).thenReturn(null);
        // should not crash trying to get the EditText text
        mPreference.onBindDialogView(mView);
    }

    @Test
    public void bindDialogView_emptyEditText_shouldNotSetSelection() {
        final String testText = "";
        final EditText editText = spy(new EditText(RuntimeEnvironment.application));
        editText.setText(testText);
        when(mView.findViewById(android.R.id.edit)).thenReturn(editText);

        mPreference.onBindDialogView(mView);

        // no need to setSelection if text was empty
        verify(editText, never()).setSelection(anyInt());
    }

    @Test
    public void bindDialogView_nonemptyEditText_shouldSetSelection() {
        final String testText = "whatever";
        final EditText editText = spy(new EditText(RuntimeEnvironment.application));
        editText.setText(testText);
        when(mView.findViewById(android.R.id.edit)).thenReturn(editText);

        mPreference.onBindDialogView(mView);

        // selection should be set to end of string
        verify(editText).setSelection(testText.length());
    }

    @Test
    public void bindDialogView_hasValidator_shouldBindToEditText() {
        final EditText editText = spy(new EditText(RuntimeEnvironment.application));
        when(mView.findViewById(android.R.id.edit)).thenReturn(editText);

        mPreference.setValidator(mValidator);
        mPreference.onBindDialogView(mView);

        verify(editText).addTextChangedListener(any(TextWatcher.class));
    }

    @Test
    public void bindDialogView_isPassword_shouldSetInputType() {
        final EditText editText = spy(new EditText(RuntimeEnvironment.application));
        when(mView.findViewById(android.R.id.edit)).thenReturn(editText);

        mPreference.setValidator(mValidator);
        mPreference.setIsPassword(true);
        mPreference.onBindDialogView(mView);

        assertThat(editText.getInputType()
                & (InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_CLASS_TEXT))
                .isNotEqualTo(0);
    }

    @Test
    public void bindViewHolder_isPassword_shouldSetInputType() {
        final TextView textView = spy(new TextView(RuntimeEnvironment.application));
        when(mViewHolder.findViewById(android.R.id.summary)).thenReturn(textView);

        mPreference.setIsSummaryPassword(true);
        mPreference.onBindViewHolder(mViewHolder);

        assertThat(textView.getInputType()
                & (InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT))
                .isNotEqualTo(0);
    }

    @Test
    public void bindViewHolder_isNotPassword_shouldNotAutoCorrectText() {
        final TextView textView = spy(new TextView(RuntimeEnvironment.application));
        when(mViewHolder.findViewById(android.R.id.summary)).thenReturn(textView);

        mPreference.setIsSummaryPassword(false);
        mPreference.onBindViewHolder(mViewHolder);

        assertThat(textView.getInputType()).isEqualTo(
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_CLASS_TEXT);
    }
}
