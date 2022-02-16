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

package com.android.settings.dream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.PreferenceScreen;

import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.widget.MainSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class StartNowPreferenceControllerTest {

    private StartNowPreferenceController mController;
    private Context mContext;

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private MainSwitchPreference mPref;
    @Mock
    private DreamBackend mBackend;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mController = new StartNowPreferenceController(mContext, "key");
        mPref = mock(MainSwitchPreference.class);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPref);

        ReflectionHelpers.setField(mController, "mBackend", mBackend);
    }

    @Test
    public void displayPreference_shouldAddOnSwitchChangeListener() {
        mController.displayPreference(mScreen);

        verify(mPref).addOnSwitchChangeListener(mController);
    }

    @Test
    public void updateState_neverDreaming_preferenceShouldDidabled() {
        when(mBackend.getWhenToDreamSetting()).thenReturn(DreamBackend.NEVER);
        mController.displayPreference(mScreen);

        mController.updateState(mPref);

        verify(mPref).setEnabled(false);
    }

    @Test
    public void updateState_dreamIsAvailable_preferenceShouldEnabled() {
        when(mBackend.getWhenToDreamSetting()).thenReturn(DreamBackend.EITHER);
        mController.displayPreference(mScreen);

        mController.updateState(mPref);

        verify(mPref).setEnabled(true);
    }
}
