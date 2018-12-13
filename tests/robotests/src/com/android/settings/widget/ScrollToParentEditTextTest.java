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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.robolectric.RuntimeEnvironment.application;

import android.graphics.Rect;
import android.widget.FrameLayout;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowView;

@RunWith(RobolectricTestRunner.class)
public class ScrollToParentEditTextTest {

    private static final int EDIT_TEXT_SIZE = 20;
    private static final int PARENT_SIZE = 50;
    private static final int SCROLL_RECT_SIZE = 30;

    private ScrollToParentEditText mEditText;
    private FrameLayout mParent;

    @Before
    public void setUp() {
        mEditText =
            new ScrollToParentEditText(application, Robolectric.buildAttributeSet().build());
        mEditText.layout(0, 0, EDIT_TEXT_SIZE, EDIT_TEXT_SIZE);

        mParent = spy(new FrameLayout(application));
        mParent.layout(0, 0, PARENT_SIZE, PARENT_SIZE);

        doReturn(true).when(mParent).requestRectangleOnScreen(any(Rect.class), anyBoolean());
    }

    @Test
    public void requestRectangleOnScreen_noParent_shouldScrollToItself() {
        assertThat(mEditText.requestRectangleOnScreen(
                new Rect(0, 0, SCROLL_RECT_SIZE, SCROLL_RECT_SIZE), true)).isFalse();
    }

    @Test
    public void requestRectangleOnScreen_withParent_shouldScrollToParent() {
        ShadowView shadowEditText = Shadows.shadowOf(mEditText);
        shadowEditText.setMyParent(mParent);

        assertThat(mEditText.requestRectangleOnScreen(
                new Rect(0, 0, SCROLL_RECT_SIZE, SCROLL_RECT_SIZE), true)).isTrue();
        verify(mParent)
                .requestRectangleOnScreen(eq(new Rect(0, 0, PARENT_SIZE, PARENT_SIZE)), eq(true));
    }
}
