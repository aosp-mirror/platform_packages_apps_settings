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

import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.widget.ValidatedEditTextPreference;
import com.android.settingslib.utils.ColorUtil;

public class AudioSharingPasswordPreference extends ValidatedEditTextPreference {
    private static final String TAG = "AudioSharingPasswordPreference";
    @Nullable private OnDialogEventListener mOnDialogEventListener;
    @Nullable private EditText mEditText;
    @Nullable private CheckBox mCheckBox;
    @Nullable private View mDialogMessage;
    private boolean mEditable = true;

    interface OnDialogEventListener {
        void onBindDialogView();

        void onPreferenceDataChanged(@NonNull String editTextValue, boolean checkBoxValue);
    }

    void setOnDialogEventListener(OnDialogEventListener listener) {
        mOnDialogEventListener = listener;
    }

    public AudioSharingPasswordPreference(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public AudioSharingPasswordPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AudioSharingPasswordPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AudioSharingPasswordPreference(Context context) {
        super(context);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mEditText = view.findViewById(android.R.id.edit);
        mCheckBox = view.findViewById(R.id.audio_sharing_stream_password_checkbox);
        mDialogMessage = view.findViewById(android.R.id.message);

        if (mEditText == null || mCheckBox == null || mDialogMessage == null) {
            Log.w(TAG, "onBindDialogView() : Invalid layout");
            return;
        }

        mCheckBox.setOnCheckedChangeListener((unused, checked) -> setEditTextEnabled(!checked));
        if (mOnDialogEventListener != null) {
            mOnDialogEventListener.onBindDialogView();
        }
    }

    @Override
    protected void onPrepareDialogBuilder(
            AlertDialog.Builder builder, DialogInterface.OnClickListener listener) {
        if (!mEditable) {
            builder.setPositiveButton(null, null);
        }
    }

    @Override
    protected void onClick(DialogInterface dialog, int which) {
        if (mEditText == null || mCheckBox == null) {
            Log.w(TAG, "onClick() : Invalid layout");
            return;
        }

        if (mOnDialogEventListener != null
                && which == DialogInterface.BUTTON_POSITIVE
                && mEditText.getText() != null) {
            mOnDialogEventListener.onPreferenceDataChanged(
                    mEditText.getText().toString(), mCheckBox.isChecked());
        }
    }

    void setEditable(boolean editable) {
        if (mEditText == null || mCheckBox == null || mDialogMessage == null) {
            Log.w(TAG, "setEditable() : Invalid layout");
            return;
        }
        mEditable = editable;
        setEditTextEnabled(editable);
        mCheckBox.setEnabled(editable);
        mDialogMessage.setVisibility(editable ? GONE : VISIBLE);
    }

    void setChecked(boolean checked) {
        if (mCheckBox == null) {
            Log.w(TAG, "setChecked() : Invalid layout");
            return;
        }
        mCheckBox.setChecked(checked);
    }

    private void setEditTextEnabled(boolean enabled) {
        if (mEditText == null) {
            Log.w(TAG, "setEditTextEnabled() : Invalid layout");
            return;
        }
        mEditText.setEnabled(enabled);
        mEditText.setAlpha(enabled ? 1.0f : ColorUtil.getDisabledAlpha(getContext()));
    }
}
