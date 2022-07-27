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
 * limitations under the License
 */

package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserManager;
import android.text.TextUtils;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settingslib.wifi.WifiPermissionChecker;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUtils.class})
public class WifiScanModeActivityTest {

    static final String LAUNCHED_PACKAGE = "launched_package";
    static final String APP_LABEL = "app_label";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    UserManager mUserManager;
    @Mock
    WifiPermissionChecker mWifiPermissionChecker;

    WifiScanModeActivity mActivity;

    @Before
    public void setUp() {
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        when(mUserManager.isGuestUser()).thenReturn(false);

        mActivity = spy(Robolectric.setupActivity(WifiScanModeActivity.class));
        when(mActivity.getApplicationContext()).thenReturn(mContext);
        mActivity.mWifiPermissionChecker = mWifiPermissionChecker;
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
    }

    @Test
    public void launchActivity_noIntentAction_shouldNotFatalException() {
        WifiScanModeActivity wifiScanModeActivity =
                Robolectric.setupActivity(WifiScanModeActivity.class);
    }

    @Test
    public void refreshAppLabel_noPackageName_shouldNotFatalException() {
        when(mWifiPermissionChecker.getLaunchedPackage()).thenReturn(null);

        mActivity.refreshAppLabel();

        assertThat(TextUtils.isEmpty(mActivity.mApp)).isTrue();
    }

    @Test
    public void refreshAppLabel_hasPackageName_shouldHasAppLabel() {
        ShadowUtils.setApplicationLabel(LAUNCHED_PACKAGE, APP_LABEL);
        when(mWifiPermissionChecker.getLaunchedPackage()).thenReturn(LAUNCHED_PACKAGE);

        mActivity.refreshAppLabel();

        assertThat(mActivity.mApp).isEqualTo(APP_LABEL);
    }

    @Test
    public void createDialog_isNotGuestUser_shouldNotFinishDialog() {
        when(mUserManager.isGuestUser()).thenReturn(false);

        mActivity.createDialog();

        verify(mActivity, never()).finish();
    }

    @Test
    public void createDialog_isGuestUser_shouldFinishDialog() {
        when(mUserManager.isGuestUser()).thenReturn(true);

        mActivity.createDialog();

        verify(mActivity).finish();
    }
}
