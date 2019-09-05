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

package com.android.settings.homepage;

import static com.google.common.truth.Truth.assertThat;

import android.view.View;
import android.widget.FrameLayout;

import com.android.settings.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SettingsHomepageActivityTest {

    @Test
    public void setHomepageContainerPaddingTop_shouldBeSetPaddingTop() {
        final SettingsHomepageActivity activity = Robolectric.buildActivity(
                SettingsHomepageActivity.class).create().get();
        final int searchBarHeight = activity.getResources().getDimensionPixelSize(
                R.dimen.search_bar_height);
        final int searchBarMargin = activity.getResources().getDimensionPixelSize(
                R.dimen.search_bar_margin);
        final View view = activity.findViewById(R.id.homepage_container);

        activity.setHomepageContainerPaddingTop();

        final int actualPaddingTop = view.getPaddingTop();
        assertThat(actualPaddingTop).isEqualTo(searchBarHeight + searchBarMargin * 2);
    }

    @Test
    public void launch_shouldHaveAnimationForIaFragment() {
        final SettingsHomepageActivity activity = Robolectric.buildActivity(
                SettingsHomepageActivity.class).create().get();
        final FrameLayout frameLayout = activity.findViewById(R.id.main_content);

        assertThat(frameLayout.getLayoutTransition()).isNotNull();
    }
}
