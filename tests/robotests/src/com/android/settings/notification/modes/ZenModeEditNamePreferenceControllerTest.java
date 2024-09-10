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
import android.widget.EditText;

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

import java.util.function.Consumer;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public class ZenModeEditNamePreferenceControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private ZenModeEditNamePreferenceController mController;
    private LayoutPreference mPreference;
    private EditText mEditText;
    @Mock private Consumer<String> mNameSetter;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        Context context = RuntimeEnvironment.application;
        PreferenceManager preferenceManager = new PreferenceManager(context);
        PreferenceScreen preferenceScreen = preferenceManager.inflateFromResource(context,
                R.xml.modes_edit_name_icon, null);
        mPreference = preferenceScreen.findPreference("name");

        mController = new ZenModeEditNamePreferenceController(context, "name", mNameSetter);
        mController.displayPreference(preferenceScreen);
        mEditText = mPreference.findViewById(android.R.id.edit);
        assertThat(mEditText).isNotNull();
    }

    @Test
    public void updateState_showsName() {
        ZenMode mode = new TestModeBuilder().setName("A fancy name").build();

        mController.updateState(mPreference, mode);

        assertThat(mEditText.getText().toString()).isEqualTo("A fancy name");
        verifyNoMoreInteractions(mNameSetter);
    }

    @Test
    public void onEditText_callsNameSetter() {
        ZenMode mode = new TestModeBuilder().setName("A fancy name").build();
        mController.updateState(mPreference, mode);
        EditText editText = mPreference.findViewById(android.R.id.edit);

        editText.setText("An even fancier name");

        verify(mNameSetter).accept("An even fancier name");
        verifyNoMoreInteractions(mNameSetter);
    }
}
