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

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static android.view.View.MeasureSpec.makeMeasureSpec;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.app.Flags;
import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.TypedValue;
import android.view.View.MeasureSpec;
import android.widget.EditText;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.widget.LayoutPreference;

import com.google.android.material.textfield.TextInputLayout;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.function.Consumer;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public class ZenModeEditNamePreferenceControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private ZenModeEditNamePreferenceController mController;
    private LayoutPreference mPreference;
    private TextInputLayout mTextInputLayout;
    private EditText mEditText;
    @Mock private Consumer<String> mNameSetter;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        PreferenceManager preferenceManager = new PreferenceManager(mContext);

        // Inflation is a test in itself, because it will crash if the Theme isn't set correctly.
        PreferenceScreen preferenceScreen = preferenceManager.inflateFromResource(mContext,
                R.xml.modes_edit_name_icon, null);
        mPreference = preferenceScreen.findPreference("name");

        mController = new ZenModeEditNamePreferenceController(mContext, "name", mNameSetter);
        mController.displayPreference(preferenceScreen);
        mTextInputLayout = mPreference.findViewById(R.id.edit_input_layout);
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

        mEditText.setText("An even fancier name");

        verify(mNameSetter).accept("An even fancier name");
        verifyNoMoreInteractions(mNameSetter);
    }

    @Test
    public void onEditText_emptyText_showsError() {
        ZenMode mode = new TestModeBuilder().setName("Default name").build();
        mController.updateState(mPreference, mode);

        mEditText.setText("");

        assertThat(mTextInputLayout.getError()).isNotNull();

        mEditText.setText("this is fine");

        assertThat(mTextInputLayout.getError()).isNull();
    }

    @Test
    @Config(qualifiers = "xxxhdpi")
    public void onEditTextMeasure_hasRequiredHeightForAccessibility() {
        mEditText.measure(makeMeasureSpec(1_000, MeasureSpec.AT_MOST),
                makeMeasureSpec(1_000, MeasureSpec.AT_MOST));

        assertThat(mEditText.getMeasuredHeight()).isAtLeast(
                (int) TypedValue.applyDimension(COMPLEX_UNIT_DIP, 48,
                        mContext.getResources().getDisplayMetrics()));
    }
}
