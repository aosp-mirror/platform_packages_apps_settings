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

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settingslib.CustomDialogPreferenceCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class for Bluetooth A2DP config preference in developer option.
 */
public abstract class BaseBluetoothDialogPreference extends CustomDialogPreferenceCompat implements
        RadioGroup.OnCheckedChangeListener{
    private static final String TAG = "BaseBluetoothDlgPref";

    protected List<Integer> mRadioButtonIds = new ArrayList<>();
    protected List<String> mRadioButtonStrings = new ArrayList<>();
    protected List<String> mSummaryStrings = new ArrayList<>();

    private Callback mCallback;

    public BaseBluetoothDialogPreference(Context context) {
        super(context);
    }

    public BaseBluetoothDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseBluetoothDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BaseBluetoothDialogPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        if (mCallback == null) {
            Log.e(TAG, "Unable to show dialog by the callback is null");
            return;
        }
        if (mRadioButtonStrings.size() != mRadioButtonIds.size()) {
            Log.e(TAG, "Unable to show dialog by the view and string size are not matched");
            return;
        }
        final int currentIndex = mCallback.getCurrentConfigIndex();
        if (currentIndex < 0 || currentIndex >= mRadioButtonIds.size()) {
            Log.e(TAG, "Unable to show dialog by the incorrect index: " + currentIndex);
            return;
        }
        // Initial radio button group
        final RadioGroup radioGroup = view.findViewById(getRadioButtonGroupId());
        if (radioGroup == null) {
            Log.e(TAG, "Unable to show dialog by no radio button group: "
                    + getRadioButtonGroupId());
            return;
        }
        radioGroup.check(mRadioButtonIds.get(currentIndex));
        radioGroup.setOnCheckedChangeListener(this);
        // Initial radio button
        final List<Integer> selectableIndex = mCallback.getSelectableIndex();
        RadioButton radioButton;
        for (int i = 0; i < mRadioButtonStrings.size(); i++) {
            radioButton = view.findViewById(mRadioButtonIds.get(i));
            if (radioButton == null) {
                Log.e(TAG, "Unable to show dialog by no radio button:" + mRadioButtonIds.get(i));
                return;
            }
            radioButton.setText(mRadioButtonStrings.get(i));
            radioButton.setEnabled(selectableIndex.contains(i));
        }
        // Initial help information text view
        final TextView helpTextView = view.findViewById(R.id.bluetooth_audio_codec_help_info);
        if (selectableIndex.size() == mRadioButtonIds.size()) {
            // View will be invisible when all options are enabled.
            helpTextView.setVisibility(View.GONE);
        } else {
            helpTextView.setText(
                    com.android.settingslib.R.string.bluetooth_select_a2dp_codec_type_help_info);
            helpTextView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (mCallback == null) {
            Log.e(TAG, "Callback is null");
            return;
        }
        mCallback.onIndexUpdated(mRadioButtonIds.indexOf(checkedId));
        getDialog().dismiss();
    }

    /**
     * Method to set callback.
     */
    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    /**
     * Method to get summary strings by index.
     */
    protected String generateSummary(int index) {
        if (index > mSummaryStrings.size()) {
            Log.e(TAG, "Unable to get summary of " + index + ". Size is " + mSummaryStrings.size());
            return null;
        }
        return index == getDefaultIndex() ? mSummaryStrings.get(getDefaultIndex()) :
                String.format(getContext().getResources().getString(
                        com.android.settingslib.R
                                .string.bluetooth_select_a2dp_codec_streaming_label),
                        mSummaryStrings.get(index));
    }

    /**
     * Method to get default index.
     */
    protected int getDefaultIndex() {
        return 0;
    }

    /**
     * Method to get radio button group id.
     */
    protected abstract int getRadioButtonGroupId();

    /**
     * Callback interface for this class to manipulate data from controller.
     */
    public interface Callback {
        /**
         * Method to get current Bluetooth A2DP config index.
         */
        int getCurrentConfigIndex();
        /**
         * Method to get selectable config index which means supported by phone and device.
         *
         * @return the available {@link List} of the Bluetooth A2DP config.
         */
        List<Integer> getSelectableIndex();
        /**
         * Method to notify controller when user changes config.
         *
         * @param index for the selected index.
         */
        void onIndexUpdated(int index);
    }
}
