/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.content.Context;
import android.content.res.TypedArray;

import com.android.settings.R;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class IconOptionsProviderImplTest {

    private static final int EXPECTED_NUMBER_OF_ICON_OPTIONS = 40;

    @Test
    public void iconResources_correctResources() {
        Context context = RuntimeEnvironment.getApplication();
        String[] descriptions = context.getResources().getStringArray(
                R.array.zen_mode_icon_options_descriptions);
        assertThat(descriptions).hasLength(EXPECTED_NUMBER_OF_ICON_OPTIONS);

        try (TypedArray icons = context.getResources().obtainTypedArray(
                R.array.zen_mode_icon_options)) {
            assertThat(icons.length()).isEqualTo(EXPECTED_NUMBER_OF_ICON_OPTIONS);
        }
    }

    @Test
    public void getIcons_returnsList() {
        Context context = RuntimeEnvironment.getApplication();
        IconOptionsProviderImpl provider = new IconOptionsProviderImpl(context);

        ImmutableList<IconOptionsProvider.IconInfo> iconOptions = provider.getIcons();
        assertThat(iconOptions).hasSize(EXPECTED_NUMBER_OF_ICON_OPTIONS);
        for (int i = 0; i < iconOptions.size(); i++) {
            assertWithMessage("Checking description of item #" + i)
                    .that(iconOptions.get(i).description()).isNotEmpty();
        }
    }
}
