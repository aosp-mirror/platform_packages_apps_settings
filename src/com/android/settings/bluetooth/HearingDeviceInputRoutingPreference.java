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

package com.android.settings.bluetooth;

import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RadioGroup;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settingslib.CustomDialogPreferenceCompat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Preference for controlling the input routing for hearing device.
 *
 * <p> This preference displays a dialog that allows users to choose which input device that want to
 * use when using this hearing device.
 */
public class HearingDeviceInputRoutingPreference extends CustomDialogPreferenceCompat {

    /**
     * Annotations for possible input routing UI for this hearing device input routing preference.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            InputRoutingValue.HEARING_DEVICE,
            InputRoutingValue.BUILTIN_MIC
    })
    public @interface InputRoutingValue {
        int HEARING_DEVICE = 0;
        int BUILTIN_MIC = 1;
    }

    private static final int INVALID_ID = -1;
    private final Context mContext;
    private final int mFromHearingDeviceButtonId = R.id.input_from_hearing_device;
    private final int mFromBuiltinMicButtonId = R.id.input_from_builtin_mic;

    @Nullable
    private RadioGroup mInputRoutingGroup;
    @Nullable
    private InputRoutingCallback mCallback;
    // Default value is hearing device as input
    @InputRoutingValue
    private int mSelectedInputRoutingValue = InputRoutingValue.HEARING_DEVICE;


    public HearingDeviceInputRoutingPreference(@NonNull Context context) {
        this(context, null);
    }

    public HearingDeviceInputRoutingPreference(@NonNull Context context,
            @Nullable AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        setDialogTitle(R.string.bluetooth_hearing_device_input_routing_dialog_title);
        setDialogLayoutResource(R.layout.hearing_device_input_routing_dialog);
        setNegativeButtonText(R.string.cancel);
        setPositiveButtonText(R.string.done_button);
    }

    /**
     * Sets the callback to receive input routing updates.
     */
    public void setInputRoutingCallback(@NonNull InputRoutingCallback callback) {
        mCallback = callback;
    }

    /**
     * Sets the {@link InputRoutingValue} value to determine which radio button should be checked,
     * and also update summary accordingly.
     *
     * @param inputRoutingValue The input routing value.
     */
    public void setChecked(@InputRoutingValue int inputRoutingValue) {
        mSelectedInputRoutingValue = inputRoutingValue;
        setSummary(getSummary());
    }

    @Override
    protected void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            int prevBtnId = getRadioButtonId(mSelectedInputRoutingValue);
            int curBtnId = Objects.requireNonNull(mInputRoutingGroup).getCheckedRadioButtonId();
            if (prevBtnId == curBtnId) {
                return;
            }

            setChecked(getSelectedInputRoutingValue());
            if (mCallback != null) {
                mCallback.onInputRoutingUpdated(mSelectedInputRoutingValue);
            }
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mInputRoutingGroup = view.requireViewById(R.id.input_routing_group);
        mInputRoutingGroup.check(getRadioButtonId(mSelectedInputRoutingValue));
    }

    @Nullable
    @Override
    public CharSequence getSummary() {
        return switch (mSelectedInputRoutingValue) {
            case InputRoutingValue.HEARING_DEVICE -> mContext.getResources().getString(
                    R.string.bluetooth_hearing_device_input_routing_hearing_device_option);
            case InputRoutingValue.BUILTIN_MIC -> mContext.getResources().getString(
                    R.string.bluetooth_hearing_device_input_routing_builtin_option);
            default -> null;
        };
    }

    private int getRadioButtonId(@InputRoutingValue int inputRoutingValue) {
        return switch (inputRoutingValue) {
            case InputRoutingValue.HEARING_DEVICE -> mFromHearingDeviceButtonId;
            case InputRoutingValue.BUILTIN_MIC -> mFromBuiltinMicButtonId;
            default -> INVALID_ID;
        };
    }

    @InputRoutingValue
    private int getSelectedInputRoutingValue() {
        int checkedId = Objects.requireNonNull(mInputRoutingGroup).getCheckedRadioButtonId();
        if (checkedId == mFromBuiltinMicButtonId) {
            return InputRoutingValue.BUILTIN_MIC;
        } else {
            // Should always return default value hearing device as input if something error
            // happens.
            return InputRoutingValue.HEARING_DEVICE;
        }
    }

    /**
     * Callback to be invoked when input routing changes.
     */
    public interface InputRoutingCallback {

        /**
         * Called when the positive button is clicked and input routing is changed.
         *
         * @param selectedInputRoutingValue The selected input routing value.
         */
        void onInputRoutingUpdated(@InputRoutingValue int selectedInputRoutingValue);
    }
}
