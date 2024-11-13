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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.bluetooth.HearingDeviceInputRoutingPreference.InputRoutingValue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link HearingDeviceInputRoutingPreference}. */
@RunWith(RobolectricTestRunner.class)
public class HearingDeviceInputRoutingPreferenceTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private HearingDeviceInputRoutingPreference mPreference;
    private TestInputRoutingCallback mTestInputRoutingCallback;
    private View mDialogView;

    @Before
    public void setup() {
        mDialogView = LayoutInflater.from(mContext).inflate(
                R.layout.hearing_device_input_routing_dialog, null);
        mTestInputRoutingCallback = spy(new TestInputRoutingCallback());
        mPreference = new HearingDeviceInputRoutingPreference(mContext);
    }

    @Test
    public void onClick_checkToBuiltinMic_callbackWithBuiltinSpeaker() {
        mPreference.setChecked(InputRoutingValue.HEARING_DEVICE);
        mPreference.setInputRoutingCallback(mTestInputRoutingCallback);
        mPreference.onBindDialogView(mDialogView);
        RadioGroup radioGroup =  mDialogView.requireViewById(R.id.input_routing_group);
        Dialog dialog = mPreference.getDialog();

        radioGroup.check(R.id.input_from_builtin_mic);
        mPreference.onClick(dialog, DialogInterface.BUTTON_POSITIVE);

        verify(mTestInputRoutingCallback).onInputRoutingUpdated(InputRoutingValue.BUILTIN_MIC);
    }

    @Test
    public void setChecked_checkNoChange_noCallback() {
        mPreference.setChecked(InputRoutingValue.HEARING_DEVICE);
        mPreference.setInputRoutingCallback(mTestInputRoutingCallback);
        mPreference.onBindDialogView(mDialogView);
        Dialog dialog = mPreference.getDialog();

        mPreference.setChecked(InputRoutingValue.HEARING_DEVICE);
        mPreference.onClick(dialog, DialogInterface.BUTTON_POSITIVE);

        verify(mTestInputRoutingCallback, never()).onInputRoutingUpdated(anyInt());
    }

    @Test
    public void setChecked_builtinMic_expectedSummary() {
        mPreference.setChecked(InputRoutingValue.BUILTIN_MIC);

        assertThat(mPreference.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.bluetooth_hearing_device_input_routing_builtin_option));
    }

    @Test
    public void setChecked_hearingDevice_expectedSummary() {
        mPreference.setChecked(InputRoutingValue.HEARING_DEVICE);

        assertThat(mPreference.getSummary().toString()).isEqualTo(mContext.getString(
                R.string.bluetooth_hearing_device_input_routing_hearing_device_option));
    }

    private static class TestInputRoutingCallback implements
            HearingDeviceInputRoutingPreference.InputRoutingCallback {

        @Override
        public void onInputRoutingUpdated(int selectedInputRoutingUiValue) {}
    }
}
