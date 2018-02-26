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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.support.v7.preference.PreferenceScreen;
import android.view.View;
import android.widget.Button;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class TwoStateButtonPreferenceControllerTest {

    private static final String KEY = "pref_key";

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private TwoStateButtonPreference mPreference;
    private TwoStateButtonPreferenceController mController;
    private Context mContext;
    private Button mButtonOn;
    private Button mButtonOff;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mPreference).when(mPreferenceScreen).findPreference(anyString());
        mButtonOn = new Button(mContext);
        doReturn(mButtonOn).when(mPreference).getStateOnButton();
        mButtonOff = new Button(mContext);
        doReturn(mButtonOff).when(mPreference).getStateOffButton();

        mController = new TestButtonsPreferenceController(mContext, KEY);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void testSetButtonVisibility_stateOn_onlyShowButtonOn() {
        mController.setButtonVisibility(true /* stateOn */);

        assertThat(mButtonOn.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(mButtonOff.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testSetButtonVisibility_stateOff_onlyShowButtonOff() {
        mController.setButtonVisibility(false /* stateOn */);

        assertThat(mButtonOn.getVisibility()).isEqualTo(View.GONE);
        assertThat(mButtonOff.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void testSetButtonEnabled_enabled_buttonEnabled() {
        mController.setButtonEnabled(true /* enabled */);

        assertThat(mButtonOn.isEnabled()).isTrue();
        assertThat(mButtonOff.isEnabled()).isTrue();
    }

    @Test
    public void testSetButtonEnabled_disabled_buttonDisabled() {
        mController.setButtonEnabled(false /* enabled */);

        assertThat(mButtonOn.isEnabled()).isFalse();
        assertThat(mButtonOff.isEnabled()).isFalse();
    }

    /**
     * Controller to test methods in {@link TwoStateButtonPreferenceController}
     */
    public static class TestButtonsPreferenceController
        extends TwoStateButtonPreferenceController {

        TestButtonsPreferenceController(Context context, String key) {
            super(context, key);
        }

        @Override
        public void onButtonClicked(boolean stateOn) {
            //do nothing
        }

        @Override
        public int getAvailabilityStatus() {
            return AVAILABLE;
        }
    }
}
