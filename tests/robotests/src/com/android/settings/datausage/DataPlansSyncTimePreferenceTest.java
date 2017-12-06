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
import android.widget.LinearLayout;
import android.widget.TextView;
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
public final class DataPlansSyncTimePreferenceTest {
    private static final String SYNC_TIME = "Today 12:24pm";

    private Preference mPreference;
    private PreferenceViewHolder mHolder;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.application;
        mPreference = new Preference(context);
        mPreference.setLayoutResource(R.layout.data_plans_sync_time_preference);

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(mPreference.getLayoutResource(),
                new LinearLayout(context), false);
        mHolder = PreferenceViewHolder.createInstanceForTests(view);
    }

    @Test
    public void shouldRender_withData() {
        mPreference.setTitle(SYNC_TIME);

        mPreference.onBindViewHolder(mHolder);

        TextView syncTimeTextView = (TextView) mHolder.findViewById(android.R.id.title);
        assertThat(syncTimeTextView.getText()).isEqualTo(SYNC_TIME);
    }
}
