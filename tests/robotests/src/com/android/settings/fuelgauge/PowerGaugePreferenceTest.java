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
package com.android.settings.fuelgauge;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class PowerGaugePreferenceTest {
    private static final String SUBTITLE = "Summary";
    private static final String CONTENT_DESCRIPTION = "Content description";
    private Context mContext;
    private PowerGaugePreference mPowerGaugePreference;
    private View mRootView;
    private View mWidgetView;
    private PreferenceViewHolder mPreferenceViewHolder;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mRootView = LayoutInflater.from(mContext).inflate(R.layout.preference,
                null);
        mWidgetView = LayoutInflater.from(mContext).inflate(R.layout.preference_widget_summary,
                null);
        ((LinearLayout) mRootView.findViewById(android.R.id.widget_frame)).addView(mWidgetView);
        mPreferenceViewHolder = PreferenceViewHolder.createInstanceForTests(mRootView);

        mPowerGaugePreference = new PowerGaugePreference(mContext);
    }

    @Test
    public void testOnBindViewHolder_bindSubtitle() {
        mPowerGaugePreference.setSubtitle(SUBTITLE);
        mPowerGaugePreference.onBindViewHolder(mPreferenceViewHolder);

        assertThat(((TextView) mPreferenceViewHolder.findViewById(
                R.id.widget_summary)).getText()).isEqualTo(SUBTITLE);
    }

    @Test
    public void testOnBindViewHolder_showAnomaly_bindAnomalyIcon() {
        mPowerGaugePreference.shouldShowAnomalyIcon(true);
        mPowerGaugePreference.onBindViewHolder(mPreferenceViewHolder);

        final Drawable[] drawables = ((TextView) mPreferenceViewHolder.findViewById(
                R.id.widget_summary)).getCompoundDrawablesRelative();

        assertThat(drawables[0]).isInstanceOf(VectorDrawable.class);
    }

    @Test
    public void testOnBindViewHolder_notShowAnomaly_bindAnomalyIcon() {
        mPowerGaugePreference.shouldShowAnomalyIcon(false);
        mPowerGaugePreference.onBindViewHolder(mPreferenceViewHolder);

        final Drawable[] drawables = ((TextView) mPreferenceViewHolder.findViewById(
                R.id.widget_summary)).getCompoundDrawablesRelative();

        assertThat(drawables[0]).isNull();
    }

    @Test
    public void testOnBindViewHolder_bindContentDescription() {
        mPowerGaugePreference.setContentDescription(CONTENT_DESCRIPTION);
        mPowerGaugePreference.onBindViewHolder(mPreferenceViewHolder);

        assertThat(mPreferenceViewHolder.findViewById(android.R.id.title).getContentDescription())
                .isEqualTo(CONTENT_DESCRIPTION);
    }
}
