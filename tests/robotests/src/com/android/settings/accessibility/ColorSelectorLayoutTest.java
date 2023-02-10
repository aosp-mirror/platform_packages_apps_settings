/*
 * Copyright (C) 2023 The Android Open Source Project
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
import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ColorSelectorLayoutTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();

    ColorSelectorLayout mColorSelectorLayout;

    @ColorInt
    int mCheckedColor = Color.TRANSPARENT;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mColorSelectorLayout = new ColorSelectorLayout(mContext);
        mColorSelectorLayout.setOnCheckedChangeListener(
                layout -> mCheckedColor = layout.getCheckedColor(Color.TRANSPARENT));
    }

    @Test
    public void setColor_checkColorChanged() {
        mColorSelectorLayout.setCheckedColor(ScreenFlashNotificationColor.AZURE.mColorInt);
        assertThat(mCheckedColor)
                .isEqualTo(ScreenFlashNotificationColor.AZURE.mColorInt);
    }

    @Test
    public void getCheckedColor_defaultValue() {
        assertThat(mColorSelectorLayout.getCheckedColor(0xFF000000))
                .isEqualTo(0xFF000000);
    }

    @Test
    public void setSelectedColor_checkColorChanged() {
        mColorSelectorLayout.setCheckedColor(ScreenFlashNotificationColor.AZURE.mColorInt);
        assertThat(mColorSelectorLayout.getCheckedColor(0xFF000000))
                .isEqualTo(ScreenFlashNotificationColor.AZURE.mColorInt);
    }
}
