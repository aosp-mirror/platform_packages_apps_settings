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

package com.android.settings.notification.modes;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.app.Flags;
import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.widget.Button;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public class ZenModeEditDonePreferenceControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private ZenModeEditDonePreferenceController mController;
    private LayoutPreference mPreference;
    private Button mButton;
    @Mock private Runnable mConfirmSave;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        Context context = RuntimeEnvironment.application;
        PreferenceManager preferenceManager = new PreferenceManager(context);
        PreferenceScreen preferenceScreen = preferenceManager.inflateFromResource(context,
                R.xml.modes_edit_name_icon, null);
        mPreference = preferenceScreen.findPreference("done");

        mController = new ZenModeEditDonePreferenceController(context, "done", mConfirmSave);
        mController.displayPreference(preferenceScreen);

        mButton = mPreference.findViewById(R.id.done);
        assertThat(mButton).isNotNull();
    }

    @Test
    public void updateState_nameNonEmpty_buttonEnabled() {
        ZenMode mode = new TestModeBuilder().setName("Such a nice name").build();

        mController.updateState(mPreference, mode);

        assertThat(mButton.isEnabled()).isTrue();
        verifyNoMoreInteractions(mConfirmSave);
    }

    @Test
    public void updateState_nameEmpty_buttonDisabled() {
        ZenMode aModeHasNoName = new TestModeBuilder().setName("").build();

        mController.updateState(mPreference, aModeHasNoName);

        assertThat(mButton.isEnabled()).isFalse();
        verifyNoMoreInteractions(mConfirmSave);
    }

    @Test
    public void onButtonClick_callsConfirmSave() {
        mButton.performClick();

        verify(mConfirmSave).run();
    }
}
