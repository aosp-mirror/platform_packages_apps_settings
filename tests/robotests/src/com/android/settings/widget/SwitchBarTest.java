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

package com.android.settings.widget;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.RuntimeEnvironment.application;

import android.content.Context;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SwitchBarTest {

    private Context mContext;
    private SwitchBar mBar;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mBar = new SwitchBar(application, Robolectric.buildAttributeSet().build());
    }

    @Test
    public void testDefaultLabels() {
        int defaultOnText = R.string.switch_on_text;
        int defaultOffText = R.string.switch_off_text;
        assertThat(((TextView) mBar.findViewById(R.id.switch_text)).getText())
                .isEqualTo(mContext.getString(defaultOffText));

        mBar.setChecked(true);
        assertThat(((TextView) mBar.findViewById(R.id.switch_text)).getText())
                .isEqualTo(mContext.getString(defaultOnText));
    }

    @Test
    public void testCustomLabels() {
        int onText = R.string.master_clear_progress_text;
        int offText = R.string.manage_space_text;
        mBar.setSwitchBarText(onText, offText);
        assertThat(((TextView) mBar.findViewById(R.id.switch_text)).getText())
                .isEqualTo(mContext.getString(offText));

        mBar.setChecked(true);
        assertThat(((TextView) mBar.findViewById(R.id.switch_text)).getText())
                .isEqualTo(mContext.getString(onText));
    }
}
