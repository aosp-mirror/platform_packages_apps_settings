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

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
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
        mRootView = LayoutInflater.from(mContext).inflate(R.layout.preference_app, null);
        mWidgetView =
            LayoutInflater.from(mContext).inflate(R.layout.preference_widget_summary, null);
        final LinearLayout widgetFrame = mRootView.findViewById(android.R.id.widget_frame);
        assertThat(widgetFrame).isNotNull();
        widgetFrame.addView(mWidgetView);
        mPreferenceViewHolder = PreferenceViewHolder.createInstanceForTests(mRootView);

        mPowerGaugePreference = new PowerGaugePreference(mContext);
        assertThat(mPowerGaugePreference.getLayoutResource()).isEqualTo(R.layout.preference_app);
    }

    @Test
    public void testOnBindViewHolder_bindSubtitle() {
        mPowerGaugePreference.setSubtitle(SUBTITLE);
        mPowerGaugePreference.onBindViewHolder(mPreferenceViewHolder);

        TextView widgetSummary = (TextView) mPreferenceViewHolder.findViewById(R.id.widget_summary);
        assertThat(widgetSummary.getText()).isEqualTo(SUBTITLE);
    }

    @Test
    public void testOnBindViewHolder_showAnomaly_bindAnomalyIcon() {
        mPowerGaugePreference.shouldShowAnomalyIcon(true);
        mPowerGaugePreference.onBindViewHolder(mPreferenceViewHolder);

        TextView widgetSummary = (TextView) mPreferenceViewHolder.findViewById(R.id.widget_summary);
        final Drawable[] drawables = widgetSummary.getCompoundDrawablesRelative();

        assertThat(drawables[0]).isInstanceOf(VectorDrawable.class);
    }

    @Test
    public void testOnBindViewHolder_notShowAnomaly_bindAnomalyIcon() {
        mPowerGaugePreference.shouldShowAnomalyIcon(false);
        mPowerGaugePreference.onBindViewHolder(mPreferenceViewHolder);

        TextView widgetSummary = (TextView) mPreferenceViewHolder.findViewById(R.id.widget_summary);
        final Drawable[] drawables = widgetSummary.getCompoundDrawablesRelative();

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
