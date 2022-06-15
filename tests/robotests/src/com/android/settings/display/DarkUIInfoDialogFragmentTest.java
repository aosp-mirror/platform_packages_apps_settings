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

package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.provider.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class DarkUIInfoDialogFragmentTest {
    private DarkUIInfoDialogFragment mFragment;
    @Mock
    private DialogInterface dialog;


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mFragment = spy(new DarkUIInfoDialogFragment());
    }

    @Test
    public void dialogDismissedOnConfirmation() {
        doReturn(RuntimeEnvironment.application).when(mFragment).getContext();
        SharedPreferences prefs = RuntimeEnvironment.application.getSharedPreferences(
                DarkUIPreferenceController.DARK_MODE_PREFS,
                Context.MODE_PRIVATE);
        assertThat(prefs.getBoolean(DarkUIPreferenceController.PREF_DARK_MODE_DIALOG_SEEN, false))
                .isFalse();
        mFragment.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
        verify(dialog, times(1)).dismiss();
        assertThat(Settings.Secure.getInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Secure.DARK_MODE_DIALOG_SEEN, -1)).isEqualTo(1);
    }
}
