/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.settings.network;

import static androidx.lifecycle.Lifecycle.Event;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;
import android.os.UserManager;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedPreference;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class MobileNetworkPreferenceControllerTest {
    private Context mContext;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;

    @Mock
    private UserManager mUserManager;

    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mScreen;

    @Mock
    private LifecycleOwner mLifecycleOwner;
    private LifecycleRegistry mLifecycleRegistry;
    private MobileNetworkPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        when(mSubscriptionManager.createForAllUserProfiles()).thenReturn(mSubscriptionManager);
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mPreferenceManager = new PreferenceManager(mContext);
        mScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mPreference = new Preference(mContext);
        mPreference.setKey(MobileNetworkPreferenceController.KEY_MOBILE_NETWORK_SETTINGS);

        mLifecycleRegistry = new LifecycleRegistry(mLifecycleOwner);
        when(mLifecycleOwner.getLifecycle()).thenReturn(mLifecycleRegistry);
    }

    @Test
    public void secondaryUser_prefIsNotAvailable() {
        when(mUserManager.isAdminUser()).thenReturn(false);
        when(mTelephonyManager.isDataCapable()).thenReturn(true);

        mController = new MobileNetworkPreferenceController(mContext);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void wifiOnly_prefIsNotAvailable() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        when(mTelephonyManager.isDataCapable()).thenReturn(false);

        mController = new MobileNetworkPreferenceController(mContext);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    @UiThreadTest
    public void goThroughLifecycle_isAvailable_shouldListenToServiceChange() {
        mController = spy(new MobileNetworkPreferenceController(mContext));
        mLifecycleRegistry.addObserver(mController);
        doReturn(true).when(mController).isAvailable();

        mLifecycleRegistry.handleLifecycleEvent(Event.ON_START);
        verify(mController).onStart();
        verify(mTelephonyManager).registerTelephonyCallback(
                mContext.getMainExecutor(), mController.mTelephonyCallback);

        mLifecycleRegistry.handleLifecycleEvent(Event.ON_STOP);
        verify(mController).onStop();
        verify(mTelephonyManager).unregisterTelephonyCallback(mController.mTelephonyCallback);
    }

    @Test
    @UiThreadTest
    public void serviceStateChange_shouldUpdatePrefSummary() {
        final String testCarrierName = "test";

        mController = spy(new MobileNetworkPreferenceController(mContext));
        mLifecycleRegistry.addObserver(mController);
        doReturn(true).when(mController).isAvailable();

        mScreen.addPreference(mPreference);

        // Display pref and go through lifecycle to set up listener.
        mController.displayPreference(mScreen);
        mLifecycleRegistry.handleLifecycleEvent(Event.ON_START);
        verify(mController).onStart();
        verify(mTelephonyManager).registerTelephonyCallback(
                mContext.getMainExecutor(), mController.mTelephonyCallback);

        doReturn(testCarrierName).when(mController).getSummary();

        mController.mTelephonyCallback.onServiceStateChanged(null);

        // Carrier name should be set.
        Assert.assertEquals(mPreference.getSummary(), testCarrierName);
    }

    @Test
    public void airplaneModeTurnedOn_shouldDisablePreference() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Global.AIRPLANE_MODE_ON, 1);
        mController = spy(new MobileNetworkPreferenceController(mContext));
        final RestrictedPreference mPreference = new RestrictedPreference(mContext);
        mController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void airplaneModeTurnedOffAndNoUserRestriction_shouldEnablePreference() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Global.AIRPLANE_MODE_ON, 0);
        mController = spy(new MobileNetworkPreferenceController(mContext));
        final RestrictedPreference mPreference = new RestrictedPreference(mContext);
        mPreference.setDisabledByAdmin(null);
        mController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void airplaneModeTurnedOffAndHasUserRestriction_shouldDisablePreference() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Global.AIRPLANE_MODE_ON, 0);
        mController = spy(new MobileNetworkPreferenceController(mContext));
        final RestrictedPreference mPreference = new RestrictedPreference(mContext);
        mPreference.setDisabledByAdmin(EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN);
        mController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isFalse();
    }
}
