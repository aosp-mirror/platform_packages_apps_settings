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

package com.android.settings.widget;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import androidx.preference.PreferenceViewHolder;
import android.view.View;
import android.widget.Button;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class ActionButtonPreferenceTest {

    private Context mContext;
    private View mRootView;
    private ActionButtonPreference mPref;
    private PreferenceViewHolder mHolder;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mRootView = View.inflate(mContext, R.layout.two_action_buttons, null /* parent */);
        mHolder = PreferenceViewHolder.createInstanceForTests(mRootView);
        mPref = new ActionButtonPreference(mContext);
    }

    @Test
    public void setVisibility_shouldUpdateButtonVisibility() {
        mPref.setButton1Visible(false).setButton2Visible(false);
        mPref.onBindViewHolder(mHolder);

        assertThat(mRootView.findViewById(R.id.button1_positive).getVisibility())
                .isEqualTo(View.INVISIBLE);
        assertThat(mRootView.findViewById(R.id.button1_negative).getVisibility())
                .isEqualTo(View.INVISIBLE);

        assertThat(mRootView.findViewById(R.id.button2_positive).getVisibility())
                .isEqualTo(View.INVISIBLE);
        assertThat(mRootView.findViewById(R.id.button2_negative).getVisibility())
                .isEqualTo(View.INVISIBLE);

        mPref.setButton1Visible(true).setButton2Visible(true);
        mPref.onBindViewHolder(mHolder);

        assertThat(mRootView.findViewById(R.id.button1_positive).getVisibility())
                .isEqualTo(View.VISIBLE);
        assertThat(mRootView.findViewById(R.id.button1_negative).getVisibility())
                .isEqualTo(View.INVISIBLE);
        assertThat(mRootView.findViewById(R.id.button2_positive).getVisibility())
                .isEqualTo(View.VISIBLE);
        assertThat(mRootView.findViewById(R.id.button2_negative).getVisibility())
                .isEqualTo(View.INVISIBLE);
    }

    @Test
    public void setPositiveNegative_shouldHideOppositeButton() {
        mPref.setButton1Positive(true).setButton2Positive(false);
        mPref.onBindViewHolder(mHolder);

        assertThat(mRootView.findViewById(R.id.button1_positive).getVisibility())
                .isEqualTo(View.VISIBLE);
        assertThat(mRootView.findViewById(R.id.button1_negative).getVisibility())
                .isEqualTo(View.INVISIBLE);
        assertThat(mRootView.findViewById(R.id.button2_positive).getVisibility())
                .isEqualTo(View.INVISIBLE);
        assertThat(mRootView.findViewById(R.id.button2_negative).getVisibility())
                .isEqualTo(View.VISIBLE);
    }

    @Test
    public void setEnabled_shouldEnableButton() {
        mPref.setButton1Enabled(true).setButton2Enabled(false);
        mPref.onBindViewHolder(mHolder);

        assertThat(mRootView.findViewById(R.id.button1_positive).isEnabled()).isTrue();
        assertThat(mRootView.findViewById(R.id.button1_negative).isEnabled()).isTrue();
        assertThat(mRootView.findViewById(R.id.button2_positive).isEnabled()).isFalse();
        assertThat(mRootView.findViewById(R.id.button2_negative).isEnabled()).isFalse();
    }

    @Test
    public void setText() {
        mPref.setButton1Text(R.string.settings_label);
        mPref.onBindViewHolder(mHolder);

        assertThat(((Button) mRootView.findViewById(R.id.button1_positive)).getText())
                .isEqualTo(mContext.getText(R.string.settings_label));
        assertThat(((Button) mRootView.findViewById(R.id.button1_negative)).getText())
                .isEqualTo(mContext.getText(R.string.settings_label));
    }

    public static ActionButtonPreference createMock() {
        final ActionButtonPreference pref = mock(ActionButtonPreference.class);
        when(pref.setButton1Text(anyInt())).thenReturn(pref);
        when(pref.setButton1Positive(anyBoolean())).thenReturn(pref);
        when(pref.setButton1Enabled(anyBoolean())).thenReturn(pref);
        when(pref.setButton1Visible(anyBoolean())).thenReturn(pref);
        when(pref.setButton1OnClickListener(any(View.OnClickListener.class))).thenReturn(pref);

        when(pref.setButton2Text(anyInt())).thenReturn(pref);
        when(pref.setButton2Positive(anyBoolean())).thenReturn(pref);
        when(pref.setButton2Enabled(anyBoolean())).thenReturn(pref);
        when(pref.setButton2Visible(anyBoolean())).thenReturn(pref);
        when(pref.setButton2OnClickListener(any(View.OnClickListener.class))).thenReturn(pref);
        return pref;
    }
}
