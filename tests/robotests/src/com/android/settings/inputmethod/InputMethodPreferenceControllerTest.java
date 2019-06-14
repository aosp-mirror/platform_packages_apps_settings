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
 * limitations under the License
 */

package com.android.settings.inputmethod;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.inputmethod.InputMethodInfo;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.shadow.ShadowInputMethodManagerWithMethodList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowInputMethodManagerWithMethodList.class)
public class InputMethodPreferenceControllerTest {

    private InputMethodPreferenceController mController;
    private Context mContext;
    private PreferenceScreen mScreen;
    private Preference mPreference;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mScreen = spy(new PreferenceScreen(mContext, null));
        mPreference = new Preference(mContext);
        mController = new InputMethodPreferenceController(mContext, "key");

        when(mScreen.getPreferenceManager()).thenReturn(mock(PreferenceManager.class));
        when(mScreen.getContext()).thenReturn(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);

        mController.displayPreference(mScreen);
    }

    @Test
    public void onStart_NoInputMethod_shouldHaveOnePreference() {
        mController.onStart();

        assertThat(mScreen.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    public void onStart_hasInputMethod_shouldHaveCorrectPreferences() {
        final List<InputMethodInfo> imis = new ArrayList<>();
        imis.add(mock(InputMethodInfo.class));
        imis.add(mock(InputMethodInfo.class));
        when(imis.get(0).getPackageName()).thenReturn("name1");
        when(imis.get(1).getPackageName()).thenReturn("name2");
        ShadowInputMethodManagerWithMethodList.getShadow().setEnabledInputMethodList(imis);

        mController.onStart();

        assertThat(mScreen.getPreferenceCount()).isEqualTo(3);
    }
}
