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

package com.android.settings.deviceinfo.simstatus;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.Fragment;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.TelephonyManager;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import com.google.common.truth.Truth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SimStatusDualSimPreferenceControllerTest {

    @Mock
    private Preference mPreference;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private Fragment mFragment;

    private Context mContext;
    private SimStatusDualSimPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mUserManager).when(mContext).getSystemService(UserManager.class);
        doReturn(mConnectivityManager).when(mContext).getSystemService(ConnectivityManager.class);
        when(mUserManager.isAdminUser()).thenReturn(true);
        when(mConnectivityManager.isNetworkSupported(ConnectivityManager.TYPE_MOBILE)).thenReturn(
                true);
        mController = new SimStatusDualSimPreferenceController(mContext, mFragment);
        ReflectionHelpers.setField(mController, "mTelephonyManager", mTelephonyManager);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        when(mPreference.getKey()).thenReturn(mController.getPreferenceKey());
    }

    @Test
    public void isAvailable_multiSim_shouldBeTrue() {
        ReflectionHelpers.setField(mController, "mIsMultiSim", true);

        Truth.assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_notMultiSim_shouldBeFalse() {
        ReflectionHelpers.setField(mController, "mIsMultiSim", false);

        Truth.assertThat(mController.isAvailable()).isFalse();
    }
}
