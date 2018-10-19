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

package com.android.settings.dashboard;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.ShapeDrawable;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class RoundedHomepageIconTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void createIcon_shouldSetBackgroundAndInset() {
        final RoundedHomepageIcon icon =
                new RoundedHomepageIcon(mContext, new ColorDrawable(Color.BLACK));

        assertThat(icon.getNumberOfLayers()).isEqualTo(2);
        assertThat(icon.getDrawable(0))
                .isEqualTo(mContext.getDrawable(R.drawable.ic_homepage_generic_background));
    }

    @Test
    public void setBackgroundColor_shouldUpdateColorFilter() {
        final RoundedHomepageIcon icon =
                spy(new RoundedHomepageIcon(mContext, new ColorDrawable(Color.BLACK)));
        final ShapeDrawable background = mock(ShapeDrawable.class);
        when(icon.getDrawable(0)).thenReturn(background);

        icon.setBackgroundColor(Color.BLUE);

        verify(background).setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_ATOP);
    }
}
