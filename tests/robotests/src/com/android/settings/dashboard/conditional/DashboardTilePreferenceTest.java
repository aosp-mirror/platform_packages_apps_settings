/**
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.dashboard.conditional;


import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.DashboardTilePreference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DashboardTilePreferenceTest {

    private ShadowApplication mShadowApplication;
    private Context mContext;
    private PreferenceViewHolder mHolder;
    private DashboardTilePreference mPreference;

    @Before
    public void setUp() {
        mShadowApplication = ShadowApplication.getInstance();
        mContext = mShadowApplication.getApplicationContext();
        mPreference = new DashboardTilePreference(mContext);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(mPreference.getLayoutResource(),
                new LinearLayout(mContext), false);

        mHolder = new PreferenceViewHolder(view);
    }

    @Test
    public void setHasDivider_shouldShowDivider() {
        mPreference.setDividerAllowedAbove(true);
        mPreference.setDividerAllowedBelow(true);
        mPreference.onBindViewHolder(mHolder);

        assertThat(mHolder.isDividerAllowedAbove()).isTrue();
        assertThat(mHolder.isDividerAllowedBelow()).isTrue();
    }

    @Test
    public void setHasNoDivider_shouldHideDivider() {
        mPreference.setDividerAllowedAbove(false);
        mPreference.setDividerAllowedBelow(false);
        mPreference.onBindViewHolder(mHolder);

        assertThat(mHolder.isDividerAllowedAbove()).isFalse();
        assertThat(mHolder.isDividerAllowedBelow()).isFalse();
    }
}
