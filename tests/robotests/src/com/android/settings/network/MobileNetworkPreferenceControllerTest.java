/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsWrapper;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(
    manifest = TestConfig.MANIFEST_PATH,
    sdk = TestConfig.SDK_VERSION,
    shadows = ShadowRestrictedLockUtilsWrapper.class
)
public class MobileNetworkPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private UserManager mUserManager;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private PreferenceScreen mScreen;

    private Lifecycle mLifecycle;
    private MobileNetworkPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycle = new Lifecycle();
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mConnectivityManager);
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE))
                .thenReturn(mTelephonyManager);
    }

    @Test
    public void secondaryUser_prefIsNotAvailable() {
        when(mUserManager.isAdminUser()).thenReturn(false);
        when(mUserManager.hasUserRestriction(anyString(), any(UserHandle.class)))
                .thenReturn(false);
        when(mConnectivityManager.isNetworkSupported(ConnectivityManager.TYPE_MOBILE))
                .thenReturn(true);

        mController = new MobileNetworkPreferenceController(mContext);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void wifiOnly_prefIsNotAvailable() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        when(mUserManager.hasUserRestriction(anyString(), any(UserHandle.class)))
                .thenReturn(false);
        when(mConnectivityManager.isNetworkSupported(ConnectivityManager.TYPE_MOBILE))
                .thenReturn(false);

        mController = new MobileNetworkPreferenceController(mContext);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void goThroughLifecycle_isAvailable_shouldListenToServiceChange() {
        mController = spy(new MobileNetworkPreferenceController(mContext));
        mLifecycle.addObserver(mController);
        doReturn(true).when(mController).isAvailable();

        mLifecycle.onResume();
        verify(mTelephonyManager).listen(mController.mPhoneStateListener,
                PhoneStateListener.LISTEN_SERVICE_STATE);

        mLifecycle.onPause();
        verify(mTelephonyManager).listen(mController.mPhoneStateListener,
                PhoneStateListener.LISTEN_NONE);
    }

    @Test
    public void serviceStateChange_shouldUpdatePrefSummary() {
        final String testCarrierName = "test";
        final Preference mPreference = mock(Preference.class);
        mController = spy(new MobileNetworkPreferenceController(mContext));
        mLifecycle.addObserver(mController);

        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        doReturn(true).when(mController).isAvailable();

        // Display pref and go through lifecycle to set up listener.
        mController.displayPreference(mScreen);
        mLifecycle.onResume();
        verify(mController).onResume();
        verify(mTelephonyManager).listen(mController.mPhoneStateListener,
                PhoneStateListener.LISTEN_SERVICE_STATE);

        // Trigger listener update
        when(mTelephonyManager.getNetworkOperatorName()).thenReturn(testCarrierName);
        mController.mPhoneStateListener.onServiceStateChanged(null);

        // Carrier name should be set.
        verify(mPreference).setSummary(testCarrierName);
    }

}
