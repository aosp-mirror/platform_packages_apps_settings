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

package com.android.settings.applications.specialaccess.pictureinpicture;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.pm.ActivityInfo;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PictureInPictureDetailsTest {

    private FakeFeatureFactory mFeatureFactory;
    private PictureInPictureDetails mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mFragment = new PictureInPictureDetails();
    }

    @Test
    public void testIgnoredApp() {
        for (String ignoredPackage : PictureInPictureSettings.IGNORE_PACKAGE_LIST) {
            assertThat(checkPackageHasPictureInPictureActivities(ignoredPackage, true))
                            .isFalse();
        }
    }

    @Test
    public void testNonPippableApp() {
        assertThat(checkPackageHasPictureInPictureActivities("com.android.dummypackage")).isFalse();
        assertThat(checkPackageHasPictureInPictureActivities("com.android.dummypackage",
                false, false, false)).isFalse();
    }

    @Test
    public void testPippableApp() {
        assertThat(checkPackageHasPictureInPictureActivities("com.android.dummypackage",
                true)).isTrue();
        assertThat(checkPackageHasPictureInPictureActivities("com.android.dummypackage",
                false, true)).isTrue();
        assertThat(checkPackageHasPictureInPictureActivities("com.android.dummypackage",
                true, false)).isTrue();
    }

    @Test
    public void logSpecialPermissionChange() {
        mFragment.logSpecialPermissionChange(true, "app");
        verify(mFeatureFactory.metricsFeatureProvider).action(
                SettingsEnums.PAGE_UNKNOWN,
                MetricsProto.MetricsEvent.APP_PICTURE_IN_PICTURE_ALLOW,
                mFragment.getMetricsCategory(),
                "app",
                0);

        mFragment.logSpecialPermissionChange(false, "app");
        verify(mFeatureFactory.metricsFeatureProvider).action(
                SettingsEnums.PAGE_UNKNOWN,
                MetricsProto.MetricsEvent.APP_PICTURE_IN_PICTURE_DENY,
                mFragment.getMetricsCategory(),
                "app",
                0);
    }

    private boolean checkPackageHasPictureInPictureActivities(String packageName,
            boolean... resizeableActivityState) {
        ActivityInfo[] activities = null;
        if (resizeableActivityState.length > 0) {
            activities = new ActivityInfo[resizeableActivityState.length];
            for (int i = 0; i < activities.length; i++) {
                activities[i] = new MockActivityInfo(resizeableActivityState[i]);
            }
        }
        return PictureInPictureSettings.checkPackageHasPictureInPictureActivities(packageName,
                activities);
    }

    private class MockActivityInfo extends ActivityInfo {

        private boolean mSupportsPictureInPicture;

        private MockActivityInfo(boolean supportsPictureInPicture) {
            mSupportsPictureInPicture = supportsPictureInPicture;
        }

        @Override
        public boolean supportsPictureInPicture() {
            return mSupportsPictureInPicture;
        }
    }
}
