/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import android.graphics.Color;
import android.view.accessibility.CaptioningManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link CaptionUtils}. */
@RunWith(RobolectricTestRunner.class)
public class CaptionUtilsTest {

    @Test
    public void parseColor_defaultPackedColor_shouldReturnUnspecified() {
        final int color = CaptionUtils.parseColor(0xFFFF00);

        assertThat(color).isEqualTo(CaptioningManager.CaptionStyle.COLOR_UNSPECIFIED);
    }

    @Test
    public void parseColor_unrecognizedColor_shouldReturnTransparent() {
        final int color = CaptionUtils.parseColor(0x00);

        assertThat(color).isEqualTo(Color.TRANSPARENT);
    }

    @Test
    public void parseColor_redColor_shouldReturnRed() {
        final int color = CaptionUtils.parseColor(0xFFFF0000);

        assertThat(color).isEqualTo(Color.RED);
    }

    @Test
    public void parseOpacity_defaultPackedColor_shouldReturnUnspecified() {
        final int color = CaptionUtils.parseOpacity(0xFFFF00);

        assertThat(color).isEqualTo(CaptioningManager.CaptionStyle.COLOR_UNSPECIFIED);
    }

    @Test
    public void parseOpacity_unrecognizedColor_shouldReturnTransparent() {
        final int color = CaptionUtils.parseOpacity(0x00);

        assertThat(color).isEqualTo(0xFFFFFF);
    }

    @Test
    public void parseOpacity_halfTransparentValue_shouldReturnHalfTransparent() {
        final int color = CaptionUtils.parseOpacity(0x80FFFFFF);

        assertThat(color).isEqualTo(0x80FFFFFF);
    }

    @Test
    public void mergeColorOpacity_halfTransparentRedValue_shouldReturnMergeColorOpacityValue() {
        final int color = CaptionUtils.mergeColorOpacity(0xFFFF0000, 0x80FFFFFF);

        assertThat(color).isEqualTo(0x80FF0000);
    }
}
