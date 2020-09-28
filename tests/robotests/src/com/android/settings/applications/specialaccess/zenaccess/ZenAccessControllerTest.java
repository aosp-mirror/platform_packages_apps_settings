/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.applications.specialaccess.zenaccess;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import android.app.NotificationManager;
import android.content.Context;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowNotificationManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowActivityManager;

@RunWith(RobolectricTestRunner.class)
public class ZenAccessControllerTest {

    private static final String TEST_PKG = "com.test.package";

    private FakeFeatureFactory mFeatureFactory;
    private Context mContext;
    private ZenAccessController mController;
    private ShadowActivityManager mActivityManager;


    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mController = new ZenAccessController(mContext, "key");
        mActivityManager = Shadow.extract(mContext.getSystemService(Context.ACTIVITY_SERVICE));
    }

    @Test
    public void isAvailable_byDefault_true() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void logSpecialPermissionChange() {
        ZenAccessController.logSpecialPermissionChange(true, "app", mContext);
        verify(mFeatureFactory.metricsFeatureProvider).action(any(Context.class),
                eq(MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_DND_ALLOW),
                eq("app"));

        ZenAccessController.logSpecialPermissionChange(false, "app", mContext);
        verify(mFeatureFactory.metricsFeatureProvider).action(any(Context.class),
                eq(MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_DND_DENY),
                eq("app"));
    }

    @Test
    @Config(shadows = ShadowNotificationManager.class)
    public void hasAccess_granted_yes() {
        final ShadowNotificationManager snm = Shadow.extract(mContext.getSystemService(
                NotificationManager.class));
        snm.setNotificationPolicyAccessGrantedForPackage(TEST_PKG);
        assertThat(ZenAccessController.hasAccess(mContext, TEST_PKG)).isTrue();
    }

    @Test
    @Config(shadows = ShadowNotificationManager.class)
    public void hasAccess_notGranted_no() {
        assertThat(ZenAccessController.hasAccess(mContext, TEST_PKG)).isFalse();
    }
}
