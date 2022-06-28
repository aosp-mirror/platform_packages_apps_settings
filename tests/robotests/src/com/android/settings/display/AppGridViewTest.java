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

package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.IconDrawableFactory;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link AppGridView}.
 */
@RunWith(RobolectricTestRunner.class)
public class AppGridViewTest {

    @Mock
    private ResolveInfo mInfo;
    @Mock
    private ActivityInfo mActivityInfo;
    @Mock
    private ApplicationInfo mApplicationInfo;
    @Mock
    private Drawable mIcon;
    @Mock
    private PackageManager mPackageManager;
    private Context mContext;
    private IconDrawableFactory mIconFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mInfo.activityInfo = mActivityInfo;
        mInfo.activityInfo.applicationInfo = mApplicationInfo;
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mPackageManager).when(mContext).getPackageManager();
        mIconFactory = IconDrawableFactory.newInstance(mContext);
    }

    @Test
    public void appEntry_shouldLoadIcon() {
        when(mPackageManager.loadUnbadgedItemIcon(mActivityInfo, mApplicationInfo))
            .thenReturn(mIcon);
        final AppGridView.ActivityEntry activityEntry =
            new AppGridView.ActivityEntry(mInfo, "label", mIconFactory);

        assertThat(activityEntry.label).isEqualTo("label");
        assertThat(activityEntry.getIcon()).isNotNull();
    }

    @Test
    public void appEntry_compare_shouldCompareIgnoreCase() {
        final AppGridView.ActivityEntry entry1 =
            new AppGridView.ActivityEntry(mInfo, "label", mIconFactory);
        final AppGridView.ActivityEntry entry2 =
            new AppGridView.ActivityEntry(mInfo, "LABEL", mIconFactory);
        final AppGridView.ActivityEntry entry3 =
            new AppGridView.ActivityEntry(mInfo, "label2", mIconFactory);

        assertThat(entry1.compareTo(entry2)).isEqualTo(0);
        assertThat(entry1.compareTo(entry3)).isNotEqualTo(0);
    }

    @Test
    public void noAppCountAttribute_matchListSize() {
        final int appCountFromSystem = 8;
        setUpResolveInfos(appCountFromSystem);

        final AppGridView appGridView = new AppGridView(mContext, /* attrs= */ null);

        assertThat(appGridView.getAdapter().getCount()).isEqualTo(/* expected= */ 6);
    }

    @Test
    public void setAppCountAttribute_matchListSize() {
        final int appCountFromSystem = 8;
        final int appCountFromAttr = 7;
        setUpResolveInfos(appCountFromSystem);

        final AppGridView appGridView =
                new AppGridView(mContext, createAttributeSet(appCountFromAttr));

        assertThat(appGridView.getAdapter().getCount()).isEqualTo(appCountFromAttr);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setAppCountAttribute_belowLowerBound_matchListSize() {
        final int appCountFromSystem = 9;
        final int appCountFromAttr = -1; // The num is just for the test.
        setUpResolveInfos(appCountFromSystem);

        new AppGridView(mContext, createAttributeSet(appCountFromAttr));
    }

    @Test
    public void setAppCountAttribute_aboveUpperBound_matchListSize() {
        final int appCountFromSystem = 10;
        final int appCountFromAttr = 15;
        setUpResolveInfos(appCountFromSystem);

        final AppGridView appGridView =
                new AppGridView(mContext, createAttributeSet(appCountFromAttr));

        assertThat(appGridView.getAdapter().getCount()).isEqualTo(appCountFromSystem);
    }

    private AttributeSet createAttributeSet(int appCount) {
        return Robolectric.buildAttributeSet()
                .addAttribute(R.attr.appCount, String.valueOf(appCount))
                .build();
    }

    private void setUpResolveInfos(int appCount) {
        when(mContext.getPackageManager().queryIntentActivities(
                any(Intent.class), anyInt()))
                .thenReturn(createFakeResolveInfos(appCount));
    }

    private List<ResolveInfo> createFakeResolveInfos(int count) {
        final List<ResolveInfo> list = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            final ResolveInfo info = new ResolveInfo();
            info.nonLocalizedLabel = String.valueOf(i);

            list.add(info);
        }

        return list;
    }
}
