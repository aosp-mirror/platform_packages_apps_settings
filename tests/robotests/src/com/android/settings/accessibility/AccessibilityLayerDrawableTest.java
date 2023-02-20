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

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.ImageTestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link AccessibilityLayerDrawable}. */
@RunWith(RobolectricTestRunner.class)
public class AccessibilityLayerDrawableTest {

    private static final int TEST_RES_ID =
            com.android.internal.R.drawable.ic_accessibility_magnification;
    private static final int TEST_RES_ID_2 =
            com.android.internal.R.drawable.ic_accessibility_color_inversion;
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void createLayerDrawable_configCorrect() {
        final Drawable expected1stDrawable = mContext.getDrawable(
                R.drawable.a11y_button_preview_base);
        final Drawable expected2ndDrawable = mContext.getDrawable(TEST_RES_ID);

        final AccessibilityLayerDrawable actualDrawable =
                AccessibilityLayerDrawable.createLayerDrawable(mContext, TEST_RES_ID,
                        /* opacity= */ 27);

        final Drawable actual1stDrawable = actualDrawable.getDrawable(0);
        final Drawable actual2ndDrawable = actualDrawable.getDrawable(1);
        assertThat(ImageTestUtils.drawableToBitmap(actual1stDrawable).sameAs(
                ImageTestUtils.drawableToBitmap(expected1stDrawable))).isTrue();
        assertThat(ImageTestUtils.drawableToBitmap(actual2ndDrawable).sameAs(
                ImageTestUtils.drawableToBitmap(expected2ndDrawable))).isTrue();
    }

    @Test
    public void updateLayerDrawable_expectedFloatingMenuLayerDrawableState() {
        final AccessibilityLayerDrawable originalDrawable =
                AccessibilityLayerDrawable.createLayerDrawable(mContext, TEST_RES_ID, /* opacity= */
                        72);

        originalDrawable.updateLayerDrawable(mContext, TEST_RES_ID_2, /* opacity= */ 27);

        assertThat(originalDrawable.getConstantState()).isEqualTo(
                new AccessibilityLayerDrawable.AccessibilityLayerDrawableState(mContext,
                        TEST_RES_ID_2, /* opacity= */ 27));
    }
}
