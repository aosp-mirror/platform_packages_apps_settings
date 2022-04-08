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

package com.android.settings.accounts;

import static com.android.settingslib.TwoTargetPreference.ICON_SIZE_MEDIUM;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class ProviderPreferenceTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void shouldUseSmallIcon() {
        final ProviderPreference providerPreference = new ProviderPreference(
                mContext, "account_type", null /* icon */, "provider_name");
        final int iconSize =
                ReflectionHelpers.getField(providerPreference, "mIconSize");
        assertThat(iconSize).isEqualTo(ICON_SIZE_MEDIUM);
    }
}
