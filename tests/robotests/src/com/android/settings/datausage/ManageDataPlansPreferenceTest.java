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
 * limitations under the License
 */

package com.android.settings.datausage;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public final class ManageDataPlansPreferenceTest {
    private Preference mPreference;
    private PreferenceViewHolder mHolder;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPreference = new Preference(mContext);
        mPreference.setLayoutResource(R.layout.manage_data_plans_preference);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(mPreference.getLayoutResource(),
                new LinearLayout(mContext), false);
        mHolder = PreferenceViewHolder.createInstanceForTests(view);
    }

    @Test
    public void shouldRender_withData() {
        mPreference.onBindViewHolder(mHolder);
        Button managePlanButton = (Button) mHolder.findViewById(R.id.manage_data_plans);
        assertThat(managePlanButton.getText())
                .isEqualTo(mContext.getString(R.string.data_plan_usage_manage_plans_button_text));
    }
}
