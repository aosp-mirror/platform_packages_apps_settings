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

package com.android.settings.development;

import static com.android.settings.development.AnimatorDurationScalePreferenceController
        .ANIMATOR_DURATION_SCALE_SELECTOR;
import static com.android.settings.development.AnimatorDurationScalePreferenceController
        .DEFAULT_VALUE;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.RemoteException;
import android.view.IWindowManager;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class AnimatorDurationScalePreferenceControllerTest {

    @Mock
    private ListPreference mPreference;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private IWindowManager mWindowManager;

    /**
     * 0: Animation off
     * 1: Animation scale .5x
     * 2: Animation scale 1x
     * 3: Animation scale 1.5x
     * 4: Animation scale 2x
     * 5: Animation scale 5x
     * 6: Animation scale 10x
     */
    private String[] mListValues;
    private String[] mListSummaries;
    private Context mContext;
    private AnimatorDurationScalePreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mListValues = mContext.getResources()
            .getStringArray(R.array.animator_duration_scale_values);
        mListSummaries = mContext.getResources()
            .getStringArray(R.array.animator_duration_scale_entries);
        mController = new AnimatorDurationScalePreferenceController(mContext);
        ReflectionHelpers.setField(mController, "mWindowManager", mWindowManager);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void onPreferenceChange_noValueSet_shouldSetDefault() throws RemoteException {
        mController.onPreferenceChange(mPreference, null /* new value */);

        verify(mWindowManager).setAnimationScale(ANIMATOR_DURATION_SCALE_SELECTOR, DEFAULT_VALUE);
    }

    @Test
    public void onPreferenceChange_option5Selected_shouldSetOption5() throws RemoteException {
        mController.onPreferenceChange(mPreference, mListValues[5]);

        verify(mWindowManager).setAnimationScale(ANIMATOR_DURATION_SCALE_SELECTOR,
                Float.valueOf(mListValues[5]));
    }

    @Test
    public void updateState_option5Set_shouldUpdatePreferenceToOption5() throws RemoteException {
        when(mWindowManager.getAnimationScale(ANIMATOR_DURATION_SCALE_SELECTOR))
            .thenReturn(Float.valueOf(mListValues[5]));

        mController.updateState(mPreference);

        verify(mPreference).setValue(mListValues[5]);
        verify(mPreference).setSummary(mListSummaries[5]);
    }

    @Test
    public void updateState_option3Set_shouldUpdatePreferenceToOption3() throws RemoteException {
        when(mWindowManager.getAnimationScale(ANIMATOR_DURATION_SCALE_SELECTOR))
            .thenReturn(Float.valueOf(mListValues[3]));

        mController.updateState(mPreference);

        verify(mPreference).setValue(mListValues[3]);
        verify(mPreference).setSummary(mListSummaries[3]);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_shouldDisablePreference() throws RemoteException {
        mController.onDeveloperOptionsSwitchDisabled();

        verify(mWindowManager).setAnimationScale(ANIMATOR_DURATION_SCALE_SELECTOR, DEFAULT_VALUE);
        verify(mPreference).setEnabled(false);
    }
}
