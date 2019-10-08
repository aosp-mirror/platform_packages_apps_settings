/**
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
package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ChangeWifiStateDetailsTest {

    private static final String PACKAGE_NAME = "app";
    private FakeFeatureFactory mFeatureFactory;
    private ChangeWifiStateDetails mFragment;
    private Context mContext;

    @Mock
    private AppStateChangeWifiStateBridge.WifiSettingsState mState;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mFragment = new ChangeWifiStateDetails();
    }

    @Test
    public void testLogSpecialPermissionChange_setTrue_returnAllow() {
        mFragment.logSpecialPermissionChange(true /*newState*/, PACKAGE_NAME);
        verify(mFeatureFactory.metricsFeatureProvider).action(nullable(Context.class),
                eq(MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_SETTINGS_CHANGE_ALLOW),
                eq(PACKAGE_NAME));
    }

    @Test
    public void testLogSpecialPermissionChange_setFalse_returnDeny() {
        mFragment.logSpecialPermissionChange(false /*newState*/, PACKAGE_NAME);
        verify(mFeatureFactory.metricsFeatureProvider).action(nullable(Context.class),
                eq(MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_SETTINGS_CHANGE_DENY),
                eq(PACKAGE_NAME));
    }

    @Test
    public void testGetSummary_permissibleTrue_returnAllowed() {
        when(mState.isPermissible()).thenReturn(true);
        assertThat(ChangeWifiStateDetails.getSummary(mContext, mState))
            .isEqualTo(mContext.getString(R.string.app_permission_summary_allowed));
    }

    @Test
    public void testGetSummary_permissibleFalse_returnNotAllowed() {
        when(mState.isPermissible()).thenReturn(false);
        assertThat(ChangeWifiStateDetails.getSummary(mContext, mState))
            .isEqualTo(mContext.getString(R.string.app_permission_summary_not_allowed));
    }
}
