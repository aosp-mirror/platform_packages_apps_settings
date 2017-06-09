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

package com.android.settings.search;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class HeaderDecoratorTest {
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private View mView;
    @Mock
    private RecyclerView mRecyclerView;
    @Mock
    private RecyclerView.LayoutParams mLayoutParams;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mView.getLayoutParams()).thenReturn(mLayoutParams);
    }

    @Test
    public void testgetItemOffsets_positionZero_headerAdded() {
        HeaderDecorator decorator = new HeaderDecorator();
        Rect outRect = new Rect();
        when(mRecyclerView.getChildAdapterPosition(any(View.class))).thenReturn(0);
        when(mView.getContext().obtainStyledAttributes(any(int[].class))
                .getDimensionPixelSize(0, 0)).thenReturn(20);
        when(mView.getContext().getResources().getDimensionPixelSize(R.dimen.search_bar_margin))
                .thenReturn(5);

        decorator.getItemOffsets(outRect, mView, mRecyclerView, null);

        assertThat(outRect).isEqualTo(new Rect(0, 30, 0, 0));
    }

    @Test
    public void testgetItemOffsets_positionGreaterThanZero_noDecoration() {
        HeaderDecorator decorator = new HeaderDecorator();
        Rect outRect = new Rect();
        when(mRecyclerView.getChildAdapterPosition(any(View.class))).thenReturn(1);

        decorator.getItemOffsets(outRect, mView, mRecyclerView, null);

        assertThat(outRect).isEqualTo(new Rect(0, 0, 0, 0));
    }
}
