/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/** Tests for {@link ShortcutPreference} */
@RunWith(RobolectricTestRunner.class)
public class ShortcutPreferenceTest {

    private ShortcutPreference mShortcutPreference;
    private View.OnClickListener mSettingButtonListener;
    private View.OnClickListener mCheckBoxListener;

    @Before
    public void setUp() {
        final Context mContext = RuntimeEnvironment.application;
        mShortcutPreference = new ShortcutPreference(mContext, null);
    }

    @Test
    public void setOnClickListeners_shouldSetListeners() {
        mShortcutPreference.setSettingButtonListener(mSettingButtonListener);
        mShortcutPreference.setCheckBoxListener(mCheckBoxListener);

        assertThat(mShortcutPreference.getCheckBoxListener()).isEqualTo(mCheckBoxListener);
        assertThat(mShortcutPreference.getSettingButtonListener()).isEqualTo(
                mSettingButtonListener);
    }
}
