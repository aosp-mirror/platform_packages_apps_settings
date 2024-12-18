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

package com.android.settings.connecteddevice.audiosharing;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AudioSharingPasswordPreferenceTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    private static final String EDIT_TEXT_CONTENT = "text";
    private Context mContext;
    private AudioSharingPasswordPreference mPreference;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
        mPreference = new AudioSharingPasswordPreference(mContext, null);
    }

    @Test
    public void onBindDialogView_correctLayout() {
        View view =
                LayoutInflater.from(mContext).inflate(R.layout.audio_sharing_password_dialog, null);
        mPreference.onBindDialogView(view);

        var editText = view.findViewById(android.R.id.edit);
        var checkBox = view.findViewById(R.id.audio_sharing_stream_password_checkbox);
        var dialogMessage = view.findViewById(android.R.id.message);

        assertThat(editText).isNotNull();
        assertThat(checkBox).isNotNull();
        assertThat(dialogMessage).isNotNull();
    }

    @Test
    public void setEditable_true() {
        View view =
                LayoutInflater.from(mContext).inflate(R.layout.audio_sharing_password_dialog, null);
        mPreference.onBindDialogView(view);

        var editText = view.findViewById(android.R.id.edit);
        var checkBox = view.findViewById(R.id.audio_sharing_stream_password_checkbox);
        var dialogMessage = view.findViewById(android.R.id.message);

        mPreference.setEditable(true);

        assertThat(editText).isNotNull();
        assertThat(editText.isEnabled()).isTrue();
        assertThat(editText.getAlpha()).isEqualTo(1.0f);
        assertThat(checkBox).isNotNull();
        assertThat(checkBox.isEnabled()).isTrue();
        assertThat(dialogMessage).isNotNull();
        assertThat(dialogMessage.getVisibility()).isEqualTo(GONE);
    }

    @Test
    public void setEditable_false() {
        View view =
                LayoutInflater.from(mContext).inflate(R.layout.audio_sharing_password_dialog, null);
        mPreference.onBindDialogView(view);

        var editText = view.findViewById(android.R.id.edit);
        var checkBox = view.findViewById(R.id.audio_sharing_stream_password_checkbox);
        var dialogMessage = view.findViewById(android.R.id.message);

        mPreference.setEditable(false);

        assertThat(editText).isNotNull();
        assertThat(editText.isEnabled()).isFalse();
        assertThat(editText.getAlpha()).isLessThan(1.0f);
        assertThat(checkBox).isNotNull();
        assertThat(checkBox.isEnabled()).isFalse();
        assertThat(dialogMessage).isNotNull();
        assertThat(dialogMessage.getVisibility()).isEqualTo(VISIBLE);
    }

    @Test
    public void setChecked_true() {
        View view =
                LayoutInflater.from(mContext).inflate(R.layout.audio_sharing_password_dialog, null);
        mPreference.onBindDialogView(view);

        CheckBox checkBox = view.findViewById(R.id.audio_sharing_stream_password_checkbox);

        mPreference.setChecked(true);

        assertThat(checkBox).isNotNull();
        assertThat(checkBox.isChecked()).isTrue();
    }

    @Test
    public void setChecked_false() {
        View view =
                LayoutInflater.from(mContext).inflate(R.layout.audio_sharing_password_dialog, null);
        mPreference.onBindDialogView(view);

        CheckBox checkBox = view.findViewById(R.id.audio_sharing_stream_password_checkbox);

        mPreference.setChecked(false);

        assertThat(checkBox).isNotNull();
        assertThat(checkBox.isChecked()).isFalse();
    }

    @Test
    public void onDialogEventListener_onClick_positiveButton() {
        AudioSharingPasswordPreference.OnDialogEventListener listener =
                mock(AudioSharingPasswordPreference.OnDialogEventListener.class);
        mPreference.setOnDialogEventListener(listener);
        View view =
                LayoutInflater.from(mContext).inflate(R.layout.audio_sharing_password_dialog, null);
        mPreference.onBindDialogView(view);

        EditText editText = view.findViewById(android.R.id.edit);
        assertThat(editText).isNotNull();
        editText.setText(EDIT_TEXT_CONTENT);

        mPreference.onClick(mock(DialogInterface.class), DialogInterface.BUTTON_POSITIVE);

        verify(listener).onBindDialogView();
        verify(listener).onPreferenceDataChanged(eq(EDIT_TEXT_CONTENT), anyBoolean());
    }

    @Test
    public void onDialogEventListener_onClick_negativeButton_doNothing() {
        AudioSharingPasswordPreference.OnDialogEventListener listener =
                mock(AudioSharingPasswordPreference.OnDialogEventListener.class);
        mPreference.setOnDialogEventListener(listener);
        View view =
                LayoutInflater.from(mContext).inflate(R.layout.audio_sharing_password_dialog, null);
        mPreference.onBindDialogView(view);

        EditText editText = view.findViewById(android.R.id.edit);
        assertThat(editText).isNotNull();
        editText.setText(EDIT_TEXT_CONTENT);

        mPreference.onClick(mock(DialogInterface.class), DialogInterface.BUTTON_NEGATIVE);

        verify(listener).onBindDialogView();
        verify(listener, never()).onPreferenceDataChanged(anyString(), anyBoolean());
    }

    @Test
    public void onPrepareDialogBuilder_editable_doNothing() {
        View view =
                LayoutInflater.from(mContext).inflate(R.layout.audio_sharing_password_dialog, null);
        mPreference.onBindDialogView(view);
        mPreference.setEditable(true);

        var dialogBuilder = mock(AlertDialog.Builder.class);
        mPreference.onPrepareDialogBuilder(
                dialogBuilder, mock(DialogInterface.OnClickListener.class));

        verify(dialogBuilder, never()).setPositiveButton(any(), any());
    }

    @Test
    public void onPrepareDialogBuilder_notEditable_disableButton() {
        View view =
                LayoutInflater.from(mContext).inflate(R.layout.audio_sharing_password_dialog, null);
        mPreference.onBindDialogView(view);
        mPreference.setEditable(false);

        var dialogBuilder = mock(AlertDialog.Builder.class);
        mPreference.onPrepareDialogBuilder(
                dialogBuilder, mock(DialogInterface.OnClickListener.class));

        verify(dialogBuilder).setPositiveButton(any(), any());
    }
}
