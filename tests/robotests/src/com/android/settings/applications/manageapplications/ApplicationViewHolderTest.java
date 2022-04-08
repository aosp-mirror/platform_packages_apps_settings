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

package com.android.settings.applications.manageapplications;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.FrameLayout;

import com.android.settings.R;
import com.android.settingslib.applications.ApplicationsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ApplicationViewHolderTest {

    private Context mContext;
    private View mView;
    private ApplicationViewHolder mHolder;

    @Before
    public void seUp() {
        mContext = RuntimeEnvironment.application;
        mView = ApplicationViewHolder.newView(new FrameLayout(mContext));
        mHolder = new ApplicationViewHolder(mView);
    }

    @Test
    public void updateDisableView_appDisabledUntilUsed_shouldSetDisabled() {
        final ApplicationInfo info = new ApplicationInfo();
        info.flags = ApplicationInfo.FLAG_INSTALLED;
        info.enabled = true;
        info.enabledSetting = PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED;
        mHolder.updateDisableView(info);

        assertThat(mHolder.mDisabled.getText()).isEqualTo(mContext.getText(R.string.disabled));
    }

    @Test
    public void setSummaries() {
        mHolder.setSummary("hello");
        assertThat(mHolder.mSummary.getText()).isEqualTo("hello");

        mHolder.setSummary(R.string.disabled);
        assertThat(mHolder.mSummary.getText()).isEqualTo(mContext.getText(R.string.disabled));
    }

    @Test
    public void updateSize() {
        final String invalidStr = "invalid";
        final ApplicationsState.AppEntry entry = mock(ApplicationsState.AppEntry.class);
        entry.internalSizeStr = "internal";
        entry.externalSizeStr = "external";
        entry.sizeStr = entry.internalSizeStr;

        mHolder.updateSizeText(entry, invalidStr, ManageApplications.SIZE_INTERNAL);
        assertThat(mHolder.mSummary.getText()).isEqualTo(entry.internalSizeStr);

        mHolder.updateSizeText(entry, invalidStr, ManageApplications.SIZE_EXTERNAL);
        assertThat(mHolder.mSummary.getText()).isEqualTo(entry.externalSizeStr);

        entry.sizeStr = null;
        entry.size = ApplicationsState.SIZE_INVALID;
        mHolder.updateSizeText(entry, invalidStr, ManageApplications.SIZE_EXTERNAL);
        assertThat(mHolder.mSummary.getText()).isEqualTo(invalidStr);
    }

    @Test
    public void oneTouchTarget() {
        assertThat(mHolder.mSwitch).isNull();
        assertThat(mHolder.mWidgetContainer.getChildCount()).isEqualTo(0);
        // assert no exception
        mHolder.updateSwitch(null, true, true);
    }

    @Test
    public void twoTouchTarget() {
        mView = ApplicationViewHolder.newView(new FrameLayout(mContext), true);
        mHolder = new ApplicationViewHolder(mView);
        assertThat(mHolder.mSwitch).isNotNull();
        assertThat(mHolder.mWidgetContainer.getChildCount()).isEqualTo(1);
    }

    @Test
    public void updateSwitch() {
        mView = ApplicationViewHolder.newView(new FrameLayout(mContext), true);
        mHolder = new ApplicationViewHolder(mView);
        mHolder.updateSwitch(v -> {
        } /* listener */, true, true);

        assertThat(mHolder.mSwitch.isChecked()).isTrue();
        assertThat(mHolder.mSwitch.isEnabled()).isTrue();
        assertThat(mHolder.mWidgetContainer.hasOnClickListeners()).isTrue();
    }
}
