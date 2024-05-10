/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.widget.Spinner;

import androidx.preference.Preference;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class SpinnerPreferenceTest {

    private Context mContext;
    private SpinnerPreference mSpinnerPreference;

    @Mock private Spinner mMockSpinner;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mSpinnerPreference = new SpinnerPreference(mContext, /* attrs= */ null);
    }

    @Test
    public void constructor_returnExpectedResult() {
        assertThat(mSpinnerPreference.getLayoutResource()).isEqualTo(R.layout.preference_spinner);
    }

    @Test
    public void initializeSpinner_returnExpectedResult() {
        final String[] items = new String[] {"item1", "item2"};
        mSpinnerPreference.initializeSpinner(items, null);
        assertThat(mSpinnerPreference.mItems).isEqualTo(items);
    }

    @Test
    public void onSaveInstanceState_returnExpectedResult() {
        doReturn(1).when(mMockSpinner).getSelectedItemPosition();
        mSpinnerPreference.mSpinner = mMockSpinner;
        SpinnerPreference.SavedState savedState =
                (SpinnerPreference.SavedState) mSpinnerPreference.onSaveInstanceState();
        assertThat(savedState.getSpinnerPosition()).isEqualTo(1);
    }

    @Test
    public void onRestoreInstanceState_returnExpectedResult() {
        SpinnerPreference.SavedState savedState =
                new SpinnerPreference.SavedState(Preference.BaseSavedState.EMPTY_STATE, 2);
        mSpinnerPreference.onRestoreInstanceState(savedState);
        assertThat(mSpinnerPreference.mSavedSpinnerPosition).isEqualTo(2);
    }
}
