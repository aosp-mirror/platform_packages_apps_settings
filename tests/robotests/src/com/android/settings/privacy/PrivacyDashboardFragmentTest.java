/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.privacy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserManager;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowPermissionControllerManager;
import com.android.settings.testutils.shadow.ShadowUserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowAccessibilityManager;
import org.robolectric.shadows.androidx.fragment.FragmentController;

import java.util.ArrayList;


@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class, ShadowPermissionControllerManager.class})
public class PrivacyDashboardFragmentTest {

    @Mock
    private LockPatternUtils mLockPatternUtils;

    private Context mContext;
    private PrivacyDashboardFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        final UserManager userManager = mContext.getSystemService(UserManager.class);
        final ShadowUserManager shadowUserManager = Shadow.extract(userManager);
        final ShadowAccessibilityManager accessibilityManager = Shadow.extract(
                AccessibilityManager.getInstance(mContext));
        accessibilityManager.setEnabledAccessibilityServiceList(new ArrayList<>());
        shadowUserManager.addProfile(new UserInfo(123, null, 0));
        when(FakeFeatureFactory.setupForTest().securityFeatureProvider.getLockPatternUtils(
                any(Context.class))).thenReturn(mLockPatternUtils);
        mFragment = spy(FragmentController.of(new PrivacyDashboardFragment())
                .create().start().get());
    }

    @Test
    public void onViewCreated_shouldInitLinearProgressBar() {
        mFragment.onViewCreated(new View(mContext), new Bundle());

        verify(mFragment).initLoadingBar();
    }
}
