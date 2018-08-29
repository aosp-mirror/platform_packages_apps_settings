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

package com.android.settings.dashboard;

import static com.google.common.truth.Truth.assertThat;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.drawer.Tile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class DashboardItemAnimatorTest {

    private DashboardItemAnimator mDashboardItemAnimator;
    private ViewHolder mViewHolder;

    @Before
    public void SetUp() {
        mDashboardItemAnimator = new DashboardItemAnimator();
        mViewHolder = new ViewHolder(new TextView(RuntimeEnvironment.application));
        mViewHolder.itemView.setTag(new Tile());
    }

    @Test
    public void testAnimateChange_NoPositionChange_NoPendingAnimation() {
        final boolean hasPendingAnimation =
            mDashboardItemAnimator.animateChange(mViewHolder, mViewHolder, 0, 1, 0, 1);
        assertThat(hasPendingAnimation).isFalse();
    }

    @Test
    public void testAnimateChange_HasPositionChange_HasPendingAnimation() {
        final boolean hasPendingAnimation =
            mDashboardItemAnimator.animateChange(mViewHolder, mViewHolder, 0, 0, 1, 1);
        assertThat(hasPendingAnimation).isTrue();
    }

    @Test
    public void testAnimateChange_HasRunningAnimationWhileNoPositionChange_NoPendingAnimation() {
        // Set pending move animations
        mDashboardItemAnimator.animateMove(mViewHolder, 0, 0, 1, 1);

        final boolean hasPendingAnimation =
            mDashboardItemAnimator.animateChange(mViewHolder, mViewHolder, 0, 1, 0, 1);
        assertThat(hasPendingAnimation).isFalse();
    }

    // Sample viewholder to use for test
    static final class ViewHolder extends RecyclerView.ViewHolder {

        ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
