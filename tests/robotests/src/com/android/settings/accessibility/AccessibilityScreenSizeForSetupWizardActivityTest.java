/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.AccessibilityScreenSizeForSetupWizardActivity.VISION_FRAGMENT_NO;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupdesign.GlifLayout;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link AccessibilityScreenSizeForSetupWizardActivity} */
@RunWith(RobolectricTestRunner.class)
public class AccessibilityScreenSizeForSetupWizardActivityTest {
    private static final int DISPLAY_SIZE_FRAGMENT_NO = 1;

    private Context mContext = ApplicationProvider.getApplicationContext();
    private AccessibilityScreenSizeForSetupWizardActivity mActivity;

    @Before
    public void setup() {
        final Intent intent = new Intent();
        intent.putExtra(VISION_FRAGMENT_NO,
                mContext.getResources().getInteger(R.integer.suw_font_size_fragment_no));
        mActivity = Robolectric.buildActivity(AccessibilityScreenSizeForSetupWizardActivity.class,
                intent).create().get();
    }

    @Test
    public void generateHeader_setPageNoAsFontSize_returnFontSizeTitle() {
        mActivity.generateHeader(
                mActivity.getResources().getInteger(R.integer.suw_font_size_fragment_no));

        final GlifLayout layout = mActivity.findViewById(R.id.setup_wizard_layout);

        assertThat(layout.getHeaderText()).isEqualTo(mContext.getText(R.string.title_font_size));
    }

    @Test
    public void generateHeader_setPageNoAsDisplaySize_returnDisplaySizeTitle() {
        mActivity.generateHeader(DISPLAY_SIZE_FRAGMENT_NO);

        final GlifLayout layout = mActivity.findViewById(R.id.setup_wizard_layout);

        assertThat(layout.getHeaderText()).isEqualTo(mContext.getText(R.string.screen_zoom_title));
    }

    @Test
    public void initFooterButton_generateDoneButton() {
        mActivity.initFooterButton();

        final GlifLayout layout = mActivity.findViewById(R.id.setup_wizard_layout);
        final FooterBarMixin mixin = layout.getMixin(FooterBarMixin.class);

        assertThat(mixin.getPrimaryButton().getText()).isEqualTo(mContext.getText(R.string.done));
    }
}
