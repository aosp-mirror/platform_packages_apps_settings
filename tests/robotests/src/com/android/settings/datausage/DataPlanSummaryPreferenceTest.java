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
import android.graphics.Color;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.widget.DonutView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {
                SettingsShadowResources.class,
                SettingsShadowResources.SettingsShadowTheme.class
        }
)
public final class DataPlanSummaryPreferenceTest {

    private static final String TEST_PLAN_USAGE = "Test plan usage";
    private static final String TEST_PLAN_NAME = "Test plan name";
    private static final String TEST_PLAN_DESCRIPTION = "Test plan description";
    private static final int PLAN_USAGE_TEXT_COLOR = Color.parseColor("#FF5C94F1");
    private static final int METER_BACKGROUND_COLOR = Color.parseColor("#FFDBDCDC");
    private static final int METER_CONSUMED_COLOR = Color.parseColor("#FF5C94F1");

    private DataPlanSummaryPreference mPreference;
    private PreferenceViewHolder mHolder;

    @Before
    public void setUp() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.string.config_headlineFontFamily, "");
        Context context = RuntimeEnvironment.application;
        mPreference = new DataPlanSummaryPreference(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(mPreference.getLayoutResource(),
                new LinearLayout(context), false);
        mHolder = PreferenceViewHolder.createInstanceForTests(view);
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void shouldRender_withoutData() {
        mPreference.onBindViewHolder(mHolder);

        TextView planUsageTextView = (TextView) mHolder.findViewById(android.R.id.title);
        assertThat(planUsageTextView.getText().toString()).isEmpty();
        TextView planNameTextView = (TextView) mHolder.findViewById(android.R.id.text1);
        assertThat(planNameTextView.getText().toString()).isEmpty();
        TextView planDescriptionTextView = (TextView) mHolder.findViewById(android.R.id.text2);
        assertThat(planDescriptionTextView.getText().toString()).isEmpty();
    }

    @Test
    public void shouldRender_withData() {
        mPreference.setTitle(TEST_PLAN_USAGE);
        mPreference.setUsageTextColor(PLAN_USAGE_TEXT_COLOR);
        mPreference.setName(TEST_PLAN_NAME);
        mPreference.setDescription(TEST_PLAN_DESCRIPTION);
        mPreference.setPercentageUsage(0.25D);
        mPreference.setMeterBackgroundColor(METER_BACKGROUND_COLOR);
        mPreference.setMeterConsumedColor(METER_CONSUMED_COLOR);

        mPreference.onBindViewHolder(mHolder);

        TextView planUsageTextView = (TextView) mHolder.findViewById(android.R.id.title);
        assertThat(planUsageTextView.getTextColors().getDefaultColor())
                .isEqualTo(PLAN_USAGE_TEXT_COLOR);
        assertThat(planUsageTextView.getText()).isEqualTo(TEST_PLAN_USAGE);

        TextView planNameTextView = (TextView) mHolder.findViewById(android.R.id.text1);
        assertThat(planNameTextView.getText()).isEqualTo(TEST_PLAN_NAME);

        TextView planDescriptionTextView = (TextView) mHolder.findViewById(android.R.id.text2);
        assertThat(planDescriptionTextView.getText()).isEqualTo(TEST_PLAN_DESCRIPTION);

        DonutView donutView = (DonutView) mHolder.findViewById(R.id.donut);
        assertThat(donutView.getMeterBackgroundColor()).isEqualTo(METER_BACKGROUND_COLOR);
        assertThat(donutView.getMeterConsumedColor()).isEqualTo(METER_CONSUMED_COLOR);
    }
}
