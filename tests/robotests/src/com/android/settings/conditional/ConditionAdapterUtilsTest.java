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
 * limitations under the License.
 */
package com.android.settings.conditional;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.DashboardAdapter;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settings.dashboard.conditional.ConditionAdapterUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import com.android.settings.R;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ConditionAdapterUtilsTest{
    @Mock
    private Condition mCondition;
    private DashboardAdapter.DashboardItemHolder mViewHolder;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        final CharSequence[] actions = new CharSequence[2];
        when(mCondition.getActions()).thenReturn(actions);

        final View view = LayoutInflater.from(mContext).inflate(R.layout.condition_card, new
                LinearLayout(mContext), true);
        mViewHolder = new DashboardAdapter.DashboardItemHolder(view);
    }

    @Test
    public void testBindView_isExpanded_returnVisible() {
        ConditionAdapterUtils.bindViews(mCondition, mViewHolder, true, null, null);
        assertThat(mViewHolder.itemView.findViewById(R.id.detail_group).getVisibility())
                .isEqualTo(View.VISIBLE);
    }

    @Test
    public void testBindView_isNotExpanded_returnGone() {
        ConditionAdapterUtils.bindViews(mCondition, mViewHolder, false, null, null);
        assertThat(mViewHolder.itemView.findViewById(R.id.detail_group).getVisibility())
                .isEqualTo(View.GONE);
    }
}
