/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings;

import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResources;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {
                SettingsShadowResources.class,
                SettingsShadowResources.SettingsShadowTheme.class
})
public class SummaryPreferenceTest {

    private Context mContext;
    private PreferenceViewHolder mHolder;
    private SummaryPreference mPreference;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPreference = new SummaryPreference(mContext, null);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(mPreference.getLayoutResource(),
                new LinearLayout(mContext), false);

        mHolder = PreferenceViewHolder.createInstanceForTests(view);
    }

    @Test
    public void disableChart_shouldNotRender() {
        mPreference.setChartEnabled(false);
        mPreference.onBindViewHolder(mHolder);

        assertTrue(
                TextUtils.isEmpty(((TextView) mHolder.findViewById(android.R.id.text1)).getText()));
        assertTrue(
                TextUtils.isEmpty(((TextView) mHolder.findViewById(android.R.id.text2)).getText()));
    }

    @Test
    public void enableChart_shouldRender() {
        final String testLabel1 = "label1";
        final String testLabel2 = "label2";
        mPreference.setChartEnabled(true);
        mPreference.setLabels(testLabel1, testLabel2);
        mPreference.onBindViewHolder(mHolder);

        assertEquals(testLabel1,
                ((TextView) mHolder.findViewById(android.R.id.text1)).getText());
        assertEquals(testLabel2,
                ((TextView) mHolder.findViewById(android.R.id.text2)).getText());
    }

}
