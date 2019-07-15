/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static org.robolectric.RuntimeEnvironment.application;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SwitchBarTest {

    private static final int COLOR_BACKGROUND = 1;
    private static final int COLOR_BACKGROUND_ACTIVATED = 2;

    private Context mContext;
    private SwitchBar mBar;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mBar = new SwitchBar(application, Robolectric.buildAttributeSet()
                .addAttribute(R.attr.switchBarBackgroundColor, String.valueOf(COLOR_BACKGROUND))
                .addAttribute(R.attr.switchBarBackgroundActivatedColor,
                        String.valueOf(COLOR_BACKGROUND_ACTIVATED))
                .build());
    }

    @Test
    public void cycleChecked_defaultLabel_shouldUpdateTextAndBackground() {
        final int defaultOnText = R.string.switch_on_text;
        final int defaultOffText = R.string.switch_off_text;

        assertThat(((TextView) mBar.findViewById(R.id.switch_text)).getText())
                .isEqualTo(mContext.getString(defaultOffText));

        mBar.setChecked(true);

        assertThat(mBar.getBackground()).isInstanceOf(ColorDrawable.class);
        assertThat(((TextView) mBar.findViewById(R.id.switch_text)).getText())
                .isEqualTo(mContext.getString(defaultOnText));
    }

    @Test
    public void cycleChecked_customLabel_shouldUpdateTextAndBackground() {
        final int onText = R.string.master_clear_progress_text;
        final int offText = R.string.manage_space_text;

        mBar.setSwitchBarText(onText, offText);
        assertThat(((TextView) mBar.findViewById(R.id.switch_text)).getText())
                .isEqualTo(mContext.getString(offText));

        mBar.setChecked(true);
        assertThat(mBar.getBackground()).isInstanceOf(ColorDrawable.class);

        assertThat(((TextView) mBar.findViewById(R.id.switch_text)).getText())
                .isEqualTo(mContext.getString(onText));
    }

    @Test
    public void setCheck_customLabelWithStringType_shouldUpdateTextAndBackground() {
        final String onText = mContext.getString(
                R.string.accessibility_service_master_switch_title);
        final String offText = mContext.getString(
                R.string.accessibility_service_master_switch_title);
        final TextView switchBarTextView = ((TextView) mBar.findViewById(R.id.switch_text));

        mBar.setSwitchBarText(onText, offText);

        assertThat(switchBarTextView.getText()).isEqualTo(offText);

        mBar.setChecked(true);

        assertThat(mBar.getBackground()).isInstanceOf(ColorDrawable.class);
        assertThat(switchBarTextView.getText()).isEqualTo(onText);
    }

    @Test
    public void disabledByAdmin_shouldDelegateToRestrictedIcon() {
        mBar.setDisabledByAdmin(new EnforcedAdmin());
        assertThat(mBar.getDelegatingView().getId()).isEqualTo(R.id.restricted_icon);
    }

    @Test
    public void notDisabledByAdmin_shouldDelegateToSwitch() {
        mBar.setDisabledByAdmin(null);
        assertThat(mBar.getDelegatingView().getId()).isEqualTo(R.id.switch_widget);
    }

    @Test
    public void performClick_shouldIsCheckedValueChange() {
        boolean isChecked = mBar.isChecked();
        mBar.performClick();
        assertThat(mBar.isChecked()).isEqualTo(!isChecked);
    }
}
