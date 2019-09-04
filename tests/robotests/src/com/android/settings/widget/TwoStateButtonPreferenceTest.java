/*
 * Copyright (C) 2018 The Android Open Source Project
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
 *
 */

package com.android.settings.widget;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.view.View;
import android.widget.Button;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class TwoStateButtonPreferenceTest {

    private TwoStateButtonPreference mPreference;
    private Context mContext;
    private Button mButtonOn;
    private Button mButtonOff;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mPreference = spy(new TwoStateButtonPreference(mContext, null /* AttributeSet */));
        mButtonOn = new Button(mContext);
        mButtonOn.setId(R.id.state_on_button);
        mButtonOff = new Button(mContext);
        mButtonOff.setId(R.id.state_off_button);
        ReflectionHelpers.setField(mPreference, "mButtonOn", mButtonOn);
        ReflectionHelpers.setField(mPreference, "mButtonOff", mButtonOff);
    }

    @Test
    public void testSetButtonVisibility_stateOn_onlyShowButtonOn() {
        mPreference.setChecked(true /* stateOn */);

        assertThat(mButtonOn.getVisibility()).isEqualTo(View.GONE);
        assertThat(mButtonOff.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void testSetButtonVisibility_stateOff_onlyShowButtonOff() {
        mPreference.setChecked(false /* stateOn */);

        assertThat(mButtonOn.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(mButtonOff.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testSetButtonEnabled_enabled_buttonEnabled() {
        mPreference.setButtonEnabled(true /* enabled */);

        assertThat(mButtonOn.isEnabled()).isTrue();
        assertThat(mButtonOff.isEnabled()).isTrue();
    }

    @Test
    public void testSetButtonEnabled_disabled_buttonDisabled() {
        mPreference.setButtonEnabled(false /* enabled */);

        assertThat(mButtonOn.isEnabled()).isFalse();
        assertThat(mButtonOff.isEnabled()).isFalse();
    }

    @Test
    public void onClick_shouldPropagateChangeToListener() {
        mPreference.onClick(mButtonOn);
        verify(mPreference).callChangeListener(true);

        mPreference.onClick(mButtonOff);
        verify(mPreference).callChangeListener(false);
    }
}
