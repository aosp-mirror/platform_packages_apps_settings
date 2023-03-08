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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class ScreenFlashNotificationColorTest {

    private static final int OPAQUE_COLOR_MASK = 0xFF000000;

    @ParameterizedRobolectricTestRunner.Parameters(name = "Color: {0}")
    public static List<?> params() {
        final List<Object[]> list = new ArrayList<>();
        for (ScreenFlashNotificationColor color : ScreenFlashNotificationColor.values()) {
            list.add(new Object[]{color});
        }
        return list;
    }

    final ScreenFlashNotificationColor mColor;

    public ScreenFlashNotificationColorTest(ScreenFlashNotificationColor color) {
        mColor = color;
    }

    @Test
    public void colorInt_assertNotTranslucent() {
        assertThat(mColor.mColorInt & OPAQUE_COLOR_MASK).isNotEqualTo(0);
    }

    @Test
    public void opaqueColorMask() {
        assertThat(mColor.mOpaqueColorInt & OPAQUE_COLOR_MASK).isEqualTo(OPAQUE_COLOR_MASK);
    }

    @Test
    public void stringRes_assertValid() {
        assertThat(mColor.mStringRes).isNotEqualTo(0);
    }
}
