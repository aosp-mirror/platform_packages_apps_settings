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

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.util.IconDrawableFactory;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AppGridViewTest {

    @Mock
    private ResolveInfo mInfo;
    @Mock
    private ActivityInfo mActivityInfo;
    private Context mContext;
    private IconDrawableFactory mIconFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mInfo.activityInfo = mActivityInfo;
        mContext = RuntimeEnvironment.application;
        mIconFactory = IconDrawableFactory.newInstance(mContext);
    }

    @Test
    public void appEntry_shouldLoadIcon() {

        final AppGridView.ActivityEntry activityEntry = new AppGridView.ActivityEntry(
                mInfo, "label", mIconFactory);

        assertThat(activityEntry.label).isEqualTo("label");
        assertThat(activityEntry.getIcon()).isNotNull();
    }

    @Test
    public void appEntry_compare_shouldCompareIgnoreCase() {
        final AppGridView.ActivityEntry entry1 = new AppGridView.ActivityEntry(
                mInfo, "label", mIconFactory);
        final AppGridView.ActivityEntry entry2 = new AppGridView.ActivityEntry(
                mInfo, "LABEL", mIconFactory);
        final AppGridView.ActivityEntry entry3 = new AppGridView.ActivityEntry(
                mInfo, "label2", mIconFactory);

        assertThat(entry1.compareTo(entry2)).isEqualTo(0);
        assertThat(entry1.compareTo(entry3)).isNotEqualTo(0);
    }
}
