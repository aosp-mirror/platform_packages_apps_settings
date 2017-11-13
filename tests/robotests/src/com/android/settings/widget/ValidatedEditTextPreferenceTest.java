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


import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ValidatedEditTextPreferenceTest {

    @Mock
    private View mView;
    @Mock
    private ValidatedEditTextPreference.Validator mValidator;

    private ValidatedEditTextPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
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
                & (InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT))
                .isNotEqualTo(0);
    }
}
