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

package com.android.settings.development.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BaseBluetoothDialogPreferenceTest {

    private static final int ID1 = 99;
    private static final int ID2 = 100;
    private static final int RADIOGROUP_ID = 101;
    private static final int TEXT_VIEW_ID = R.id.bluetooth_audio_codec_help_info;
    private static final String BUTTON1 = "Test button1";
    private static final String BUTTON2 = "Test button2";
    private static final String SUMMARY1 = "Test summary1";
    private static final String SUMMARY2 = "Test summary2";

    @Mock
    private BaseBluetoothDialogPreference.Callback mCallback;
    @Mock
    private Dialog mDialog;
    @Mock
    private View mView;

    private BaseBluetoothDialogPreference mPreference;
    private Context mContext;
    private RadioButton mRadioButton1;
    private RadioButton mRadioButton2;
    private RadioGroup mRadioGroup;
    private TextView mTextView;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPreference = spy(new BaseBluetoothDialogPreferenceImpl(mContext));
        mRadioButton1 = new RadioButton(mContext);
        mRadioButton1.setId(ID1);
        mRadioButton2 = new RadioButton(mContext);
        mRadioButton2.setId(ID2);
        mRadioGroup = new RadioGroup(mContext);
        mRadioGroup.addView(mRadioButton1);
        mRadioGroup.addView(mRadioButton2);
        mTextView = new TextView(mContext);
        mPreference.mRadioButtonIds.add(ID1);
        mPreference.mRadioButtonIds.add(ID2);
        mPreference.mRadioButtonStrings.add(BUTTON1);
        mPreference.mRadioButtonStrings.add(BUTTON2);
        mPreference.mSummaryStrings.add(SUMMARY1);
        mPreference.mSummaryStrings.add(SUMMARY2);
        mPreference.setCallback(mCallback);
        when(mView.findViewById(mPreference.getRadioButtonGroupId())).thenReturn(mRadioGroup);
    }

    @Test
    public void onBindDialogView_checkRadioButtonsSelection() {
        when(mCallback.getCurrentConfigIndex()).thenReturn(1);

        assertThat(mRadioGroup.getCheckedRadioButtonId()).isNotEqualTo(ID2);
        mPreference.onBindDialogView(mView);

        assertThat(mRadioGroup.getCheckedRadioButtonId()).isEqualTo(ID2);
    }

    @Test
    public void onBindDialogView_checkRadioButtonsText() {
        when(mView.findViewById(ID1)).thenReturn(mRadioButton1);
        when(mView.findViewById(ID2)).thenReturn(mRadioButton2);

        assertThat(mRadioButton1.getText()).isNotEqualTo(BUTTON1);
        assertThat(mRadioButton2.getText()).isNotEqualTo(BUTTON2);
        mPreference.onBindDialogView(mView);

        assertThat(mRadioButton1.getText()).isEqualTo(BUTTON1);
        assertThat(mRadioButton2.getText()).isEqualTo(BUTTON2);
    }

    @Test
    public void onBindDialogView_checkRadioButtonsState() {
        when(mView.findViewById(ID1)).thenReturn(mRadioButton1);
        when(mView.findViewById(ID2)).thenReturn(mRadioButton2);
        List<Integer> indexes = new ArrayList<>();
        indexes.add(0);
        when(mCallback.getSelectableIndex()).thenReturn(indexes);

        assertThat(mRadioButton1.isEnabled()).isTrue();
        assertThat(mRadioButton2.isEnabled()).isTrue();
        mPreference.onBindDialogView(mView);

        assertThat(mRadioButton1.isEnabled()).isTrue();
        assertThat(mRadioButton2.isEnabled()).isFalse();
    }

    @Test
    public void onBindDialogView_allButtonsEnabled_hideHelpText() {
        when(mView.findViewById(ID1)).thenReturn(mRadioButton1);
        when(mView.findViewById(ID2)).thenReturn(mRadioButton2);
        when(mView.findViewById(TEXT_VIEW_ID)).thenReturn(mTextView);
        List<Integer> indexes = new ArrayList<>();
        indexes.add(0);
        indexes.add(1);
        when(mCallback.getSelectableIndex()).thenReturn(indexes);

        assertThat(mTextView.getVisibility()).isEqualTo(View.VISIBLE);
        mPreference.onBindDialogView(mView);
        assertThat(mTextView.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void onBindDialogView_buttonDisabled_showHelpText() {
        when(mView.findViewById(ID1)).thenReturn(mRadioButton1);
        when(mView.findViewById(ID2)).thenReturn(mRadioButton2);
        when(mView.findViewById(TEXT_VIEW_ID)).thenReturn(mTextView);
        List<Integer> indexes = new ArrayList<>();
        indexes.add(0);
        when(mCallback.getSelectableIndex()).thenReturn(indexes);

        mPreference.onBindDialogView(mView);
        assertThat(mTextView.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void onCheckedChanged_verifyIndex() {
        when(mPreference.getDialog()).thenReturn(mDialog);

        mPreference.onCheckedChanged(mRadioGroup, ID2);
        verify(mCallback).onIndexUpdated(1);
    }

    @Test
    public void generateSummary_checkString() {
        final String summary = String.format(mContext.getResources().getString(
                R.string.bluetooth_select_a2dp_codec_streaming_label), SUMMARY2);

        assertThat(mPreference.generateSummary(1)).isEqualTo(summary);
    }


    private static class BaseBluetoothDialogPreferenceImpl extends BaseBluetoothDialogPreference {

        private BaseBluetoothDialogPreferenceImpl(Context context) {
            super(context);
        }

        @Override
        protected int getRadioButtonGroupId() {
            return RADIOGROUP_ID;
        }
    }
}
