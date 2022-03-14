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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.widget.Button;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.widget.LayoutPreference;

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
    private LayoutPreference mLayoutPref;
    @Mock
    private Button mButton;
    @Mock
    private DreamBackend mBackend;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mController = new StartNowPreferenceController(mContext, "key");
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mLayoutPref);
        when(mLayoutPref.findViewById(R.id.dream_start_now_button)).thenReturn(mButton);

        ReflectionHelpers.setField(mController, "mBackend", mBackend);
    }

    @Test
    public void updateState_neverDreaming_buttonShouldDidabled() {
        when(mBackend.getWhenToDreamSetting()).thenReturn(DreamBackend.NEVER);
        mController.displayPreference(mScreen);

        mController.updateState(mLayoutPref);

        verify(mButton).setEnabled(false);
    }

    @Test
    public void updateState_dreamIsAvailable_buttonShouldEnabled() {
        when(mBackend.getWhenToDreamSetting()).thenReturn(DreamBackend.EITHER);
        mController.displayPreference(mScreen);

        mController.updateState(mLayoutPref);

        verify(mButton).setEnabled(true);
    }
}
