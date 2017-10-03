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

package com.android.settings.applications;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static android.view.WindowManager.LayoutParams.PRIVATE_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;

import android.app.Activity;
import android.content.Context;

import android.view.Window;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;

import com.android.settings.testutils.shadow.ShadowAppInfoBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DrawOverlayDetailsTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Activity mActivity;

    @Mock
    private Window mWindow;

    private FakeFeatureFactory mFeatureFactory;

    @Spy
    private DrawOverlayDetails mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mActivity);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mActivity);
    }

    @Test
    public void logSpecialPermissionChange() {
        when(mFragment.getContext()).thenReturn(
                ShadowApplication.getInstance().getApplicationContext());

        mFragment.logSpecialPermissionChange(true, "app");
        verify(mFeatureFactory.metricsFeatureProvider).action(nullable(Context.class),
                eq(MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_APPDRAW_ALLOW), eq("app"));

        mFragment.logSpecialPermissionChange(false, "app");
        verify(mFeatureFactory.metricsFeatureProvider).action(nullable(Context.class),
                eq(MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_APPDRAW_DENY), eq("app"));
    }

    @Test
    @Config(shadows = {ShadowAppInfoBase.class})
    public void hideNonSystemOverlaysWhenResumed() {
        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mActivity.getWindow()).thenReturn(mWindow);

        mFragment.onResume();
        mFragment.onPause();

        InOrder inOrder = Mockito.inOrder(mWindow);
        inOrder.verify(mWindow).addFlags(PRIVATE_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);
        inOrder.verify(mWindow).clearFlags(PRIVATE_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);
        inOrder.verifyNoMoreInteractions();
    }
}
